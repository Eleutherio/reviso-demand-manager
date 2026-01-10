package com.guilherme.reviso_demand_manager.application;

import com.guilherme.reviso_demand_manager.domain.AgencyPasswordResetToken;
import com.guilherme.reviso_demand_manager.domain.User;
import com.guilherme.reviso_demand_manager.domain.UserRole;
import com.guilherme.reviso_demand_manager.infra.AgencyPasswordResetTokenRepository;
import com.guilherme.reviso_demand_manager.infra.EmailMessage;
import com.guilherme.reviso_demand_manager.infra.EmailSendStatus;
import com.guilherme.reviso_demand_manager.infra.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgencyPasswordRecoveryServiceTest {

    @Test
    void requestToken_retornaSent_quandoUsuarioNaoExiste() {
        UserRepository userRepository = mock(UserRepository.class);
        AgencyPasswordResetTokenRepository tokenRepository = mock(AgencyPasswordResetTokenRepository.class);
        EmailOutboxService emailOutboxService = mock(EmailOutboxService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        when(userRepository.findByEmailIgnoreCase("inexistente@agencia.com")).thenReturn(Optional.empty());

        AgencyPasswordRecoveryService service = new AgencyPasswordRecoveryService(
            userRepository,
            tokenRepository,
            emailOutboxService,
            passwordEncoder,
            15
        );

        EmailSendStatus status = service.requestToken("inexistente@agencia.com");

        assertEquals(EmailSendStatus.SENT, status);
        verify(tokenRepository, never()).save(any());
        verify(emailOutboxService, never()).enqueueAndSend(any());
    }

    @Test
    void requestToken_enfileiraEmail_paraUsuarioDeAgencia() {
        UserRepository userRepository = mock(UserRepository.class);
        AgencyPasswordResetTokenRepository tokenRepository = mock(AgencyPasswordResetTokenRepository.class);
        EmailOutboxService emailOutboxService = mock(EmailOutboxService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("user@agencia.com");
        user.setRole(UserRole.AGENCY_ADMIN);
        user.setActive(true);

        when(userRepository.findByEmailIgnoreCase("user@agencia.com")).thenReturn(Optional.of(user));
        when(emailOutboxService.enqueueAndSend(any())).thenReturn(EmailSendStatus.SENT);

        AgencyPasswordRecoveryService service = new AgencyPasswordRecoveryService(
            userRepository,
            tokenRepository,
            emailOutboxService,
            passwordEncoder,
            15
        );

        EmailSendStatus status = service.requestToken("user@agencia.com");

        assertEquals(EmailSendStatus.SENT, status);

        ArgumentCaptor<AgencyPasswordResetToken> tokenCaptor =
            ArgumentCaptor.forClass(AgencyPasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());

        AgencyPasswordResetToken saved = tokenCaptor.getValue();
        assertEquals(userId, saved.getUserId());
        assertEquals("user@agencia.com", saved.getEmail());
        assertNotNull(saved.getTokenHash());
        assertEquals(64, saved.getTokenHash().length());
        assertNotNull(saved.getExpiresAt());
        assertNotNull(saved.getCreatedAt());
        assertNull(saved.getUsedAt());

        ArgumentCaptor<EmailMessage> emailCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailOutboxService).enqueueAndSend(emailCaptor.capture());
        EmailMessage email = emailCaptor.getValue();
        assertEquals("user@agencia.com", email.toEmail());
        assertEquals("Token de recuperacao de senha", email.subject());
        assertTrue(email.body().contains("Use este token para continuar"));
    }

    @Test
    void resetPassword_retornaFalse_quandoTokenInvalido() {
        UserRepository userRepository = mock(UserRepository.class);
        AgencyPasswordResetTokenRepository tokenRepository = mock(AgencyPasswordResetTokenRepository.class);
        EmailOutboxService emailOutboxService = mock(EmailOutboxService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        when(tokenRepository.findTopByEmailAndTokenHashAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            eq("user@agencia.com"),
            any(),
            any(OffsetDateTime.class)
        )).thenReturn(Optional.empty());

        AgencyPasswordRecoveryService service = new AgencyPasswordRecoveryService(
            userRepository,
            tokenRepository,
            emailOutboxService,
            passwordEncoder,
            15
        );

        boolean result = service.resetPassword("user@agencia.com", "123456", "nova123");

        assertFalse(result);
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_atualizaSenha_quandoTokenValido() {
        UserRepository userRepository = mock(UserRepository.class);
        AgencyPasswordResetTokenRepository tokenRepository = mock(AgencyPasswordResetTokenRepository.class);
        EmailOutboxService emailOutboxService = mock(EmailOutboxService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("user@agencia.com");
        user.setRole(UserRole.AGENCY_ADMIN);
        user.setActive(true);

        AgencyPasswordResetToken token = new AgencyPasswordResetToken();
        token.setId(UUID.randomUUID());
        token.setUserId(userId);
        token.setEmail("user@agencia.com");
        token.setTokenHash("hash");
        token.setExpiresAt(OffsetDateTime.now().plusMinutes(5));
        token.setCreatedAt(OffsetDateTime.now());

        when(tokenRepository.findTopByEmailAndTokenHashAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            eq("user@agencia.com"),
            any(),
            any(OffsetDateTime.class)
        )).thenReturn(Optional.of(token));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("nova123")).thenReturn("hash-novo");

        AgencyPasswordRecoveryService service = new AgencyPasswordRecoveryService(
            userRepository,
            tokenRepository,
            emailOutboxService,
            passwordEncoder,
            15
        );

        boolean result = service.resetPassword("user@agencia.com", "123456", "nova123");

        assertTrue(result);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("hash-novo", userCaptor.getValue().getPasswordHash());

        verify(tokenRepository).save(token);
        assertNotNull(token.getUsedAt());
    }
}
