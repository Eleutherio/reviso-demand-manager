package com.guilherme.reviso_demand_manager.web;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignRequestDTO(
        @NotNull(message = "assigneeId é obrigatório")
        UUID assigneeId,
        UUID actorId
) {
}
