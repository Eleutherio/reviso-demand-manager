package com.guilherme.reviso_demand_manager.web;

import com.guilherme.reviso_demand_manager.domain.AgencyDepartment;
import com.guilherme.reviso_demand_manager.domain.RequestPriority;
import com.guilherme.reviso_demand_manager.domain.RequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateRequestDTO(
    @NotNull(message = "ID da empresa é obrigatório")
    UUID companyId,

    UUID briefingId,
    
    @NotBlank(message = "Título é obrigatório")
    String title,
    
    String description,
    RequestType type,
    RequestPriority priority,

    @NotNull(message = "Departamento é obrigatório")
    AgencyDepartment department,

    OffsetDateTime dueDate
) {
}
