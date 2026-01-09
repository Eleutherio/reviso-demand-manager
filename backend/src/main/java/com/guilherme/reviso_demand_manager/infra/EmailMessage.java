package com.guilherme.reviso_demand_manager.infra;

public record EmailMessage(
    String toEmail,
    String subject,
    String body
) {
}
