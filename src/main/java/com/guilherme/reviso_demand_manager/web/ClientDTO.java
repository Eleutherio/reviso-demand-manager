package com.guilherme.reviso_demand_manager.web;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ClientDTO(
    UUID id,
    String name,
    String segment,
    Boolean active,
    OffsetDateTime createdAt
) {
}
