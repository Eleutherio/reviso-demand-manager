package com.guilherme.reviso_demand_manager.web;

import com.guilherme.reviso_demand_manager.domain.CompanyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCompanyDTO(
    @NotBlank(message = "Nome é obrigatório")
    String name,
    
    @NotNull(message = "Tipo é obrigatório")
    CompanyType type
) {
}
