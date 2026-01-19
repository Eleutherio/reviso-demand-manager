package com.guilherme.reviso_demand_manager.application;

import java.util.UUID;

public interface BillingService {

    /**
     * Inicia checkout (Stripe) ou provisiona trial (Mock)
     * @return sessionUrl (Stripe) ou redirect URL (Mock)
     */
    String startCheckoutOrTrial(UUID planId, String agencyName, String adminEmail, String adminPassword, String successUrl, String cancelUrl) throws Exception;

    /**
     * Confirma pagamento e ativa agência (apenas Stripe)
     */
    void onPaymentConfirmed(String subscriptionId, String customerId);

    /**
     * Retorna status da subscription
     */
    SubscriptionStatusDTO getSubscriptionStatus(UUID agencyId);

    record SubscriptionStatusDTO(
            String status,
            String planName,
            String expiresAt,
            Boolean isActive
    ) {}
}
