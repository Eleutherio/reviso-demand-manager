package com.guilherme.reviso_demand_manager.domain;

public enum SubscriptionStatus {
    INCOMPLETE,      // Checkout completed, awaiting payment
    INCOMPLETE_EXPIRED, // Payment window expired
    TRIALING,        // In trial period (if configured)
    ACTIVE,          // Payment confirmed, agency active
    PAST_DUE,        // Payment failed, grace period
    CANCELED,        // Subscription canceled
    UNPAID;          // Payment failed after retries

    public boolean isActive() {
        return this == ACTIVE || this == TRIALING;
    }

    public boolean canTransitionTo(SubscriptionStatus newStatus) {
        return switch (this) {
            case INCOMPLETE -> newStatus == ACTIVE || newStatus == INCOMPLETE_EXPIRED || newStatus == CANCELED;
            case INCOMPLETE_EXPIRED -> newStatus == CANCELED;
            case TRIALING -> newStatus == ACTIVE || newStatus == CANCELED;
            case ACTIVE -> newStatus == PAST_DUE || newStatus == CANCELED || newStatus == UNPAID;
            case PAST_DUE -> newStatus == ACTIVE || newStatus == CANCELED || newStatus == UNPAID;
            case UNPAID -> newStatus == ACTIVE || newStatus == CANCELED;
            case CANCELED -> false; // Terminal state
        };
    }
}
