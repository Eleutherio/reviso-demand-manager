package com.guilherme.reviso_demand_manager.infra;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeWebhookValidatorOfficial {

    private final String webhookSecret;

    public StripeWebhookValidatorOfficial(@Value("${stripe.webhook-secret:}") String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public boolean isValid(String payload, String signature) {
        if (webhookSecret == null || webhookSecret.isEmpty()) {
            return true; // Skip validation in dev/test
        }

        try {
            // Use official Stripe SDK (handles timestamp tolerance, parsing, etc.)
            Webhook.constructEvent(payload, signature, webhookSecret);
            return true;
        } catch (SignatureVerificationException e) {
            return false;
        }
    }
}
