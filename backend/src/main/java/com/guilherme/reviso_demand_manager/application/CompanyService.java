package com.guilherme.reviso_demand_manager.application;

import com.guilherme.reviso_demand_manager.domain.Company;
import com.guilherme.reviso_demand_manager.domain.CompanyType;
import com.guilherme.reviso_demand_manager.infra.CompanyRepository;
import com.guilherme.reviso_demand_manager.web.CompanyDTO;
import com.guilherme.reviso_demand_manager.web.CreateCompanyDTO;
import com.guilherme.reviso_demand_manager.web.ResourceNotFoundException;
import com.guilherme.reviso_demand_manager.web.UpdateCompanyDTO;
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
        company.setSegment(dto.segment());
        company.setContactEmail(dto.contactEmail());
        company.setSite(dto.site());
        company.setUsefulLinks(dto.usefulLinks());
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

    @Transactional(readOnly = true)
    public List<CompanyDTO> listClientCompanies() {
        return companyRepository.findByTypeOrderByNameAsc(CompanyType.CLIENT)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public CompanyDTO updateCompany(UUID companyId, UpdateCompanyDTO dto) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada"));

        if (dto.name() != null) {
            company.setName(blankToNull(dto.name()));
        }
        if (dto.type() != null) {
            company.setType(dto.type());
        }
        if (dto.active() != null) {
            company.setActive(dto.active());
        }

        if (dto.segment() != null) {
            company.setSegment(blankToNull(dto.segment()));
        }
        if (dto.contactEmail() != null) {
            company.setContactEmail(blankToNull(dto.contactEmail()));
        }
        if (dto.site() != null) {
            company.setSite(blankToNull(dto.site()));
        }
        if (dto.usefulLinks() != null) {
            company.setUsefulLinks(dto.usefulLinks());
        }

        Company saved = companyRepository.save(company);
        return toDTO(saved);
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private CompanyDTO toDTO(Company company) {
        return new CompanyDTO(
                company.getId(),
                company.getName(),
                company.getType(),
                company.getActive(),
                company.getSegment(),
                company.getContactEmail(),
                company.getSite(),
                company.getUsefulLinks(),
                company.getCreatedAt()
        );
    }
}
