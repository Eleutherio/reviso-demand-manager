package com.guilherme.reviso_demand_manager.web;

import jakarta.validation.constraints.NotBlank;

public record CreateClientDTO(
    @NotBlank(message = "Name is required")
    String name,
    String segment
) {
}
