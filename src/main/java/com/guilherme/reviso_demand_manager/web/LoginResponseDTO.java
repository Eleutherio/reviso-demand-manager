package com.guilherme.reviso_demand_manager.web;

import com.guilherme.reviso_demand_manager.domain.UserRole;
import java.util.UUID;

public record LoginResponseDTO(
    String token,
    UUID userId,
    String fullName,
    String email,
    UserRole role,
    UUID companyId
) {
}
