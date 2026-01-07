package com.guilherme.reviso_demand_manager.infra;

import com.guilherme.reviso_demand_manager.domain.Company;
import com.guilherme.reviso_demand_manager.domain.CompanyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
	List<Company> findByTypeOrderByNameAsc(CompanyType type);
}
