package com.guilherme.reviso_demand_manager.web;

import com.guilherme.reviso_demand_manager.application.BriefingService;
import com.guilherme.reviso_demand_manager.infra.JwtAuthFilter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/briefings")
public class BriefingController {

    private final BriefingService briefingService;

    public BriefingController(BriefingService briefingService) {
        this.briefingService = briefingService;
    }

    @PostMapping
    public ResponseEntity<BriefingDTO> createBriefing(
            @Valid @RequestBody CreateBriefingDTO dto,
            Authentication authentication) {
        
        JwtAuthFilter.AuthenticatedUser user = (JwtAuthFilter.AuthenticatedUser) authentication.getPrincipal();
        
        if (user.companyId() == null) {
            throw new IllegalArgumentException("CLIENT_USER deve ter companyId");
        }

        BriefingDTO created = briefingService.createBriefing(dto, user.companyId(), user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/mine")
    public ResponseEntity<List<BriefingDTO>> getMyBriefings(Authentication authentication) {
        JwtAuthFilter.AuthenticatedUser user = (JwtAuthFilter.AuthenticatedUser) authentication.getPrincipal();
        
        if (user.companyId() == null) {
            throw new IllegalArgumentException("CLIENT_USER deve ter companyId");
        }

        return ResponseEntity.ok(briefingService.listMyBriefings(user.companyId()));
    }
}
