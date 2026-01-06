package com.guilherme.reviso_demand_manager.web;

import com.guilherme.reviso_demand_manager.domain.CompanyType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CompanyDTO(
    UUID id,
    String name,
    CompanyType type,
    Boolean active,
    OffsetDateTime createdAt
) {
}
