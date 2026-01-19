package com.guilherme.reviso_demand_manager.domain;

public enum SubscriptionStatus {
    INCOMPLETE,         // Checkout completed, awaiting payment (Stripe)
    INCOMPLETE_EXPIRED, // Payment window expired (Stripe)
    TRIALING,           // Trial period (Mock ou Stripe trial)
    TRIAL_EXPIRED,      // Trial expirado (Mock)
    ACTIVE,             // Payment confirmed, agency active (Stripe)
    PAST_DUE,           // Payment failed, grace period (Stripe)
    CANCELED,           // Subscription canceled
    UNPAID;             // Payment failed after retries (Stripe)

    public boolean isActive() {
        return this == ACTIVE || this == TRIALING;
    }

    public boolean canTransitionTo(SubscriptionStatus newStatus) {
        return switch (this) {
            case INCOMPLETE -> newStatus == ACTIVE || newStatus == INCOMPLETE_EXPIRED || newStatus == CANCELED;
            case INCOMPLETE_EXPIRED -> newStatus == CANCELED;
            case TRIALING -> newStatus == ACTIVE || newStatus == TRIAL_EXPIRED || newStatus == CANCELED;
            case TRIAL_EXPIRED -> newStatus == ACTIVE || newStatus == CANCELED;
            case ACTIVE -> newStatus == PAST_DUE || newStatus == CANCELED || newStatus == UNPAID;
            case PAST_DUE -> newStatus == ACTIVE || newStatus == CANCELED || newStatus == UNPAID;
            case UNPAID -> newStatus == ACTIVE || newStatus == CANCELED;
            case CANCELED -> false; // Terminal state
        };
    }
}
