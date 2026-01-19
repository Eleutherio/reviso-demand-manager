package com.guilherme.reviso_demand_manager.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SignupRequestDTO(
        @NotNull UUID planId,
        @NotBlank String agencyName,
        @NotBlank @Email String adminEmail,
        @NotBlank String adminPassword
) {}
