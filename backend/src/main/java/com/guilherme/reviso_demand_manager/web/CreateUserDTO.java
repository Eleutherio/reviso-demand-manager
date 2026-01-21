package com.guilherme.reviso_demand_manager.web;

import com.guilherme.reviso_demand_manager.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateUserDTO(
    @NotBlank(message = "Nome completo e obrigatorio")
    String fullName,

    @NotBlank(message = "Email e obrigatorio")
    @Email(message = "Email invalido")
    String email,

    @NotBlank(message = "Senha e obrigatoria")
    String password,

    @NotNull(message = "Role e obrigatoria")
    UserRole role,

    UUID companyId,
    String companyCode,
    UUID accessProfileId
) {
}
