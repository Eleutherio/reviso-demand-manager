package com.guilherme.reviso_demand_manager.web;

import com.guilherme.reviso_demand_manager.application.TenantProvisioningService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/tenants")
public class AdminTenantController {

    private final TenantProvisioningService tenantProvisioningService;

    public AdminTenantController(TenantProvisioningService tenantProvisioningService) {
        this.tenantProvisioningService = tenantProvisioningService;
    }

    @PostMapping("/{agencyId}/provision")
    @PreAuthorize("hasRole('AGENCY_ADMIN')")
    public ResponseEntity<Map<String, String>> provisionTenant(@PathVariable UUID agencyId) {
        tenantProvisioningService.provisionTenant(agencyId);
        return ResponseEntity.ok(Map.of("message", "Tenant provisioned successfully"));
    }
}
