package com.guilherme.reviso_demand_manager.infra;

public interface EmailProvider {
    EmailSendResult send(EmailMessage message);
}
