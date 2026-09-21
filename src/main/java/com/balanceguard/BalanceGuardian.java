package com.balanceguard;

import com.balanceguard.client.InfraiClient;
import com.balanceguard.config.InfraiConfig;
import com.balanceguard.domain.PaymentEvent;
import com.balanceguard.service.BalanceProtectionService;

import java.time.Clock;

public final class BalanceGuardian {
    private BalanceGuardian() {}

    public static void main(String[] args) {
        if (args.length != 6) {
            System.err.println("Usage: BalanceGuardian <event-id> <balance-after-usd> <recharge-amount-usd> <recharge-fired> <risk-signal> <recipient>");
            System.exit(2);
        }

        InfraiConfig config = InfraiConfig.fromEnvironment();
        InfraiClient gateway = new InfraiClient(config);
        BalanceProtectionService service = new BalanceProtectionService(
                gateway, config.triggerBalance(), config.auditPath(), Clock.systemUTC());

        PaymentEvent event = new PaymentEvent(
                args[0], Double.parseDouble(args[1]), Double.parseDouble(args[2]),
                Boolean.parseBoolean(args[3]), Boolean.parseBoolean(args[4]));

        service.configure(event.rechargeAmountUsd());
        BalanceProtectionService.ProcessedPayment result = service.process(event, args[5]);
        System.out.printf("event_id=%s decision=%s message_id=%s%n",
                result.eventId(), result.decision(), result.messageId());
    }
}
