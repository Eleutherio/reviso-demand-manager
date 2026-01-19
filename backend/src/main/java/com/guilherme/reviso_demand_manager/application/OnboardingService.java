package com.guilherme.reviso_demand_manager.application;

import com.guilherme.reviso_demand_manager.domain.*;
import com.guilherme.reviso_demand_manager.infra.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class OnboardingService {

    private static final Logger log = LoggerFactory.getLogger(OnboardingService.class);

    private final AgencyRepository agencyRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final PendingSignupRepository pendingSignupRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailOutboxService emailService;
    private final StripeClient stripeClient;
    private final BillingConfig billingConfig;

    public OnboardingService(
            AgencyRepository agencyRepository,
            CompanyRepository companyRepository,
            UserRepository userRepository,
            SubscriptionRepository subscriptionRepository,
            SubscriptionPlanRepository planRepository,
            PendingSignupRepository pendingSignupRepository,
            PasswordEncoder passwordEncoder,
            EmailOutboxService emailService,
            StripeClient stripeClient,
            BillingConfig billingConfig
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
        this.billingConfig = billingConfig;
    }

    public String createCheckoutSession(UUID planId, String agencyName, String adminEmail, String adminPassword, String successUrl, String cancelUrl) throws Exception {
        var plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

        if (!plan.getActive()) {
            throw new IllegalArgumentException("Plan is not active");
        }

        if (userRepository.findByEmail(adminEmail).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Mock: provisiona direto sem Stripe
        if (billingConfig.isMock()) {
            return provisionAgencyMock(planId, agencyName, adminEmail, adminPassword);
        }

        // Stripe: checkout real
        if (plan.getStripePriceId() == null || plan.getStripePriceId().equals("CONFIGURE_IN_STRIPE")) {
            throw new IllegalArgumentException("Plan is not properly configured");
        }

        // Hash password IMMEDIATELY (never send to Stripe)
        var passwordHash = passwordEncoder.encode(adminPassword);

        // Metadata: NO PASSWORD (secure)
        var metadata = Map.of(
                "plan_id", planId.toString(),
                "agency_name", agencyName,
                "admin_email", adminEmail
        );

        var sessionJson = stripeClient.createCheckoutSession(plan.getStripePriceId(), successUrl, cancelUrl, metadata);
        var checkoutSessionId = extractSessionIdFromJson(sessionJson);

        // Store password hash locally (secure)
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

        log.info("Pending signup created: checkoutSession={}, email={}", 
                maskSensitive(checkoutSessionId), maskEmail(adminEmail));

        return sessionJson;
    }

    @Transactional
    private String provisionAgencyMock(UUID planId, String agencyName, String adminEmail, String adminPassword) {
        log.info("Mock provisioning: agency={}, email={}", agencyName, maskEmail(adminEmail));

        var passwordHash = passwordEncoder.encode(adminPassword);

        var agency = new Agency();
        agency.setId(UUID.randomUUID());
        agency.setName(agencyName);
        agency.setActive(true); // Ativo imediatamente (trial)
        agency.setCreatedAt(OffsetDateTime.now());
        agencyRepository.save(agency);

        var company = new Company();
        company.setId(UUID.randomUUID());
        company.setAgencyId(agency.getId());
        company.setCompanyCode("AG-" + agency.getId().toString().substring(0, 8).toUpperCase());
        company.setName(agencyName);
        company.setType(CompanyType.AGENCY);
        company.setSegment("ADMIN");
        company.setContactEmail(adminEmail);
        company.setActive(true);
        company.setCreatedAt(OffsetDateTime.now());
        companyRepository.save(company);

        var admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setFullName(adminEmail.split("@")[0]);
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordHash);
        admin.setRole(UserRole.AGENCY_ADMIN);
        admin.setAgencyId(agency.getId());
        admin.setCompanyId(company.getId());
        admin.setActive(true);
        admin.setCreatedAt(OffsetDateTime.now());
        userRepository.save(admin);

        var subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setAgencyId(agency.getId());
        subscription.setPlanId(planId);
        subscription.setStatus(SubscriptionStatus.TRIALING); // Trial mock
        subscription.setCurrentPeriodStart(OffsetDateTime.now());
        subscription.setCurrentPeriodEnd(OffsetDateTime.now().plusDays(billingConfig.getTrialDays()));
        subscription.setCreatedAt(OffsetDateTime.now());
        subscriptionRepository.save(subscription);

        emailService.enqueueAndSend(new EmailMessage(
                adminEmail,
                "Bem-vindo ao Reviso!",
                "Olá! Sua agência " + agencyName + " foi criada com sucesso.\n\nAcesse: http://localhost:4200\nEmail: " + adminEmail
        ));

        log.info("Mock agency provisioned: agencyId={}", agency.getId());

        // Retorna JSON mock (frontend espera sessionUrl)
        return "{\"url\":\"http://localhost:4200/login\",\"id\":\"mock_session\"}";
    }

    private String extractSessionIdFromJson(String json) {
        try {
            var start = json.indexOf("\"id\":\"");
            if (start == -1) {
                throw new IllegalArgumentException("Session ID not found in JSON");
            }
            start += 6;
            var end = json.indexOf("\"", start);
            if (end == -1) {
                throw new IllegalArgumentException("Invalid JSON format");
            }
            return json.substring(start, end);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Failed to parse session ID from JSON", e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public UUID provisionAgency(String checkoutSessionId, String stripeSubscriptionId, String stripeCustomerId) {
        log.info("Provisioning agency: subscription={}, checkoutSession={}", 
                maskSensitive(stripeSubscriptionId), maskSensitive(checkoutSessionId));

        // Idempotency: check by subscription_id
        var existingBySub = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);
        if (existingBySub.isPresent()) {
            log.info("Agency already provisioned (by subscription): agencyId={}", existingBySub.get().getAgencyId());
            return existingBySub.get().getAgencyId();
        }

        // Idempotency: check by checkout_session_id
        var existingByCheckout = subscriptionRepository.findByStripeCheckoutSessionId(checkoutSessionId);
        if (existingByCheckout.isPresent()) {
            log.info("Agency already provisioned (by checkout): agencyId={}", existingByCheckout.get().getAgencyId());
            return existingByCheckout.get().getAgencyId();
        }

        // Retrieve password hash from local storage (secure)
        var pendingSignup = pendingSignupRepository.findByCheckoutSessionId(checkoutSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Pending signup not found"));

        if (userRepository.findByEmail(pendingSignup.getAdminEmail()).isPresent()) {
            log.warn("Provisioning failed: email already registered: {}", maskEmail(pendingSignup.getAdminEmail()));
            throw new IllegalArgumentException("Email already registered");
        }

        try {
            var agency = new Agency();
            agency.setId(UUID.randomUUID());
            agency.setName(pendingSignup.getAgencyName());
            agency.setActive(false);
            agency.setCreatedAt(OffsetDateTime.now());
            agencyRepository.save(agency);
            log.debug("Agency created: id={}", agency.getId());

            var company = new Company();
            company.setId(UUID.randomUUID());
            company.setAgencyId(agency.getId());
            company.setCompanyCode("AG-" + agency.getId().toString().substring(0, 8).toUpperCase());
            company.setName(pendingSignup.getAgencyName());
            company.setType(CompanyType.AGENCY);
            company.setSegment("ADMIN");
            company.setContactEmail(pendingSignup.getAdminEmail());
            company.setActive(true);
            company.setCreatedAt(OffsetDateTime.now());
            companyRepository.save(company);
            log.debug("Company created: id={}", company.getId());

            var admin = new User();
            admin.setId(UUID.randomUUID());
            admin.setFullName(pendingSignup.getAdminEmail().split("@")[0]);
            admin.setEmail(pendingSignup.getAdminEmail());
            admin.setPasswordHash(pendingSignup.getPasswordHash()); // Use stored hash
            admin.setRole(UserRole.AGENCY_ADMIN);
            admin.setAgencyId(agency.getId());
            admin.setCompanyId(company.getId());
            admin.setActive(true);
            admin.setCreatedAt(OffsetDateTime.now());
            userRepository.save(admin);
            log.debug("Admin user created: id={}", admin.getId());

            var subscription = new Subscription();
            subscription.setId(UUID.randomUUID());
            subscription.setAgencyId(agency.getId());
            subscription.setPlanId(pendingSignup.getPlanId());
            subscription.setStripeCheckoutSessionId(checkoutSessionId);
            subscription.setStripeSubscriptionId(stripeSubscriptionId);
            subscription.setStripeCustomerId(stripeCustomerId);
            subscription.setStatus(SubscriptionStatus.INCOMPLETE);
            subscription.setCreatedAt(OffsetDateTime.now());
            subscriptionRepository.save(subscription);
            log.debug("Subscription created: id={}", subscription.getId());

            // Cleanup: delete pending signup
            pendingSignupRepository.delete(pendingSignup);

            log.info("Agency provisioned successfully: agencyId={}, subscription={}", 
                    agency.getId(), maskSensitive(stripeSubscriptionId));
            return agency.getId();
        } catch (Exception e) {
            log.error("Failed to provision agency: subscription={}, error={}", 
                    maskSensitive(stripeSubscriptionId), e.getMessage(), e);
            throw new RuntimeException("Failed to provision agency: " + e.getMessage(), e);
        }
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

    @Transactional
    public void activateAgency(String stripeSubscriptionId, String stripeCustomerId) {
        log.info("Activating agency: subscription={}, customer={}", 
                maskSensitive(stripeSubscriptionId), maskSensitive(stripeCustomerId));

        var subscription = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        // Validate customer matches (prevent activation by wrong invoice)
        if (!subscription.getStripeCustomerId().equals(stripeCustomerId)) {
            log.error("Customer mismatch: expected={}, got={}", 
                    maskSensitive(subscription.getStripeCustomerId()), maskSensitive(stripeCustomerId));
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
            if (companies.isEmpty()) {
                throw new IllegalArgumentException("Company not found");
            }
            var company = companies.get(0);
            emailService.enqueueAndSend(new EmailMessage(
                    company.getContactEmail(),
                    "Bem-vindo ao Reviso!",
                    "Olá! Sua agência " + agency.getName() + " foi criada com sucesso.\n\nAcesse: http://localhost:4200\nEmail: " + company.getContactEmail()
            ));

            log.info("Agency activated successfully: agencyId={}, subscription={}", 
                    agency.getId(), maskSensitive(stripeSubscriptionId));
        } else {
            log.debug("Agency already active: agencyId={}", agency.getId());
        }
    }

    @Transactional
    public void handleSubscriptionUpdate(String stripeSubscriptionId, SubscriptionStatus newStatus, OffsetDateTime periodStart, OffsetDateTime periodEnd) {
        log.info("Handling subscription update: subscription={}, newStatus={}", 
                maskSensitive(stripeSubscriptionId), newStatus);

        var subscription = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        subscription.transitionTo(newStatus);
        subscription.setCurrentPeriodStart(periodStart);
        subscription.setCurrentPeriodEnd(periodEnd);
        subscriptionRepository.save(subscription);

        var agency = agencyRepository.findById(subscription.getAgencyId())
                .orElseThrow(() -> new IllegalArgumentException("Agency not found"));
        var wasActive = agency.getActive();
        agency.setActive(subscription.isActive());
        agencyRepository.save(agency);

        log.info("Subscription updated: agencyId={}, status={}, active={}", 
                agency.getId(), newStatus, agency.getActive());

        if (wasActive && !agency.getActive()) {
            log.warn("Agency deactivated: agencyId={}, subscription={}", 
                    agency.getId(), maskSensitive(stripeSubscriptionId));
        }
    }

    @Transactional
    public void handlePaymentFailed(String stripeSubscriptionId) {
        log.warn("Handling payment failure: subscription={}", maskSensitive(stripeSubscriptionId));

        var subscription = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        subscription.transitionTo(SubscriptionStatus.PAST_DUE);
        subscriptionRepository.save(subscription);

        var agency = agencyRepository.findById(subscription.getAgencyId())
                .orElseThrow(() -> new IllegalArgumentException("Agency not found"));
        
        // Keep active during grace period (Stripe handles retries)
        log.info("Payment failed: agencyId={}, subscription={}, status=PAST_DUE", 
                agency.getId(), maskSensitive(stripeSubscriptionId));
    }

    @Transactional
    public void handleSubscriptionDeleted(String stripeSubscriptionId) {
        log.warn("Handling subscription deletion: subscription={}", maskSensitive(stripeSubscriptionId));

        var subscription = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        subscription.transitionTo(SubscriptionStatus.CANCELED);
        subscriptionRepository.save(subscription);

        var agency = agencyRepository.findById(subscription.getAgencyId())
                .orElseThrow(() -> new IllegalArgumentException("Agency not found"));
        
        agency.setActive(false);
        agencyRepository.save(agency);

        log.info("Subscription canceled: agencyId={}, subscription={}, active=false", 
                agency.getId(), maskSensitive(stripeSubscriptionId));
    }

    public CheckoutStatus getCheckoutStatus(String checkoutSessionId) {
        var subscription = subscriptionRepository.findByStripeCheckoutSessionId(checkoutSessionId);
        
        if (subscription.isEmpty()) {
            return new CheckoutStatus("pending", null, null, null);
        }

        var sub = subscription.get();
        var agency = agencyRepository.findById(sub.getAgencyId())
                .orElseThrow(() -> new IllegalArgumentException("Agency not found for checkout session"));
        var user = userRepository.findByAgencyId(sub.getAgencyId())
                .stream()
                .filter(u -> u.getRole() == UserRole.AGENCY_ADMIN)
                .findFirst()
                .orElse(null);

        return new CheckoutStatus(
                mapStatus(sub.getStatus()),
                agency.getName(),
                user != null ? user.getEmail() : null,
                sub.getStatus() == SubscriptionStatus.ACTIVE
        );
    }

    private String mapStatus(SubscriptionStatus status) {
        return switch (status) {
            case INCOMPLETE, INCOMPLETE_EXPIRED -> "pending";
            case ACTIVE, TRIALING -> "active";
            case PAST_DUE, UNPAID -> "payment_failed";
            case CANCELED -> "canceled";
            default -> throw new IllegalArgumentException("Unknown status: " + status);
        };
    }

    public record CheckoutStatus(
            String status,
            String agencyName,
            String adminEmail,
            Boolean isActive
    ) {}
}
