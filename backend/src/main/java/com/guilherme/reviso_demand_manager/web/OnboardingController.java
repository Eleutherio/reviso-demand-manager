package com.guilherme.reviso_demand_manager.web;

import com.guilherme.reviso_demand_manager.application.BillingService;
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

    private final BillingService billingService;
    private final SubscriptionPlanRepository planRepository;
    private final RateLimitService rateLimitService;
    private final BillingConfig billingConfig;
    private final String frontendUrl;

    public OnboardingController(
            BillingService billingService,
            SubscriptionPlanRepository planRepository,
            RateLimitService rateLimitService,
            BillingConfig billingConfig,
            @Value("${frontend.base-url}") String frontendUrl
    ) {
        this.billingService = billingService;
        this.planRepository = planRepository;
        this.rateLimitService = rateLimitService;
        this.billingConfig = billingConfig;
        this.frontendUrl = frontendUrl;
    }

    @GetMapping("/plans")
    public List<SubscriptionPlanDTO> listPlans() {
        return planRepository.findByActiveTrue().stream()
                .map(plan -> new SubscriptionPlanDTO(
                        plan.getId(),
                        plan.getCode(),
                        plan.getName(),
                        null,
                        BigDecimal.ZERO,
                        "BRL",
                        "month",
                        plan.getMaxUsers(),
                        plan.getMaxRequestsPerMonth()
                ))
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

            var url = billingService.startCheckoutOrTrial(
                    request.planId(),
                    request.agencyName(),
                    request.adminEmail(),
                    request.adminPassword(),
                    successUrl,
                    cancelUrl
            );

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
        // Mock sempre retorna sucesso
        if (billingConfig.isMock()) {
            return ResponseEntity.ok(Map.of("status", "success", "message", "Trial ativo"));
        }

        // Stripe: consulta real
        try {
            var status = billingService.getSubscriptionStatus(null); // TODO: get from session
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Failed to get checkout status", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to get status"));
        }
    }
}
