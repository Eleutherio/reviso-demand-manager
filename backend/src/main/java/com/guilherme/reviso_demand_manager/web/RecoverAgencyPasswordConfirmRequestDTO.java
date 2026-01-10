package com.guilherme.reviso_demand_manager.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RecoverAgencyPasswordConfirmRequestDTO(
    @NotBlank(message = "Email e obrigatorio")
    @Email(message = "Email invalido")
    String email,
    @NotBlank(message = "Token e obrigatorio")
    String token,
    @NotBlank(message = "Senha e obrigatoria")
    String newPassword
) {
}
