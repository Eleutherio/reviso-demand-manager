package com.guilherme.reviso_demand_manager.domain;

public class AccessPolicy {

    public static boolean canLogin(SubscriptionStatus status) {
        // Permite login em qualquer status (exceto CANCELED)
        return status != SubscriptionStatus.CANCELED;
    }

    public static boolean canRead(SubscriptionStatus status) {
        // Permite leitura em qualquer status ativo ou trial expirado
        return status.isActive() || status == SubscriptionStatus.TRIAL_EXPIRED;
    }

    public static boolean canWrite(SubscriptionStatus status) {
        // Permite escrita apenas em status ativo
        return status.isActive();
    }

    public static boolean canAccessPremium(SubscriptionStatus status) {
        // Premium apenas para ACTIVE
        return status == SubscriptionStatus.ACTIVE;
    }

    public static boolean isBlocked(SubscriptionStatus status) {
        // Bloqueado se cancelado ou inadimplente
        return status == SubscriptionStatus.CANCELED || 
               status == SubscriptionStatus.UNPAID ||
               status == SubscriptionStatus.INCOMPLETE_EXPIRED;
    }

    public static String getBlockReason(SubscriptionStatus status) {
        return switch (status) {
            case TRIAL_EXPIRED -> "Trial expirado. Faça upgrade para continuar.";
            case PAST_DUE -> "Pagamento pendente. Atualize seu método de pagamento.";
            case UNPAID -> "Assinatura suspensa por falta de pagamento.";
            case CANCELED -> "Assinatura cancelada.";
            case INCOMPLETE_EXPIRED -> "Checkout expirado. Inicie um novo signup.";
            default -> "Acesso bloqueado.";
        };
    }
}
