package com.guilherme.reviso_demand_manager.web;

import com.guilherme.reviso_demand_manager.domain.RequestPriority;
import com.guilherme.reviso_demand_manager.domain.RequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateRequestDTO(
    @NotNull(message = "Client ID is required")
    UUID clientId,
    
    @NotBlank(message = "Title is required")
    String title,
    
    String description,
    RequestType type,
    RequestPriority priority,
    OffsetDateTime dueDate
) {
}
