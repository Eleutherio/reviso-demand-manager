package com.guilherme.reviso_demand_manager.infra;

public record EmailSendResult(
    EmailSendStatus status,
    String providerId,
    String errorMessage
) {
}
