package com.guilherme.reviso_demand_manager.application;

import com.guilherme.reviso_demand_manager.domain.Briefing;
import com.guilherme.reviso_demand_manager.domain.Request;
import com.guilherme.reviso_demand_manager.domain.RequestStatus;
import com.guilherme.reviso_demand_manager.infra.BriefingRepository;
import com.guilherme.reviso_demand_manager.infra.RequestRepository;
import com.guilherme.reviso_demand_manager.web.BriefingDTO;
import com.guilherme.reviso_demand_manager.web.CreateBriefingDTO;
import com.guilherme.reviso_demand_manager.web.RequestDTO;
import com.guilherme.reviso_demand_manager.web.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BriefingService {

    private final BriefingRepository briefingRepository;
    private final RequestRepository requestRepository;

    public BriefingService(BriefingRepository briefingRepository, RequestRepository requestRepository) {
        this.briefingRepository = briefingRepository;
        this.requestRepository = requestRepository;
    }

    @Transactional
    public BriefingDTO createBriefing(CreateBriefingDTO dto, UUID companyId, UUID userId) {
        Briefing briefing = new Briefing();
        briefing.setId(UUID.randomUUID());
        briefing.setCompanyId(companyId);
        briefing.setCreatedByUserId(userId);
        briefing.setTitle(dto.title());
        briefing.setDescription(dto.description());
        briefing.setStatus("PENDING");
        briefing.setCreatedAt(OffsetDateTime.now());

        Briefing saved = briefingRepository.save(briefing);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<BriefingDTO> listMyBriefings(UUID companyId) {
        return briefingRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BriefingDTO> listBriefingsByStatus(String status) {
        if (status != null && !status.isBlank()) {
            return briefingRepository.findByStatusOrderByCreatedAtDesc(status).stream()
                    .map(this::toDTO)
                    .toList();
        }
        return briefingRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RequestDTO> listMyRequests(UUID companyId) {
        return requestRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .map(this::toRequestDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public RequestDTO getRequestById(UUID requestId, UUID companyId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Demanda não encontrada"));

        // Tenant isolation check
        if (!request.getCompanyId().equals(companyId)) {
            throw new ResourceNotFoundException("Demanda não encontrada");
        }

        return toRequestDTO(request);
    }

    @Transactional
    public RequestDTO convertBriefingToRequest(UUID briefingId) {
        Briefing briefing = briefingRepository.findById(briefingId)
                .orElseThrow(() -> new ResourceNotFoundException("Briefing não encontrado"));

        if (!"PENDING".equals(briefing.getStatus())) {
            throw new IllegalStateException("Apenas briefings PENDING podem ser convertidos");
        }

        // Create request from briefing
        Request request = new Request();
        request.setId(UUID.randomUUID());
        request.setCompanyId(briefing.getCompanyId());
        request.setClientId(briefing.getCompanyId()); // Use company as client for now
        request.setTitle(briefing.getTitle());
        request.setDescription(briefing.getDescription());
        request.setStatus(RequestStatus.NEW);
        request.setRevisionCount(0);
        request.setCreatedAt(OffsetDateTime.now());
        request.setUpdatedAt(OffsetDateTime.now());

        Request saved = requestRepository.save(request);

        // Update briefing status
        briefing.setStatus("CONVERTED");
        briefingRepository.save(briefing);

        return toRequestDTO(saved);
    }

    @Transactional
    public void rejectBriefing(UUID briefingId) {
        Briefing briefing = briefingRepository.findById(briefingId)
                .orElseThrow(() -> new ResourceNotFoundException("Briefing não encontrado"));

        if (!"PENDING".equals(briefing.getStatus())) {
            throw new IllegalStateException("Apenas briefings PENDING podem ser rejeitados");
        }

        briefing.setStatus("REJECTED");
        briefingRepository.save(briefing);
    }

    private BriefingDTO toDTO(Briefing briefing) {
        return new BriefingDTO(
                briefing.getId(),
                briefing.getCompanyId(),
                briefing.getCreatedByUserId(),
                briefing.getTitle(),
                briefing.getDescription(),
                briefing.getStatus(),
                briefing.getCreatedAt()
        );
    }

    private RequestDTO toRequestDTO(Request request) {
        return new RequestDTO(
                request.getId(),
                request.getClientId(),
                request.getCompanyId(),
                request.getTitle(),
                request.getDescription(),
                request.getType(),
                request.getPriority(),
                request.getStatus(),
                request.getAssigneeId(),
                request.getDueDate(),
                request.getRevisionCount(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}
