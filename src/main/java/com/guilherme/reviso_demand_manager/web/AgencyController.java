package com.guilherme.reviso_demand_manager.web;

import com.guilherme.reviso_demand_manager.application.BriefingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/agency")
public class AgencyController {

    private final BriefingService briefingService;

    public AgencyController(BriefingService briefingService) {
        this.briefingService = briefingService;
    }

    @GetMapping("/briefings")
    public ResponseEntity<List<BriefingDTO>> listBriefings(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(briefingService.listBriefingsByStatus(status));
    }

    @PostMapping("/briefings/{id}/convert")
    public ResponseEntity<RequestDTO> convertBriefingToRequest(@PathVariable UUID id) {
        RequestDTO request = briefingService.convertBriefingToRequest(id);
        return ResponseEntity.ok(request);
    }

    @PatchMapping("/briefings/{id}/reject")
    public ResponseEntity<Void> rejectBriefing(@PathVariable UUID id) {
        briefingService.rejectBriefing(id);
        return ResponseEntity.noContent().build();
    }
}
