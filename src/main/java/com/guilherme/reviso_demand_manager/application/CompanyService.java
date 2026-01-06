package com.guilherme.reviso_demand_manager.application;

import com.guilherme.reviso_demand_manager.domain.Company;
import com.guilherme.reviso_demand_manager.infra.CompanyRepository;
import com.guilherme.reviso_demand_manager.web.CompanyDTO;
import com.guilherme.reviso_demand_manager.web.CreateCompanyDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Transactional
    public CompanyDTO createCompany(CreateCompanyDTO dto) {
        Company company = new Company();
        company.setId(UUID.randomUUID());
        company.setName(dto.name());
        company.setType(dto.type());
        company.setActive(true);
        company.setCreatedAt(OffsetDateTime.now());

        Company saved = companyRepository.save(company);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<CompanyDTO> listAllCompanies() {
        return companyRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    private CompanyDTO toDTO(Company company) {
        return new CompanyDTO(
                company.getId(),
                company.getName(),
                company.getType(),
                company.getActive(),
                company.getCreatedAt()
        );
    }
}
