package com.guilherme.reviso_demand_manager.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginClientRequestDTO(
    @NotBlank(message = "Codigo da empresa e obrigatorio")
    String companyCode,

    @NotBlank(message = "Email e obrigatorio")
    @Email(message = "Email invalido")
    String email,

    @NotBlank(message = "Senha e obrigatoria")
    String password
) {
}
