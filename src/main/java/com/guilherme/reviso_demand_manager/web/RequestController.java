package com.guilherme.reviso_demand_manager.web;

import com.guilherme.reviso_demand_manager.application.RequestService;
import com.guilherme.reviso_demand_manager.application.RequestWorkflowService;
import com.guilherme.reviso_demand_manager.domain.RequestEvent;
import com.guilherme.reviso_demand_manager.domain.RequestPriority;
import com.guilherme.reviso_demand_manager.domain.RequestStatus;
import com.guilherme.reviso_demand_manager.domain.RequestType;
import com.guilherme.reviso_demand_manager.web.AssignRequestDTO;
import com.guilherme.reviso_demand_manager.web.ChangeStatusDTO;
import com.guilherme.reviso_demand_manager.web.CommentDTO;
import com.guilherme.reviso_demand_manager.web.RequestEventDTO;
import com.guilherme.reviso_demand_manager.web.RevisionDTO;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/requests")
public class RequestController {

    private final RequestService requestService;
    private final RequestWorkflowService requestWorkflowService;

    public RequestController(RequestService requestService, RequestWorkflowService requestWorkflowService) {
        this.requestService = requestService;
        this.requestWorkflowService = requestWorkflowService;
    }

    @PostMapping
    public ResponseEntity<RequestDTO> createRequest(@Valid @RequestBody CreateRequestDTO dto) {
        RequestDTO created = requestService.createRequest(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RequestDTO> getRequestById(@PathVariable UUID id) {
        RequestDTO request = requestService.getRequestById(id);
        return ResponseEntity.ok(request);
    }

    @GetMapping
    public ResponseEntity<Page<RequestDTO>> getAllRequests(
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) RequestPriority priority,
            @RequestParam(required = false) RequestType type,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) OffsetDateTime dueBefore,
            @RequestParam(required = false) OffsetDateTime createdFrom,
            @RequestParam(required = false) OffsetDateTime createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Page<RequestDTO> requests = requestService.getAllRequests(
                status, priority, type, clientId, 
                dueBefore, createdFrom, createdTo,
                page, size, sortBy, direction
        );
        return ResponseEntity.ok(requests);
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<RequestEventDTO> changeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeStatusDTO dto) {
        RequestEvent event = requestWorkflowService.changeStatus(id, dto.toStatus(), dto.message(), dto.actorId());
        return ResponseEntity.ok(toEventDTO(event));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<RequestEventDTO> addComment(@PathVariable UUID id, @Valid @RequestBody CommentDTO dto) {
        RequestEvent event = requestWorkflowService.addComment(id, dto.message(), dto.actorId());
        return ResponseEntity.ok(toEventDTO(event));
    }

    @PostMapping("/{id}/revisions")
    public ResponseEntity<RequestEventDTO> addRevision(@PathVariable UUID id, @RequestBody RevisionDTO dto) {
        RequestEvent event = requestWorkflowService.addRevision(id, dto.message(), dto.actorId());
        return ResponseEntity.ok(toEventDTO(event));
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<RequestEventDTO> assign(@PathVariable UUID id, @Valid @RequestBody AssignRequestDTO dto) {
        RequestEvent event = requestWorkflowService.assign(id, dto.assigneeId(), dto.actorId());
        return ResponseEntity.ok(toEventDTO(event));
    }

    @GetMapping("/{id}/events")
    public ResponseEntity<List<RequestEventDTO>> listEvents(@PathVariable UUID id) {
        List<RequestEventDTO> events = requestWorkflowService.listEvents(id).stream()
                .map(this::toEventDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(events);
    }

    private RequestEventDTO toEventDTO(RequestEvent event) {
        return new RequestEventDTO(
                event.getId(),
                event.getRequest().getId(),
                event.getActorId(),
                event.getEventType(),
                event.getFromStatus(),
                event.getToStatus(),
                event.getMessage(),
                event.getRevisionNumber(),
                event.getCreatedAt()
        );
    }
}
