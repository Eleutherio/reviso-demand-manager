package com.guilherme.reviso_demand_manager.web;

import com.guilherme.reviso_demand_manager.domain.UserRole;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UserDTO(
    UUID id,
    String fullName,
    String email,
    UserRole role,
    UUID companyId,
    Boolean active,
    OffsetDateTime createdAt
) {
}
