package com.guilherme.reviso_demand_manager.web;

import com.guilherme.reviso_demand_manager.domain.RequestStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ChangeStatusDTO(
        @NotNull(message = "Novo status é obrigatório")
        RequestStatus toStatus,
        String message,
        UUID actorId
) {
}
