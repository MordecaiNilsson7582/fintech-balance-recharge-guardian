package com.balanceguard.client;

import com.balanceguard.config.InfraiConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public final class InfraiClient {
    private final HttpClient http;
    private final URI baseUrl;
    private final String apiKey;

    public InfraiClient(InfraiConfig config) {
        this(HttpClient.newBuilder().build(), config.baseUrl(), config.apiKey());
    }

    InfraiClient(HttpClient http, URI baseUrl, String apiKey) {
        this.http = http;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public void configureAutoRecharge(double triggerBalance, double rechargeAmount) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("trigger_balance", triggerBalance);
        body.put("recharge_amount", rechargeAmount);
        request("PUT", "/v1/account/autorecharge/configure", body);
    }

    public String sendRechargeNotice(String to, String subject, String text) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("to", to);
        body.put("subject", subject);
        body.put("body", text);
        Map<String, Object> data = request("POST", "/v1/email/send", body);
        Object messageId = data.get("message_id");
        if (!(messageId instanceof String id) || id.isBlank()) {
            throw new IllegalStateException("email.send response omitted message_id");
        }
        return id;
    }

    private Map<String, Object> request(String method, String path, Map<String, Object> body) {
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(baseUrl.resolve(path))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(JsonCodec.encode(body)))
                    .build();
            try {
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                Map<String, Object> envelope = JsonCodec.decodeObject(response.body());
                if (response.statusCode() == 429 && attempt < 3) {
                    pause(retryDelay(response, attempt));
                    continue;
                }
                if (!Boolean.TRUE.equals(envelope.get("ok"))) {
                    throw envelopeError(envelope, response.statusCode());
                }
                if (response.statusCode() >= 500) {
                    throw new InfraiException("HTTP_" + response.statusCode(), "Infrai request was not accepted", response.statusCode());
                }
                Object data = envelope.get("data");
                if (data instanceof Map<?, ?> map) {
                    Map<String, Object> result = new LinkedHashMap<>();
                    map.forEach((key, value) -> result.put(String.valueOf(key), value));
                    return result;
                }
                return Map.of();
            } catch (IOException e) {
                throw new IllegalStateException("Could not reach Infrai", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Infrai request interrupted", e);
            }
        }
        throw new IllegalStateException("Retry attempts exhausted");
    }

    private static InfraiException envelopeError(Map<String, Object> envelope, int status) {
        Object rawError = envelope.get("error");
        if (rawError instanceof Map<?, ?> error) {
            String code = String.valueOf(error.containsKey("code") ? error.get("code") : "REQUEST_REJECTED");
            String message = String.valueOf(error.containsKey("message") ? error.get("message") : "Request rejected");
            return new InfraiException(code, message, status);
        }
        return new InfraiException("REQUEST_REJECTED", "Request rejected", status);
    }

    private static Duration retryDelay(HttpResponse<?> response, int attempt) {
        String value = response.headers().firstValue("Retry-After").orElse("");
        try {
            return Duration.ofSeconds(Math.max(0, Long.parseLong(value)));
        } catch (NumberFormatException ignored) {
            try {
                long millis = Duration.between(ZonedDateTime.now(), ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)).toMillis();
                return Duration.ofMillis(Math.max(0, millis));
            } catch (RuntimeException invalidDate) {
                return Duration.ofMillis(250L * (1L << attempt));
            }
        }
    }

    private static void pause(Duration delay) throws InterruptedException {
        Thread.sleep(delay.toMillis());
    }
}
