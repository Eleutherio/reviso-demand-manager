package com.guilherme.reviso_demand_manager.web;

public record CycleTimeDTO(
        Long avgDays,
        Long avgHours,
        Double avgSeconds
) {
}
