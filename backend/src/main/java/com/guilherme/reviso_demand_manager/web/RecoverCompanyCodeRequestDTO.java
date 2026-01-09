package com.guilherme.reviso_demand_manager.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RecoverCompanyCodeRequestDTO(
    @NotBlank(message = "Email e obrigatorio")
    @Email(message = "Email invalido")
    String email
) {
}
