package com.balanceguard;

import com.balanceguard.domain.PaymentEvent;
import com.balanceguard.domain.RechargeDecision;
import com.balanceguard.service.BalanceProtectionService;

public final class BalanceProtectionDecisionTest {
    public static void main(String[] args) {
        BalanceProtectionService service = new BalanceProtectionService(null, 25.00, null, null);

        assertDecision(service,
                new PaymentEvent("pay-settled", 48.00, 100.00, false, false),
                RechargeDecision.NO_ACTION);
        assertDecision(service,
                new PaymentEvent("recharge-normal", 12.50, 100.00, true, false),
                RechargeDecision.NOTIFY_RECHARGE);
        assertDecision(service,
                new PaymentEvent("recharge-risk", 12.50, 100.00, true, true),
                RechargeDecision.REVIEW_AND_NOTIFY);
        assertDecision(service,
                new PaymentEvent("recharge-anomaly", 80.00, 100.00, true, false),
                RechargeDecision.REVIEW_AND_NOTIFY);

        System.out.println("BalanceProtectionDecisionTest passed: 4 decisions");
    }

    private static void assertDecision(
            BalanceProtectionService service, PaymentEvent event, RechargeDecision expected) {
        RechargeDecision actual = service.decide(event);
        if (actual != expected) {
            throw new AssertionError(event.eventId() + ": expected " + expected + ", got " + actual);
        }
    }
}
