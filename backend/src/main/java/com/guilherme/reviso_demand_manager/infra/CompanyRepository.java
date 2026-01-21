package com.guilherme.reviso_demand_manager.infra;

import com.guilherme.reviso_demand_manager.domain.Company;
import com.guilherme.reviso_demand_manager.domain.CompanyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    List<Company> findByAgencyIdOrderByNameAsc(UUID agencyId);
    List<Company> findByAgencyIdAndTypeOrderByNameAsc(UUID agencyId, CompanyType type);
    Optional<Company> findByCompanyCodeIgnoreCaseAndAgencyId(String companyCode, UUID agencyId);
    Optional<Company> findByIdAndAgencyId(UUID id, UUID agencyId);
    List<Company> findByAgencyIdAndIdIn(UUID agencyId, Collection<UUID> ids);
    boolean existsByCompanyCodeAndAgencyId(String companyCode, UUID agencyId);
    long countByAgencyId(UUID agencyId);
}
