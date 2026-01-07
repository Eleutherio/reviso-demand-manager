package com.guilherme.reviso_demand_manager.web;

import com.guilherme.reviso_demand_manager.application.BriefingService;
import com.guilherme.reviso_demand_manager.application.CompanyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<CompanyDTO>> listClientCompanies() {
        return ResponseEntity.ok(companyService.listClientCompanies());
    }

    @GetMapping("/briefings")
    public ResponseEntity<List<BriefingDTO>> listBriefings(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(briefingService.listBriefingsByStatus(status));
    }

    @PostMapping("/briefings/{id}/convert")
    public ResponseEntity<RequestDTO> convertBriefingToRequest(
            @PathVariable UUID id,
            @Valid @RequestBody ConvertBriefingDTO dto) {
        RequestDTO request = briefingService.convertBriefingToRequest(id, dto.department());
        return ResponseEntity.ok(request);
    }

    @PatchMapping("/briefings/{id}/reject")
    public ResponseEntity<Void> rejectBriefing(@PathVariable UUID id) {
        briefingService.rejectBriefing(id);
        return ResponseEntity.noContent().build();
    }
}
