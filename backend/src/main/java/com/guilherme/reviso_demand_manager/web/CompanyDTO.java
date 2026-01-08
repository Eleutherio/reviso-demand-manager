package com.guilherme.reviso_demand_manager.web;

import com.guilherme.reviso_demand_manager.domain.CompanyType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CompanyDTO(
    UUID id,
    String companyCode,
    String name,
    CompanyType type,
    Boolean active,
    String segment,
    String contactEmail,
    String site,
    List<String> usefulLinks,
    OffsetDateTime createdAt
) {
}
