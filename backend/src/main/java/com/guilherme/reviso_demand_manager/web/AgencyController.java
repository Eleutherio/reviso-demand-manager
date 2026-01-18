package com.guilherme.reviso_demand_manager.web;

import com.guilherme.reviso_demand_manager.application.BriefingService;
import com.guilherme.reviso_demand_manager.application.CompanyService;
import com.guilherme.reviso_demand_manager.infra.JwtAuthFilter;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/agency")
public class AgencyController {

    private final BriefingService briefingService;
    private final CompanyService companyService;

    public AgencyController(BriefingService briefingService, CompanyService companyService) {
        this.briefingService = briefingService;
        this.companyService = companyService;
    }

    @GetMapping("/companies")
    public ResponseEntity<List<CompanyDTO>> listClientCompanies(Authentication authentication) {
        return ResponseEntity.ok(companyService.listClientCompanies(requireAgencyId(authentication)));
    }

    @GetMapping("/briefings")
    public ResponseEntity<List<BriefingDTO>> listBriefings(
            @RequestParam(required = false) String status,
            Authentication authentication) {
        return ResponseEntity.ok(briefingService.listBriefingsByStatus(status, requireAgencyId(authentication)));
    }

    @PostMapping("/briefings/{id}/convert")
    public ResponseEntity<RequestDTO> convertBriefingToRequest(
            @PathVariable UUID id,
            @Valid @RequestBody ConvertBriefingDTO dto,
            Authentication authentication) {
        RequestDTO request = briefingService.convertBriefingToRequest(
            id,
            dto.department(),
            requireAgencyId(authentication)
        );
        return ResponseEntity.ok(request);
    }

    @PatchMapping("/briefings/{id}/reject")
    public ResponseEntity<Void> rejectBriefing(@PathVariable UUID id, Authentication authentication) {
        briefingService.rejectBriefing(id, requireAgencyId(authentication));
        return ResponseEntity.noContent().build();
    }

    private UUID requireAgencyId(Authentication authentication) {
        JwtAuthFilter.AuthenticatedUser user = (JwtAuthFilter.AuthenticatedUser) authentication.getPrincipal();
        if (user == null || user.agencyId() == null) {
            throw new IllegalArgumentException("agencyId is required");
        }
        return user.agencyId();
    }
}
