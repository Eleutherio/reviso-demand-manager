package com.guilherme.reviso_demand_manager.web;

public record RequestsByStatusDTO(
        String status,
        Long total
) {
}
