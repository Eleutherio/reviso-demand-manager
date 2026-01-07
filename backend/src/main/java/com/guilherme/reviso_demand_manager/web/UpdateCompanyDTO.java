package com.guilherme.reviso_demand_manager.web;

import com.guilherme.reviso_demand_manager.domain.CompanyType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateCompanyDTO(
        @NotBlank(message = "Nome é obrigatório")
        String name,

        @NotNull(message = "Tipo é obrigatório")
        CompanyType type,
        Boolean active,

        @NotBlank(message = "Segmento é obrigatório")
        String segment,

        @NotBlank(message = "Email de contato é obrigatório")
        @Email(message = "Email de contato inválido")
        String contactEmail,
        String site,
        List<String> usefulLinks
) {
}
