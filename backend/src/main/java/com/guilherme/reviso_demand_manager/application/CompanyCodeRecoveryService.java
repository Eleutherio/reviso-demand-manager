package com.guilherme.reviso_demand_manager.application;

import com.guilherme.reviso_demand_manager.domain.Company;
import com.guilherme.reviso_demand_manager.domain.CompanyType;
import com.guilherme.reviso_demand_manager.domain.User;
import com.guilherme.reviso_demand_manager.domain.UserRole;
import com.guilherme.reviso_demand_manager.infra.CompanyRepository;
import com.guilherme.reviso_demand_manager.infra.EmailMessage;
import com.guilherme.reviso_demand_manager.infra.EmailSendStatus;
import com.guilherme.reviso_demand_manager.infra.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class CompanyCodeRecoveryService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final EmailOutboxService emailOutboxService;

    public CompanyCodeRecoveryService(
        CompanyRepository companyRepository,
        UserRepository userRepository,
        EmailOutboxService emailOutboxService
    ) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.emailOutboxService = emailOutboxService;
    }

    public EmailSendStatus sendRecoveryEmail(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        List<String> companyCodes = resolveActiveCompanyCodes(email);
        if (companyCodes.isEmpty()) {
            return EmailSendStatus.SENT;
        }
        EmailMessage message = new EmailMessage(
            email,
            "Seus codigos de empresa",
            buildCompanyCodeRecoveryBody(companyCodes)
        );
        return emailOutboxService.enqueueAndSend(message);
    }

    List<String> resolveActiveCompanyCodes(String email) {
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            return List.of();
        }

        if (!Boolean.TRUE.equals(user.getActive())
            || user.getRole() != UserRole.CLIENT_USER
            || user.getCompanyId() == null) {
            return List.of();
        }

        Company company = companyRepository.findById(user.getCompanyId()).orElse(null);
        if (company == null
            || !Boolean.TRUE.equals(company.getActive())
            || company.getType() != CompanyType.CLIENT
            || !isValidCode(company.getCompanyCode())) {
            return List.of();
        }

        return List.of(company.getCompanyCode());
    }

    private String normalizeEmail(String rawEmail) {
        if (rawEmail == null) return "";
        return rawEmail.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isValidCode(String code) {
        return code != null && !code.isBlank();
    }

    private String buildCompanyCodeRecoveryBody(List<String> companyCodes) {
        StringBuilder builder = new StringBuilder();
        builder.append("Recebemos seu pedido para recuperar o codigo da empresa.\n\n");
        builder.append("Codigos ativos associados a este email:\n");
        for (String code : companyCodes) {
            builder.append("- ").append(code).append('\n');
        }
        builder.append("\nSe voce nao solicitou, ignore este email.");
        return builder.toString();
    }
}
