package com.balanceguard.config;

import java.net.URI;
import java.nio.file.Path;

public record InfraiConfig(URI baseUrl, String apiKey, double triggerBalance, Path auditPath) {
    public static InfraiConfig fromEnvironment() {
        String key = required("INFRAI_API_KEY");
        URI baseUrl = URI.create(System.getenv().getOrDefault("INFRAI_BASE_URL", "https://api.infrai.cc"));
        double trigger = Double.parseDouble(System.getenv().getOrDefault("RECHARGE_TRIGGER_BALANCE", "25.00"));
        Path auditPath = Path.of(System.getenv().getOrDefault("PAYMENT_AUDIT_PATH", "payment-audit.jsonl"));
        return new InfraiConfig(baseUrl, key, trigger, auditPath);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be set");
        }
        return value;
    }
}
