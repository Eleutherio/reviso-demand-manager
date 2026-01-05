package com.guilherme.reviso_demand_manager.web;

import com.guilherme.reviso_demand_manager.application.RequestService;
import com.guilherme.reviso_demand_manager.domain.RequestPriority;
import com.guilherme.reviso_demand_manager.domain.RequestStatus;
import com.guilherme.reviso_demand_manager.domain.RequestType;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/requests")
public class RequestController {

    private final RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
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
}
