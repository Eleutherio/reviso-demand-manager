package com.guilherme.reviso_demand_manager.web;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CommentDTO(
        @NotBlank(message = "Mensagem é obrigatória")
        String message,
        UUID actorId,
        Boolean visibleToClient
) {
}
