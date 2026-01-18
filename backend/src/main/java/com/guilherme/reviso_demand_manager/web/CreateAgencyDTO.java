package com.guilherme.reviso_demand_manager.web;

import jakarta.validation.constraints.NotBlank;

public record CreateAgencyDTO(
    @NotBlank(message = "Nome e obrigatorio")
    String name
) {
}
