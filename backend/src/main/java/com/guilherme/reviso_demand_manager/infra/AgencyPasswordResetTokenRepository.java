package com.guilherme.reviso_demand_manager.infra;

import com.guilherme.reviso_demand_manager.domain.AgencyPasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface AgencyPasswordResetTokenRepository extends JpaRepository<AgencyPasswordResetToken, UUID> {
    Optional<AgencyPasswordResetToken> findTopByEmailAndTokenHashAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
        String email,
        String tokenHash,
        OffsetDateTime now
    );

    @Modifying
    @Query("update AgencyPasswordResetToken t set t.usedAt = :now where t.email = :email and t.usedAt is null")
    int markUsedByEmail(@Param("email") String email, @Param("now") OffsetDateTime now);
}
