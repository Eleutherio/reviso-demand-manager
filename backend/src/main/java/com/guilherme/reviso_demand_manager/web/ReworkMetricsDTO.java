package com.guilherme.reviso_demand_manager.web;

public record ReworkMetricsDTO(
        Long reworkCount,
        Long totalCount,
        Double reworkPercentage
) {
}
