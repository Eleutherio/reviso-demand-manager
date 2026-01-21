package com.guilherme.reviso_demand_manager.application;

import com.guilherme.reviso_demand_manager.domain.Company;
import com.guilherme.reviso_demand_manager.domain.CompanyType;
import com.guilherme.reviso_demand_manager.domain.Subscription;
import com.guilherme.reviso_demand_manager.domain.SubscriptionPlan;
import com.guilherme.reviso_demand_manager.infra.CompanyRepository;
import com.guilherme.reviso_demand_manager.infra.SubscriptionPlanRepository;
import com.guilherme.reviso_demand_manager.infra.SubscriptionRepository;
import com.guilherme.reviso_demand_manager.web.CompanyDTO;
import com.guilherme.reviso_demand_manager.web.CreateCompanyDTO;
import com.guilherme.reviso_demand_manager.web.ResourceNotFoundException;
import com.guilherme.reviso_demand_manager.web.UpdateCompanyDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    public CompanyService(
            CompanyRepository companyRepository,
            SubscriptionRepository subscriptionRepository,
            SubscriptionPlanRepository subscriptionPlanRepository
    ) {
        this.companyRepository = companyRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
    }

    @Transactional
    public CompanyDTO createCompany(CreateCompanyDTO dto, UUID agencyId) {
        if (agencyId == null) {
            throw new IllegalArgumentException("agencyId is required");
        }
        enforceCompanyLimit(agencyId);
        Company company = new Company();
        company.setId(UUID.randomUUID());
        company.setAgencyId(agencyId);
        company.setCompanyCode(generateCompanyCode(dto.name(), dto.type(), dto.segment(), agencyId));
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
    public List<CompanyDTO> listAllCompanies(UUID agencyId) {
        if (agencyId == null) {
            throw new IllegalArgumentException("agencyId is required");
        }
        return companyRepository.findByAgencyIdOrderByNameAsc(agencyId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CompanyDTO> listClientCompanies(UUID agencyId) {
        if (agencyId == null) {
            throw new IllegalArgumentException("agencyId is required");
        }
        return companyRepository.findByAgencyIdAndTypeOrderByNameAsc(agencyId, CompanyType.CLIENT)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public CompanyDTO updateCompany(UUID companyId, UpdateCompanyDTO dto, UUID agencyId) {
        if (agencyId == null) {
            throw new IllegalArgumentException("agencyId is required");
        }
        Company company = companyRepository.findByIdAndAgencyId(companyId, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa nao encontrada"));

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
                company.getAgencyId(),
                company.getCompanyCode(),
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

    private String generateCompanyCode(String name, CompanyType type, String segment, UUID agencyId) {
        String base = buildBaseCode(name, type, segment);
        String candidate = base;
        int suffix = 1;

        while (companyRepository.existsByCompanyCodeAndAgencyId(candidate, agencyId)) {
            candidate = base + "-" + String.format("%02d", suffix);
            suffix++;
        }

        return candidate;
    }

    private String buildBaseCode(String name, CompanyType type, String segment) {
        String namePart = normalizePart(name, 4);
        String typePart = type == CompanyType.CLIENT ? "CL" : "AG";
        String segmentPart = normalizePart(segment, 3);
        return namePart + "-" + typePart + "-" + segmentPart;
    }

    private String normalizePart(String value, int length) {
        String normalized = normalize(value);
        if (normalized.length() >= length) {
            return normalized.substring(0, length);
        }

        StringBuilder padded = new StringBuilder(normalized);
        while (padded.length() < length) {
            padded.append('X');
        }
        return padded.toString();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        String withoutMarks = decomposed.replaceAll("\\p{M}", "");
        return withoutMarks.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private void enforceCompanyLimit(UUID agencyId) {
        Integer maxCompanies = resolveMaxCompanies(agencyId);
        if (maxCompanies == null || maxCompanies <= 0) {
            return;
        }
        long current = companyRepository.countByAgencyId(agencyId);
        if (current >= maxCompanies) {
            throw new IllegalStateException("Limite do plano atingido");
        }
    }

    private Integer resolveMaxCompanies(UUID agencyId) {
        Subscription subscription = subscriptionRepository.findByAgencyId(agencyId).orElse(null);
        if (subscription == null) {
            return null;
        }
        SubscriptionPlan plan = subscriptionPlanRepository.findById(subscription.getPlanId()).orElse(null);
        if (plan == null) {
            return null;
        }
        if (plan.getMaxCompanies() != null) {
            return plan.getMaxCompanies();
        }
        return plan.getMaxUsers();
    }
}
