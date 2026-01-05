package com.guilherme.reviso_demand_manager.application;

import com.guilherme.reviso_demand_manager.domain.Request;
import com.guilherme.reviso_demand_manager.domain.RequestEvent;
import com.guilherme.reviso_demand_manager.domain.RequestEventType;
import com.guilherme.reviso_demand_manager.domain.RequestStatus;
import com.guilherme.reviso_demand_manager.infra.RequestEventRepository;
import com.guilherme.reviso_demand_manager.infra.RequestRepository;
import com.guilherme.reviso_demand_manager.web.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class RequestWorkflowService {

    private final RequestRepository requestRepository;
    private final RequestEventRepository requestEventRepository;

    public RequestWorkflowService(RequestRepository requestRepository, RequestEventRepository requestEventRepository) {
        this.requestRepository = requestRepository;
        this.requestEventRepository = requestEventRepository;
    }

    @Transactional
    public RequestEvent appendEvent(UUID requestId,
                                    RequestEventType eventType,
                                    RequestStatus toStatus,
                                    String message,
                                    UUID actorId) {
        if (eventType == null) {
            throw new IllegalArgumentException("eventType is required");
        }

        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        RequestStatus fromStatus = request.getStatus();

        if (eventType == RequestEventType.STATUS_CHANGED) {
            if (toStatus == null) {
                throw new IllegalArgumentException("toStatus is required for STATUS_CHANGED event");
            }

            request.setStatus(toStatus); // validates allowed transitions
            if (!toStatus.equals(fromStatus)) {
                int current = request.getRevisionCount() != null ? request.getRevisionCount() : 0;
                request.setRevisionCount(current + 1);
            }
        }

        requestRepository.save(request);

        RequestEvent event = new RequestEvent();
        event.setId(UUID.randomUUID());
        event.setRequest(request);
        event.setActorId(actorId);
        event.setEventType(eventType);
        event.setFromStatus(fromStatus);
        event.setToStatus(toStatus);
        event.setMessage(message);
        event.setRevisionNumber(request.getRevisionCount());
        event.setCreatedAt(OffsetDateTime.now());

        return requestEventRepository.save(event);
    }
}
