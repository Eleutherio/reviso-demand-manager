package com.guilherme.reviso_demand_manager.web;

import com.guilherme.reviso_demand_manager.infra.ReportRepository;
import com.guilherme.reviso_demand_manager.infra.JwtAuthFilter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportRepository reportRepository;

    public ReportController(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @GetMapping("/requests-by-status")
    public ResponseEntity<List<RequestsByStatusDTO>> requestsByStatus(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            Authentication authentication
    ) {
        List<RequestsByStatusDTO> result = reportRepository.requestsByStatus(from, to, requireAgencyId(authentication))
                .stream()
                .map(row -> new RequestsByStatusDTO(row.status(), row.total()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/overdue")
    public ResponseEntity<OverdueDTO> overdue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime at,
            Authentication authentication
    ) {
        OffsetDateTime checkTime = at != null ? at : OffsetDateTime.now();
        ReportRepository.OverdueRow row = reportRepository.overdue(checkTime, requireAgencyId(authentication));
        return ResponseEntity.ok(new OverdueDTO(row.total()));
    }

    @GetMapping("/avg-cycle-time")
    public ResponseEntity<CycleTimeDTO> avgCycleTime(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            Authentication authentication
    ) {
        ReportRepository.AvgCycleTimeRow row = reportRepository.avgCycleTime(from, to, requireAgencyId(authentication));
        Double avgSeconds = row.avgSeconds() != null ? row.avgSeconds() : 0.0;
        Long avgHours = (long) (avgSeconds / 3600);
        Long avgDays = avgHours / 24;
        return ResponseEntity.ok(new CycleTimeDTO(avgDays, avgHours % 24, avgSeconds));
    }

    @GetMapping("/rework-metrics")
    public ResponseEntity<ReworkMetricsDTO> reworkMetrics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            Authentication authentication
    ) {
        ReportRepository.ReworkMetricsRow row = reportRepository.reworkMetrics(from, to, requireAgencyId(authentication));
        Long reworkCount = row.reworkCount();
        Long totalCount = row.totalCount();
        Double percentage = totalCount > 0 ? (reworkCount.doubleValue() / totalCount) * 100 : 0.0;
        return ResponseEntity.ok(new ReworkMetricsDTO(reworkCount, totalCount, percentage));
    }

    private java.util.UUID requireAgencyId(Authentication authentication) {
        JwtAuthFilter.AuthenticatedUser user = (JwtAuthFilter.AuthenticatedUser) authentication.getPrincipal();
        if (user == null || user.agencyId() == null) {
            throw new IllegalArgumentException("agencyId is required");
        }
        return user.agencyId();
    }
}
