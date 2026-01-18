package com.guilherme.reviso_demand_manager.infra;

import com.guilherme.reviso_demand_manager.domain.Briefing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BriefingRepository extends JpaRepository<Briefing, UUID> {
    List<Briefing> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);
    List<Briefing> findByStatusOrderByCreatedAtDesc(String status);
    List<Briefing> findByCompanyIdAndAgencyIdOrderByCreatedAtDesc(UUID companyId, UUID agencyId);
    List<Briefing> findByAgencyIdOrderByCreatedAtDesc(UUID agencyId);
    List<Briefing> findByAgencyIdAndStatusOrderByCreatedAtDesc(UUID agencyId, String status);
    Optional<Briefing> findByIdAndAgencyId(UUID id, UUID agencyId);
}
