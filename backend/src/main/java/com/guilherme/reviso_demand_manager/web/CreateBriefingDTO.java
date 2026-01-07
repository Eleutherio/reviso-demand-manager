package com.guilherme.reviso_demand_manager.web;

import jakarta.validation.constraints.NotBlank;

public record CreateBriefingDTO(
    @NotBlank(message = "Título é obrigatório")
    String title,
    
    String description
) {
}
