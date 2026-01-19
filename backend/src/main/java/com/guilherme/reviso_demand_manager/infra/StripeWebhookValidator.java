package com.guilherme.reviso_demand_manager.infra;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Service
public class StripeWebhookValidator {

    private final String webhookSecret;

    public StripeWebhookValidator(@Value("${stripe.webhook-secret:}") String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public boolean isValid(String payload, String signature) {
        if (webhookSecret == null || webhookSecret.isEmpty()) {
            return true; // Skip validation in dev/test
        }

        try {
            var parts = signature.split(",");
            String timestamp = null;
            String expectedSignature = null;

            for (var part : parts) {
                var kv = part.split("=", 2);
                if (kv.length == 2) {
                    if ("t".equals(kv[0])) timestamp = kv[1];
                    if ("v1".equals(kv[0])) expectedSignature = kv[1];
                }
            }

            if (timestamp == null || expectedSignature == null) return false;

            var signedPayload = timestamp + "." + payload;
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            var hash = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
            var computedSignature = bytesToHex(hash);

            return computedSignature.equals(expectedSignature);
        } catch (Exception e) {
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        var sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
