package com.guilherme.reviso_demand_manager.application;

import com.guilherme.reviso_demand_manager.domain.Request;
import com.guilherme.reviso_demand_manager.domain.RequestStatus;
import com.guilherme.reviso_demand_manager.domain.RequestType;
import com.guilherme.reviso_demand_manager.domain.RequestPriority;
import com.guilherme.reviso_demand_manager.domain.Company;
import com.guilherme.reviso_demand_manager.infra.CompanyRepository;
import com.guilherme.reviso_demand_manager.infra.RequestRepository;
import com.guilherme.reviso_demand_manager.infra.spec.RequestSpecifications;
import com.guilherme.reviso_demand_manager.web.CreateRequestDTO;
import com.guilherme.reviso_demand_manager.web.RequestDTO;
import com.guilherme.reviso_demand_manager.web.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class RequestService {

    private final RequestRepository requestRepository;
    private final CompanyRepository companyRepository;

    public RequestService(RequestRepository requestRepository, CompanyRepository companyRepository) {
        this.requestRepository = requestRepository;
        this.companyRepository = companyRepository;
    }

    @Transactional
    public RequestDTO createRequest(CreateRequestDTO dto) {
        Request request = new Request();
        request.setId(UUID.randomUUID());
        request.setCompanyId(dto.companyId());
        request.setBriefingId(dto.briefingId());
        request.setTitle(dto.title());
        request.setDescription(dto.description());
        request.setType(dto.type() != null ? dto.type() : RequestType.OTHER);
        request.setPriority(dto.priority() != null ? dto.priority() : RequestPriority.MEDIUM);
        request.setDepartment(dto.department());
        request.setStatus(RequestStatus.NEW);
        request.setDueDate(dto.dueDate());
        request.setRevisionCount(0);
        request.setCreatedAt(OffsetDateTime.now());
        request.setUpdatedAt(OffsetDateTime.now());

        Request saved = requestRepository.save(request);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public RequestDTO getRequestById(UUID id) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        return toDTO(request);
    }

    @Transactional(readOnly = true)
    public Page<RequestDTO> getAllRequests(
            RequestStatus status,
            RequestPriority priority,
            RequestType type,
            UUID companyId,
            OffsetDateTime dueBefore,
            OffsetDateTime createdFrom,
            OffsetDateTime createdTo,
            int page,
            int size,
            String sortBy,
            String direction) {

        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Specification<Request> spec = RequestSpecifications.build(
            companyId, status, type, priority, dueBefore, createdFrom, createdTo
        );

        return requestRepository.findAll(spec, pageable).map(this::toDTO);
    }

    private RequestDTO toDTO(Request request) {
        return new RequestDTO(
                request.getId(),
                request.getCompanyId(),
                resolveCompanyName(request.getCompanyId()),
                request.getBriefingId(),
                request.getTitle(),
                request.getDescription(),
                request.getType(),
                request.getPriority(),
                request.getDepartment(),
                request.getStatus(),
            request.getAssigneeId(),
                request.getDueDate(),
                request.getRevisionCount(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }

    private String resolveCompanyName(UUID companyId) {
        if (companyId == null) return null;
        return companyRepository.findById(companyId)
                .map(Company::getName)
                .orElse(null);
    }
}
