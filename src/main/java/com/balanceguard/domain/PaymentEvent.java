package com.balanceguard.domain;

public record PaymentEvent(
        String eventId,
        double balanceAfterUsd,
        double rechargeAmountUsd,
        boolean rechargeFired,
        boolean riskSignal) {

    public PaymentEvent {
        if (eventId == null || eventId.isBlank()) throw new IllegalArgumentException("eventId is required");
        if (balanceAfterUsd < 0) throw new IllegalArgumentException("balanceAfterUsd cannot be negative");
        if (rechargeAmountUsd <= 0) throw new IllegalArgumentException("rechargeAmountUsd must be positive");
    }
}
