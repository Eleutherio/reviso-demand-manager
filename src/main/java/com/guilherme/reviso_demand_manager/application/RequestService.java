package com.guilherme.reviso_demand_manager.application;

import com.guilherme.reviso_demand_manager.domain.Request;
import com.guilherme.reviso_demand_manager.domain.RequestStatus;
import com.guilherme.reviso_demand_manager.infra.RequestRepository;
import com.guilherme.reviso_demand_manager.web.CreateRequestDTO;
import com.guilherme.reviso_demand_manager.web.RequestDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RequestService {

    private final RequestRepository requestRepository;

    public RequestService(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    @Transactional
    public RequestDTO createRequest(CreateRequestDTO dto) {
        Request request = new Request();
        request.setId(UUID.randomUUID());
        request.setClientId(dto.clientId());
        request.setTitle(dto.title());
        request.setDescription(dto.description());
        request.setType(dto.type());
        request.setPriority(dto.priority());
        request.setStatus(RequestStatus.NEW);
        request.setDueDate(dto.dueDate());
        request.setRevisionCount(0);
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());

        Request saved = requestRepository.save(request);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public RequestDTO getRequestById(UUID id) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        return toDTO(request);
    }

    private RequestDTO toDTO(Request request) {
        return new RequestDTO(
                request.getId(),
                request.getClientId(),
                request.getTitle(),
                request.getDescription(),
                request.getType(),
                request.getPriority(),
                request.getStatus(),
                request.getDueDate(),
                request.getRevisionCount(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}
