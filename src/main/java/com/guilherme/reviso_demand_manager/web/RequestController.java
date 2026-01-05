package com.guilherme.reviso_demand_manager.web;

import com.guilherme.reviso_demand_manager.application.RequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
