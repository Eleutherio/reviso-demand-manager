package com.guilherme.reviso_demand_manager.web;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BriefingDTO(
    UUID id,
    UUID companyId,
    UUID createdByUserId,
    String title,
    String description,
    String status,
    OffsetDateTime createdAt
) {
}
