package com.guilherme.reviso_demand_manager.web;

import java.math.BigDecimal;
import java.util.UUID;

public record SubscriptionPlanDTO(
        UUID id,
        String code,
        String name,
        String stripePriceId,
        BigDecimal price,
        String currency,
        String interval,
        Integer maxUsers,
        Integer maxRequestsPerMonth,
        Integer maxCompanies
) {}
