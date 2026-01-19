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
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "billing.provider", havingValue = "mock")
public class MockBillingService implements BillingService {

    private static final Logger log = LoggerFactory.getLogger(MockBillingService.class);

    private final AgencyRepository agencyRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailOutboxService emailService;
    private final BillingConfig billingConfig;

    public MockBillingService(
            AgencyRepository agencyRepository,
            CompanyRepository companyRepository,
            UserRepository userRepository,
            SubscriptionRepository subscriptionRepository,
            SubscriptionPlanRepository planRepository,
            PasswordEncoder passwordEncoder,
            EmailOutboxService emailService,
            BillingConfig billingConfig
    ) {
        this.agencyRepository = agencyRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.billingConfig = billingConfig;
    }

    @Override
    @Transactional
    public String startCheckoutOrTrial(UUID planId, String agencyName, String adminEmail, String adminPassword, String successUrl, String cancelUrl) {
        log.info("Mock billing: starting trial for agency={}, email={}", agencyName, maskEmail(adminEmail));

        var plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

        if (!plan.getActive()) {
            throw new IllegalArgumentException("Plan is not active");
        }

        if (userRepository.findByEmail(adminEmail).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        var passwordHash = passwordEncoder.encode(adminPassword);

        var agency = new Agency();
        agency.setId(UUID.randomUUID());
        agency.setName(agencyName);
        agency.setActive(true); // Ativo imediatamente
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
        subscription.setStatus(SubscriptionStatus.TRIALING);
        subscription.setCurrentPeriodStart(OffsetDateTime.now());
        subscription.setCurrentPeriodEnd(OffsetDateTime.now().plusDays(billingConfig.getTrialDays()));
        subscription.setCreatedAt(OffsetDateTime.now());
        subscriptionRepository.save(subscription);

        emailService.enqueueAndSend(new EmailMessage(
                adminEmail,
                "Bem-vindo ao Reviso!",
                "Olá! Sua agência " + agencyName + " foi criada com sucesso.\n\nTrial: " + billingConfig.getTrialDays() + " dias\n\nAcesse: " + successUrl.split("\\?")[0] + "\nEmail: " + adminEmail
        ));

        log.info("Mock billing: agency provisioned agencyId={}, trial={}days", agency.getId(), billingConfig.getTrialDays());

        // Retorna URL de sucesso (sem checkout)
        return successUrl.replace("{CHECKOUT_SESSION_ID}", "mock_trial");
    }

    @Override
    public void onPaymentConfirmed(String subscriptionId, String customerId) {
        // Mock não tem pagamento
        log.debug("Mock billing: onPaymentConfirmed ignored (no payment in mock)");
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

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        var parts = email.split("@");
        return parts[0].substring(0, Math.min(2, parts[0].length())) + "***@" + parts[1];
    }
}
