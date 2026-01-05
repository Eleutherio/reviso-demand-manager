package com.guilherme.reviso_demand_manager.web;

import com.guilherme.reviso_demand_manager.domain.RequestPriority;
import com.guilherme.reviso_demand_manager.domain.RequestStatus;
import com.guilherme.reviso_demand_manager.domain.RequestType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RequestDTO(
    UUID id,
    UUID clientId,
    String title,
    String description,
    RequestType type,
    RequestPriority priority,
    RequestStatus status,
    UUID assigneeId,
    OffsetDateTime dueDate,
    Integer revisionCount,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
