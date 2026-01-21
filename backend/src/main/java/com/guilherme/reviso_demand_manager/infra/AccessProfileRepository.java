package com.guilherme.reviso_demand_manager.infra;

import com.guilherme.reviso_demand_manager.domain.AccessProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccessProfileRepository extends JpaRepository<AccessProfile, UUID> {
    Optional<AccessProfile> findByIdAndAgencyId(UUID id, UUID agencyId);
    List<AccessProfile> findByAgencyIdOrderByNameAsc(UUID agencyId);
}
