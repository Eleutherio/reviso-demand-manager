package com.guilherme.reviso_demand_manager.web;

import com.guilherme.reviso_demand_manager.application.OnboardingService;
import com.guilherme.reviso_demand_manager.domain.SubscriptionStatus;
import com.guilherme.reviso_demand_manager.infra.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/onboarding")
public class OnboardingController {

    private static final Logger log = LoggerFactory.getLogger(OnboardingController.class);

    private final OnboardingService onboardingService;
    private final SubscriptionPlanRepository planRepository;
    private final StripeClient stripeClient;
    private final StripeWebhookValidatorOfficial webhookValidator;
    private final StripeWebhookEventRepository webhookEventRepository;
    private final RateLimitService rateLimitService;
    private final String frontendUrl;

    public OnboardingController(
            OnboardingService onboardingService,
            SubscriptionPlanRepository planRepository,
            StripeClient stripeClient,
            StripeWebhookValidatorOfficial webhookValidator,
            StripeWebhookEventRepository webhookEventRepository,
            RateLimitService rateLimitService,
            @Value("${frontend.base-url}") String frontendUrl
    ) {
        this.onboardingService = onboardingService;
        this.planRepository = planRepository;
        this.stripeClient = stripeClient;
        this.webhookValidator = webhookValidator;
        this.webhookEventRepository = webhookEventRepository;
        this.rateLimitService = rateLimitService;
        this.frontendUrl = frontendUrl;
    }

    @GetMapping("/plans")
    public List<SubscriptionPlanDTO> listPlans() {
        return planRepository.findByActiveTrue().stream()
                .map(plan -> {
                    try {
                        var priceJson = stripeClient.retrievePrice(plan.getStripePriceId());
                        var priceData = parseJson(priceJson);
                        var unitAmount = ((Number) priceData.get("unit_amount")).longValue();
                        var currency = (String) priceData.get("currency");
                        var recurring = (Map<String, Object>) priceData.get("recurring");
                        var interval = recurring != null ? (String) recurring.get("interval") : "month";

                        return new SubscriptionPlanDTO(
                                plan.getId(),
                                plan.getCode(),
                                plan.getName(),
                                plan.getStripePriceId(),
                                new BigDecimal(unitAmount).divide(new BigDecimal(100)),
                                currency.toUpperCase(),
                                interval,
                                plan.getMaxUsers(),
                                plan.getMaxRequestsPerMonth()
                        );
                    } catch (Exception e) {
                        log.error("Failed to fetch price from Stripe for plan {}", plan.getCode(), e);
                        return new SubscriptionPlanDTO(
                                plan.getId(),
                                plan.getCode(),
                                plan.getName(),
                                plan.getStripePriceId(),
                                null,
                                null,
                                null,
                                plan.getMaxUsers(),
                                plan.getMaxRequestsPerMonth()
                        );
                    }
                })
                .toList();
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(
            @Valid @RequestBody SignupRequestDTO request,
            HttpServletRequest httpRequest
    ) {
        // Rate limiting by IP
        var clientIp = getClientIp(httpRequest);
        if (!rateLimitService.isSignupAllowed(clientIp)) {
            log.warn("Rate limit exceeded for signup from IP: {}", clientIp);
            return ResponseEntity.status(429)
                    .body(Map.of("error", "Too many signup attempts. Try again later."));
        }

        try {
            var successUrl = frontendUrl + "/onboarding/success?session_id={CHECKOUT_SESSION_ID}";
            var cancelUrl = frontendUrl + "/onboarding/cancel";

            // Service validates plan and email (never trusts client)
            var sessionJson = onboardingService.createCheckoutSession(
                    request.planId(),
                    request.agencyName(),
                    request.adminEmail(),
                    request.adminPassword(),
                    successUrl,
                    cancelUrl
            );

            var url = extractUrlFromJson(sessionJson);
            return ResponseEntity.ok(new CheckoutSessionResponseDTO(url));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid signup request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create checkout session", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to process signup"));
        }
    }

    private String getClientIp(HttpServletRequest request) {
        var xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @GetMapping("/checkout-status")
    public ResponseEntity<?> checkoutStatus(@RequestParam String sessionId) {
        try {
            var status = onboardingService.getCheckoutStatus(sessionId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Failed to get checkout status", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to get status"));
        }
    }

    @PostMapping("/webhook/stripe")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Void> stripeWebhook(
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

        // Idempotency: atomic insert (returns null if duplicate)
        var inserted = webhookEventRepository.insertIfNotExists(eventId, type, OffsetDateTime.now());
        
        if (inserted == null) {
            log.info("webhook=stripe action=skipped eventId={} reason=already_processed", eventId);
            return ResponseEntity.ok().build();
        }

        // Process event (after lock acquired)
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
            // Return 500 so Stripe retries
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
                    onboardingService.activateAgency(subscriptionId, customerId);
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

    private String extractUrlFromJson(String json) {
        var start = json.indexOf("\"url\":\"") + 7;
        var end = json.indexOf("\"", start);
        return json.substring(start, end).replace("\\/", "/");
    }
}
