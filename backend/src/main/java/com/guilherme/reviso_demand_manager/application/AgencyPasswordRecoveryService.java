package com.guilherme.reviso_demand_manager.application;

import com.guilherme.reviso_demand_manager.domain.AgencyPasswordResetToken;
import com.guilherme.reviso_demand_manager.domain.User;
import com.guilherme.reviso_demand_manager.domain.UserRole;
import com.guilherme.reviso_demand_manager.infra.AgencyPasswordResetTokenRepository;
import com.guilherme.reviso_demand_manager.infra.EmailMessage;
import com.guilherme.reviso_demand_manager.infra.EmailSendStatus;
import com.guilherme.reviso_demand_manager.infra.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
public class AgencyPasswordRecoveryService {

    private final UserRepository userRepository;
    private final AgencyPasswordResetTokenRepository tokenRepository;
    private final EmailOutboxService emailOutboxService;
    private final PasswordEncoder passwordEncoder;
    private final long tokenTtlMinutes;
    private final SecureRandom secureRandom = new SecureRandom();

    public AgencyPasswordRecoveryService(
        UserRepository userRepository,
        AgencyPasswordResetTokenRepository tokenRepository,
        EmailOutboxService emailOutboxService,
        PasswordEncoder passwordEncoder,
        @Value("${agency-password-recovery.token-ttl-minutes:15}") long tokenTtlMinutes
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailOutboxService = emailOutboxService;
        this.passwordEncoder = passwordEncoder;
        this.tokenTtlMinutes = tokenTtlMinutes;
    }

    @Transactional
    public EmailSendStatus requestToken(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getActive()) || !isAgencyRole(user.getRole())) {
            return EmailSendStatus.SENT;
        }

        String token = generateToken();
        OffsetDateTime now = now();
        tokenRepository.markUsedByEmail(email, now);

        AgencyPasswordResetToken entity = new AgencyPasswordResetToken();
        entity.setId(UUID.randomUUID());
        entity.setUserId(user.getId());
        entity.setEmail(email);
        entity.setTokenHash(hashToken(token));
        entity.setExpiresAt(now.plusMinutes(tokenTtlMinutes));
        entity.setCreatedAt(now);
        tokenRepository.save(entity);

        EmailMessage message = new EmailMessage(
            email,
            "Token de recuperacao de senha",
            buildEmailBody(token)
        );
        return emailOutboxService.enqueueAndSend(message);
    }

    @Transactional
    public boolean resetPassword(String rawEmail, String rawToken, String newPassword) {
        String email = normalizeEmail(rawEmail);
        String token = normalizeToken(rawToken);
        if (email.isBlank() || token.isBlank() || newPassword == null || newPassword.isBlank()) {
            return false;
        }

        OffsetDateTime now = now();
        String tokenHash = hashToken(token);
        AgencyPasswordResetToken entity = tokenRepository
            .findTopByEmailAndTokenHashAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                email,
                tokenHash,
                now
            )
            .orElse(null);
        if (entity == null) {
            return false;
        }

        User user = userRepository.findById(entity.getUserId()).orElse(null);
        if (user == null
            || !Boolean.TRUE.equals(user.getActive())
            || !isAgencyRole(user.getRole())
            || !email.equalsIgnoreCase(user.getEmail())) {
            return false;
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        entity.setUsedAt(now);
        tokenRepository.save(entity);
        return true;
    }

    private boolean isAgencyRole(UserRole role) {
        return role == UserRole.AGENCY_ADMIN || role == UserRole.AGENCY_USER;
    }

    private String normalizeEmail(String rawEmail) {
        if (rawEmail == null) return "";
        return rawEmail.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeToken(String rawToken) {
        if (rawToken == null) return "";
        return rawToken.trim().toUpperCase(Locale.ROOT);
    }

    private String generateToken() {
        int value = secureRandom.nextInt(1_000_000);
        return String.format(Locale.ROOT, "%06d", value);
    }

    private String buildEmailBody(String token) {
        StringBuilder builder = new StringBuilder();
        builder.append("Recebemos seu pedido para recuperar sua senha.\n\n");
        builder.append("Use este token para continuar:\n");
        builder.append(token).append("\n\n");
        builder.append("Este token expira em ").append(tokenTtlMinutes).append(" minutos.\n");
        builder.append("Se voce nao solicitou, ignore este email.");
        return builder.toString();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Hashing nao disponivel", ex);
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
