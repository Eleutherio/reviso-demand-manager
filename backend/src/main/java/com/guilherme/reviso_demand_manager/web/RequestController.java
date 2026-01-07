package com.guilherme.reviso_demand_manager.web;

import com.guilherme.reviso_demand_manager.application.BriefingService;
import com.guilherme.reviso_demand_manager.application.RequestService;
import com.guilherme.reviso_demand_manager.application.RequestWorkflowService;
import com.guilherme.reviso_demand_manager.domain.RequestEvent;
import com.guilherme.reviso_demand_manager.domain.RequestPriority;
import com.guilherme.reviso_demand_manager.domain.RequestStatus;
import com.guilherme.reviso_demand_manager.domain.RequestType;
import com.guilherme.reviso_demand_manager.infra.JwtAuthFilter;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    private final BriefingService briefingService;

    public RequestController(RequestService requestService, 
                             RequestWorkflowService requestWorkflowService,
                             BriefingService briefingService) {
        this.requestService = requestService;
        this.requestWorkflowService = requestWorkflowService;
        this.briefingService = briefingService;
    }

    @PostMapping
    public ResponseEntity<RequestDTO> createRequest(@Valid @RequestBody CreateRequestDTO dto) {
        RequestDTO created = requestService.createRequest(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RequestDTO> getRequestById(
            @PathVariable UUID id,
            Authentication authentication) {
        
        // Se for CLIENT_USER, valida tenant isolation
        if (authentication != null && authentication.getPrincipal() instanceof JwtAuthFilter.AuthenticatedUser user) {
            if (user.companyId() != null) {
                // Usa método com tenant check
                return ResponseEntity.ok(briefingService.getRequestById(id, user.companyId()));
            }
        }
        
        // Para AGENCY_ADMIN/AGENCY_USER, retorna sem filtro
        RequestDTO request = requestService.getRequestById(id);
        return ResponseEntity.ok(request);
    }

    @GetMapping
    public ResponseEntity<Page<RequestDTO>> getAllRequests(
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) RequestPriority priority,
            @RequestParam(required = false) RequestType type,
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) OffsetDateTime dueBefore,
            @RequestParam(required = false) OffsetDateTime createdFrom,
            @RequestParam(required = false) OffsetDateTime createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Page<RequestDTO> requests = requestService.getAllRequests(
            status, priority, type, companyId,
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
        RequestEvent event = requestWorkflowService.addComment(id, dto.message(), dto.actorId(), dto.visibleToClient());
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
    public ResponseEntity<List<RequestEventDTO>> listEvents(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean onlyVisibleToClient,
            Authentication authentication) {
        
        // Se for CLIENT_USER, aplica tenant isolation e força visible_to_client=true
        if (authentication != null && authentication.getPrincipal() instanceof JwtAuthFilter.AuthenticatedUser user) {
            if (user.companyId() != null) {
                // Valida tenant antes de listar eventos
                briefingService.getRequestById(id, user.companyId());
                // Força visible_to_client=true para clientes
                onlyVisibleToClient = true;
            }
        }
        
        List<RequestEventDTO> events = requestWorkflowService.listEvents(id, onlyVisibleToClient).stream()
                .map(this::toEventDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(events);
    }

    // CLIENT_USER endpoints with tenant isolation
    @GetMapping("/mine")
    public ResponseEntity<List<RequestDTO>> getMyRequests(Authentication authentication) {
        JwtAuthFilter.AuthenticatedUser user = (JwtAuthFilter.AuthenticatedUser) authentication.getPrincipal();
        
        if (user.companyId() == null) {
            throw new IllegalArgumentException("CLIENT_USER deve ter companyId");
        }

        return ResponseEntity.ok(briefingService.listMyRequests(user.companyId()));
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
                event.getVisibleToClient(),
                event.getRevisionNumber(),
                event.getCreatedAt()
        );
    }
}
