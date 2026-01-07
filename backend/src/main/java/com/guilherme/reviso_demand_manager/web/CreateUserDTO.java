package com.guilherme.reviso_demand_manager.web;

import com.guilherme.reviso_demand_manager.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateUserDTO(
    @NotBlank(message = "Nome completo é obrigatório")
    String fullName,
    
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    String email,
    
    @NotBlank(message = "Senha é obrigatória")
    String password,
    
    @NotNull(message = "Role é obrigatória")
    UserRole role,
    
    UUID companyId
) {
}
