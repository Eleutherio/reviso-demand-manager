package com.guilherme.reviso_demand_manager.web;

import com.guilherme.reviso_demand_manager.domain.AgencyDepartment;
import jakarta.validation.constraints.NotNull;

public record ConvertBriefingDTO(
        @NotNull(message = "Departamento é obrigatório")
        AgencyDepartment department
) {
}
