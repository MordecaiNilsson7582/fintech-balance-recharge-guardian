package com.balanceguard.service;

import com.balanceguard.client.InfraiClient;
import com.balanceguard.domain.PaymentEvent;
import com.balanceguard.domain.RechargeDecision;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

public final class BalanceProtectionService {
    private final InfraiClient gateway;
    private final double triggerBalance;
    private final Path auditPath;
    private final Clock clock;

    public BalanceProtectionService(InfraiClient gateway, double triggerBalance, Path auditPath, Clock clock) {
        this.gateway = gateway;
        this.triggerBalance = triggerBalance;
        this.auditPath = auditPath;
        this.clock = clock;
    }

    public void configure(double rechargeAmountUsd) {
        gateway.configureAutoRecharge(triggerBalance, rechargeAmountUsd);
    }

    public RechargeDecision decide(PaymentEvent event) {
        if (!event.rechargeFired()) return RechargeDecision.NO_ACTION;
        if (event.riskSignal() || event.balanceAfterUsd() > triggerBalance) {
            return RechargeDecision.REVIEW_AND_NOTIFY;
        }
        return RechargeDecision.NOTIFY_RECHARGE;
    }

    public ProcessedPayment process(PaymentEvent event, String recipient) {
        RechargeDecision decision = decide(event);
        String messageId = null;
        if (decision != RechargeDecision.NO_ACTION) {
            String subject = decision == RechargeDecision.REVIEW_AND_NOTIFY
                    ? "Recharge event requires review" : "Account recharge recorded";
            String text = String.format(Locale.ROOT,
                    "Payment event %s recorded a %.2f USD recharge. Balance after event: %.2f USD. Decision: %s.",
                    event.eventId(), event.rechargeAmountUsd(), event.balanceAfterUsd(), decision);
            messageId = gateway.sendRechargeNotice(recipient, subject, text);
        }
        appendAudit(event, decision, messageId);
        return new ProcessedPayment(event.eventId(), decision, messageId);
    }

    private void appendAudit(PaymentEvent event, RechargeDecision decision, String messageId) {
        String line = "{\"recorded_at\":\"" + Instant.now(clock) + "\",\"event_id\":\""
                + clean(event.eventId()) + "\",\"decision\":\"" + decision + "\",\"message_id\":"
                + (messageId == null ? "null" : "\"" + clean(messageId) + "\"") + "}" + System.lineSeparator();
        try {
            Path parent = auditPath.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(auditPath, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new IllegalStateException("Could not append payment audit record", e);
        }
    }

    private static String clean(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public record ProcessedPayment(String eventId, RechargeDecision decision, String messageId) {}
}
