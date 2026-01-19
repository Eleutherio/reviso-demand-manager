package com.guilherme.reviso_demand_manager.application;

import com.guilherme.reviso_demand_manager.domain.*;
import com.guilherme.reviso_demand_manager.infra.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "billing.provider", havingValue = "stripe")
public class StripeBillingService implements BillingService {

    private static final Logger log = LoggerFactory.getLogger(StripeBillingService.class);

    private final AgencyRepository agencyRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final PendingSignupRepository pendingSignupRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailOutboxService emailService;
    private final StripeClient stripeClient;

    public StripeBillingService(
            AgencyRepository agencyRepository,
            CompanyRepository companyRepository,
            UserRepository userRepository,
            SubscriptionRepository subscriptionRepository,
            SubscriptionPlanRepository planRepository,
            PendingSignupRepository pendingSignupRepository,
            PasswordEncoder passwordEncoder,
            EmailOutboxService emailService,
            StripeClient stripeClient
    ) {
        this.agencyRepository = agencyRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.pendingSignupRepository = pendingSignupRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.stripeClient = stripeClient;
    }

    @Override
    public String startCheckoutOrTrial(UUID planId, String agencyName, String adminEmail, String adminPassword, String successUrl, String cancelUrl) throws Exception {
        log.info("Stripe billing: creating checkout for agency={}, email={}", agencyName, maskEmail(adminEmail));

        var plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

        if (!plan.getActive()) {
            throw new IllegalArgumentException("Plan is not active");
        }

        if (plan.getStripePriceId() == null || plan.getStripePriceId().equals("CONFIGURE_IN_STRIPE")) {
            throw new IllegalArgumentException("Plan is not properly configured");
        }

        if (userRepository.findByEmail(adminEmail).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        var passwordHash = passwordEncoder.encode(adminPassword);

        var metadata = Map.of(
                "plan_id", planId.toString(),
                "agency_name", agencyName,
                "admin_email", adminEmail
        );

        var sessionJson = stripeClient.createCheckoutSession(plan.getStripePriceId(), successUrl, cancelUrl, metadata);
        var checkoutSessionId = extractSessionIdFromJson(sessionJson);

        var pendingSignup = new PendingSignup();
        pendingSignup.setId(UUID.randomUUID());
        pendingSignup.setCheckoutSessionId(checkoutSessionId);
        pendingSignup.setPlanId(planId);
        pendingSignup.setAgencyName(agencyName);
        pendingSignup.setAdminEmail(adminEmail);
        pendingSignup.setPasswordHash(passwordHash);
        pendingSignup.setCreatedAt(OffsetDateTime.now());
        pendingSignup.setExpiresAt(OffsetDateTime.now().plusHours(24));
        pendingSignupRepository.save(pendingSignup);

        log.info("Stripe billing: pending signup created checkoutSession={}", maskSensitive(checkoutSessionId));

        return extractUrlFromJson(sessionJson);
    }

    @Override
    @Transactional
    public void onPaymentConfirmed(String subscriptionId, String customerId) {
        log.info("Stripe billing: payment confirmed subscription={}", maskSensitive(subscriptionId));

        var subscription = subscriptionRepository.findByStripeSubscriptionId(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        if (!subscription.getStripeCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("Customer ID mismatch");
        }

        var agency = agencyRepository.findById(subscription.getAgencyId())
                .orElseThrow(() -> new IllegalArgumentException("Agency not found"));

        if (!agency.getActive()) {
            subscription.transitionTo(SubscriptionStatus.ACTIVE);
            subscriptionRepository.save(subscription);

            agency.setActive(true);
            agencyRepository.save(agency);

            var companies = companyRepository.findByAgencyIdAndTypeOrderByNameAsc(subscription.getAgencyId(), CompanyType.AGENCY);
            if (!companies.isEmpty()) {
                var company = companies.get(0);
                emailService.enqueueAndSend(new EmailMessage(
                        company.getContactEmail(),
                        "Bem-vindo ao Reviso!",
                        "Olá! Sua agência " + agency.getName() + " foi ativada com sucesso."
                ));
            }

            log.info("Stripe billing: agency activated agencyId={}", agency.getId());
        }
    }

    @Override
    public SubscriptionStatusDTO getSubscriptionStatus(UUID agencyId) {
        var subscription = subscriptionRepository.findByAgencyId(agencyId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        var plan = planRepository.findById(subscription.getPlanId())
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

        return new SubscriptionStatusDTO(
                subscription.getStatus().name(),
                plan.getName(),
                subscription.getCurrentPeriodEnd() != null ? subscription.getCurrentPeriodEnd().toString() : null,
                subscription.isActive()
        );
    }

    private String extractSessionIdFromJson(String json) {
        var start = json.indexOf("\"id\":\"") + 6;
        var end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private String extractUrlFromJson(String json) {
        var start = json.indexOf("\"url\":\"") + 7;
        var end = json.indexOf("\"", start);
        return json.substring(start, end).replace("\\/", "/");
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        var parts = email.split("@");
        return parts[0].substring(0, Math.min(2, parts[0].length())) + "***@" + parts[1];
    }

    private String maskSensitive(Object value) {
        if (value == null) return "***";
        var str = value.toString();
        if (str.length() < 8) return "***";
        return str.substring(0, 8) + "***";
    }
}
