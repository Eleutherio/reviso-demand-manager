package com.guilherme.reviso_demand_manager.infra;

import com.guilherme.reviso_demand_manager.domain.EmailOutbox;
import com.guilherme.reviso_demand_manager.domain.EmailOutboxStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, UUID> {
    Page<EmailOutbox> findByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(
        EmailOutboxStatus status,
        OffsetDateTime nextAttemptAt,
        Pageable pageable
    );
}
