package com.guilherme.reviso_demand_manager.infra;

import com.guilherme.reviso_demand_manager.domain.PendingSignup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PendingSignupRepository extends JpaRepository<PendingSignup, UUID> {
    Optional<PendingSignup> findByCheckoutSessionId(String checkoutSessionId);
}
