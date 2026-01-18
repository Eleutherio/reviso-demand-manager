package com.guilherme.reviso_demand_manager.infra;

import com.guilherme.reviso_demand_manager.domain.Company;
import com.guilherme.reviso_demand_manager.domain.CompanyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    List<Company> findByTypeOrderByNameAsc(CompanyType type);
    List<Company> findByAgencyIdOrderByNameAsc(UUID agencyId);
    List<Company> findByAgencyIdAndTypeOrderByNameAsc(UUID agencyId, CompanyType type);
    Optional<Company> findByCompanyCodeIgnoreCase(String companyCode);
    Optional<Company> findByCompanyCodeIgnoreCaseAndActiveTrue(String companyCode);
    Optional<Company> findByCompanyCodeIgnoreCaseAndAgencyId(String companyCode, UUID agencyId);
    Optional<Company> findByIdAndAgencyId(UUID id, UUID agencyId);
    boolean existsByCompanyCode(String companyCode);
}
