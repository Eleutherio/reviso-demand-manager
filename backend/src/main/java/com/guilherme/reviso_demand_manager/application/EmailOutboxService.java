package com.guilherme.reviso_demand_manager.application;

import com.guilherme.reviso_demand_manager.domain.EmailOutbox;
import com.guilherme.reviso_demand_manager.domain.EmailOutboxStatus;
import com.guilherme.reviso_demand_manager.infra.EmailMessage;
import com.guilherme.reviso_demand_manager.infra.EmailOutboxRepository;
import com.guilherme.reviso_demand_manager.infra.EmailProvider;
import com.guilherme.reviso_demand_manager.infra.EmailSendResult;
import com.guilherme.reviso_demand_manager.infra.EmailSendStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class EmailOutboxService {

    private final EmailOutboxRepository outboxRepository;
    private final EmailProvider emailProvider;
    private final int maxAttempts;
    private final long retryDelaySeconds;
    private final long quotaDelayMinutes;
    private final int batchSize;

    public EmailOutboxService(
        EmailOutboxRepository outboxRepository,
        EmailProvider emailProvider,
        @Value("${email.queue.max-attempts:5}") int maxAttempts,
        @Value("${email.queue.retry-delay-seconds:60}") long retryDelaySeconds,
        @Value("${email.queue.quota-delay-minutes:60}") long quotaDelayMinutes,
        @Value("${email.queue.batch-size:20}") int batchSize
    ) {
        this.outboxRepository = outboxRepository;
        this.emailProvider = emailProvider;
        this.maxAttempts = maxAttempts;
        this.retryDelaySeconds = retryDelaySeconds;
        this.quotaDelayMinutes = quotaDelayMinutes;
        this.batchSize = batchSize;
    }

    @Transactional
    public EmailSendStatus enqueueAndSend(EmailMessage message) {
        OffsetDateTime now = now();

        EmailOutbox outbox = new EmailOutbox();
        outbox.setId(UUID.randomUUID());
        outbox.setToEmail(message.toEmail());
        outbox.setSubject(message.subject());
        outbox.setBody(message.body());
        outbox.setStatus(EmailOutboxStatus.PENDING);
        outbox.setAttempts(0);
        outbox.setNextAttemptAt(now);
        outbox.setCreatedAt(now);
        outbox.setUpdatedAt(now);

        outboxRepository.save(outbox);

        return attemptSend(outbox);
    }

    @Scheduled(fixedDelayString = "${email.queue.fixed-delay-ms:30000}")
    @Transactional
    public void processQueue() {
        OffsetDateTime now = now();
        List<EmailOutbox> pending = outboxRepository
            .findByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(
                EmailOutboxStatus.PENDING,
                now,
                PageRequest.of(0, batchSize)
            )
            .getContent();

        for (EmailOutbox outbox : pending) {
            attemptSend(outbox);
        }
    }

    private EmailSendStatus attemptSend(EmailOutbox outbox) {
        if (outbox.getAttempts() >= maxAttempts) {
            return markFailed(outbox, "Maximo de tentativas atingido");
        }

        EmailMessage message = new EmailMessage(
            outbox.getToEmail(),
            outbox.getSubject(),
            outbox.getBody()
        );

        EmailSendResult result = emailProvider.send(message);
        return applyResult(outbox, result);
    }

    private EmailSendStatus applyResult(EmailOutbox outbox, EmailSendResult result) {
        OffsetDateTime now = now();
        int attempts = outbox.getAttempts() + 1;
        outbox.setAttempts(attempts);
        outbox.setUpdatedAt(now);

        if (result.status() == EmailSendStatus.SENT) {
            outbox.setStatus(EmailOutboxStatus.SENT);
            outbox.setProviderId(result.providerId());
            outbox.setLastError(null);
            outboxRepository.save(outbox);
            return EmailSendStatus.SENT;
        }

        if (result.status() == EmailSendStatus.QUOTA) {
            outbox.setStatus(EmailOutboxStatus.PENDING);
            outbox.setNextAttemptAt(now.plusMinutes(quotaDelayMinutes));
            outbox.setLastError(result.errorMessage());
            outboxRepository.save(outbox);
            return EmailSendStatus.QUOTA;
        }

        if (result.status() == EmailSendStatus.RETRY) {
            outbox.setStatus(EmailOutboxStatus.PENDING);
            outbox.setNextAttemptAt(now.plusSeconds(retryDelaySeconds * attempts));
            outbox.setLastError(result.errorMessage());
            outboxRepository.save(outbox);
            return EmailSendStatus.RETRY;
        }

        return markFailed(outbox, result.errorMessage());
    }

    private EmailSendStatus markFailed(EmailOutbox outbox, String error) {
        outbox.setStatus(EmailOutboxStatus.FAILED);
        outbox.setLastError(error);
        outbox.setUpdatedAt(now());
        outboxRepository.save(outbox);
        return EmailSendStatus.FAILED;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
