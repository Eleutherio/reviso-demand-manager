package com.guilherme.reviso_demand_manager.web;

import java.util.UUID;

public record RevisionDTO(
        String message,
        UUID actorId
) {
}
