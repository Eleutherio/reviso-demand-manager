package com.guilherme.reviso_demand_manager.web;

import com.guilherme.reviso_demand_manager.application.OnboardingService;
import com.guilherme.reviso_demand_manager.application.StripeBillingService;
import com.guilherme.reviso_demand_manager.domain.SubscriptionStatus;
import com.guilherme.reviso_demand_manager.infra.StripeWebhookEventRepository;
import com.guilherme.reviso_demand_manager.infra.StripeWebhookValidatorOfficial;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;

@RestController
@RequestMapping("/onboarding/webhook")
@ConditionalOnProperty(name = "billing.provider", havingValue = "stripe")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final StripeBillingService billingService;
    private final OnboardingService onboardingService;
    private final StripeWebhookValidatorOfficial webhookValidator;
    private final StripeWebhookEventRepository webhookEventRepository;

    public StripeWebhookController(
            StripeBillingService billingService,
            OnboardingService onboardingService,
            StripeWebhookValidatorOfficial webhookValidator,
            StripeWebhookEventRepository webhookEventRepository
    ) {
        this.billingService = billingService;
        this.onboardingService = onboardingService;
        this.webhookValidator = webhookValidator;
        this.webhookEventRepository = webhookEventRepository;
    }

    @PostMapping("/stripe")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature
    ) {
        var startTime = System.currentTimeMillis();

        if (!webhookValidator.isValid(payload, signature)) {
            log.warn("webhook=stripe action=signature_invalid");
            return ResponseEntity.status(401).build();
        }

        Map<String, Object> event;
        try {
            event = parseJson(payload);
        } catch (Exception e) {
            log.error("webhook=stripe action=parse_failed error={}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        var eventId = (String) event.get("id");
        var type = (String) event.get("type");

        log.info("webhook=stripe action=received eventId={} eventType={}", eventId, type);

        // Idempotency
        var inserted = webhookEventRepository.insertIfNotExists(eventId, type, OffsetDateTime.now());

        if (inserted == null) {
            log.info("webhook=stripe action=skipped eventId={} reason=already_processed", eventId);
            return ResponseEntity.ok().build();
        }

        try {
            processWebhookEvent(event);

            var duration = System.currentTimeMillis() - startTime;
            log.info("webhook=stripe action=processed eventId={} eventType={} duration={}ms",
                    eventId, type, duration);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            var duration = System.currentTimeMillis() - startTime;
            log.error("webhook=stripe action=failed eventId={} eventType={} duration={}ms error={}",
                    eventId, type, duration, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    private void processWebhookEvent(Map<String, Object> event) {
        var type = (String) event.get("type");
        var data = (Map<String, Object>) event.get("data");
        var object = (Map<String, Object>) data.get("object");

        switch (type) {
            case "checkout.session.completed" -> {
                var sessionId = (String) object.get("id");
                var subscriptionId = (String) object.get("subscription");
                var customerId = (String) object.get("customer");

                onboardingService.provisionAgency(sessionId, subscriptionId, customerId);
            }
            case "invoice.paid" -> {
                var subscriptionId = (String) object.get("subscription");
                var customerId = (String) object.get("customer");
                if (subscriptionId != null && customerId != null) {
                    billingService.onPaymentConfirmed(subscriptionId, customerId);
                }
            }
            case "invoice.payment_failed" -> {
                var subscriptionId = (String) object.get("subscription");
                if (subscriptionId != null) {
                    onboardingService.handlePaymentFailed(subscriptionId);
                }
            }
            case "customer.subscription.updated" -> {
                var subscriptionId = (String) object.get("id");
                var statusStr = (String) object.get("status");
                var status = SubscriptionStatus.valueOf(statusStr.toUpperCase());
                var periodStart = OffsetDateTime.ofInstant(Instant.ofEpochSecond(((Number) object.get("current_period_start")).longValue()), ZoneId.systemDefault());
                var periodEnd = OffsetDateTime.ofInstant(Instant.ofEpochSecond(((Number) object.get("current_period_end")).longValue()), ZoneId.systemDefault());

                onboardingService.handleSubscriptionUpdate(subscriptionId, status, periodStart, periodEnd);
            }
            case "customer.subscription.deleted" -> {
                var subscriptionId = (String) object.get("id");
                onboardingService.handleSubscriptionDeleted(subscriptionId);
            }
        }
    }

    private Map<String, Object> parseJson(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }
}
