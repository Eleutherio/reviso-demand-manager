package com.guilherme.reviso_demand_manager.web;

import com.guilherme.reviso_demand_manager.domain.RequestEventType;
import com.guilherme.reviso_demand_manager.domain.RequestStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RequestEventDTO(
        UUID id,
        UUID requestId,
        UUID actorId,
        RequestEventType eventType,
        RequestStatus fromStatus,
        RequestStatus toStatus,
        String message,
        Boolean visibleToClient,
        Integer revisionNumber,
        OffsetDateTime createdAt
) {
}
