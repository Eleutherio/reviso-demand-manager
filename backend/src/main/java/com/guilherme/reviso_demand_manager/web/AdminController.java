package com.guilherme.reviso_demand_manager.web;

import com.guilherme.reviso_demand_manager.application.AgencyService;
import com.guilherme.reviso_demand_manager.application.CompanyService;
import com.guilherme.reviso_demand_manager.application.UserService;
import com.guilherme.reviso_demand_manager.infra.JwtAuthFilter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AgencyService agencyService;
    private final CompanyService companyService;
    private final UserService userService;

    public AdminController(AgencyService agencyService, CompanyService companyService, UserService userService) {
        this.agencyService = agencyService;
        this.companyService = companyService;
        this.userService = userService;
    }

    @PostMapping("/companies")
    public ResponseEntity<CompanyDTO> createCompany(
            @Valid @RequestBody CreateCompanyDTO dto,
            Authentication authentication) {
        CompanyDTO created = companyService.createCompany(dto, requireAgencyId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/agencies")
    public ResponseEntity<AgencyDTO> createAgency(
            @Valid @RequestBody CreateAgencyDTO dto,
            Authentication authentication) {
        JwtAuthFilter.AuthenticatedUser user = (JwtAuthFilter.AuthenticatedUser) authentication.getPrincipal();
        AgencyDTO created = agencyService.createAgency(dto, user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/agencies/me")
    public ResponseEntity<AgencyDTO> getMyAgency(Authentication authentication) {
        JwtAuthFilter.AuthenticatedUser user = (JwtAuthFilter.AuthenticatedUser) authentication.getPrincipal();
        if (user == null || user.agencyId() == null) {
            throw new IllegalArgumentException("agencyId is required");
        }
        return ResponseEntity.ok(agencyService.getAgency(user.agencyId()));
    }

    @GetMapping("/companies")
    public ResponseEntity<List<CompanyDTO>> listCompanies(Authentication authentication) {
        return ResponseEntity.ok(companyService.listAllCompanies(requireAgencyId(authentication)));
    }

    @PatchMapping("/companies/{id}")
    public ResponseEntity<CompanyDTO> updateCompany(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCompanyDTO dto,
            Authentication authentication) {
        CompanyDTO updated = companyService.updateCompany(id, dto, requireAgencyId(authentication));
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/users")
    public ResponseEntity<UserDTO> createUser(
            @Valid @RequestBody CreateUserDTO dto,
            Authentication authentication) {
        UserDTO created = userService.createUser(dto, requireAgencyId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> listUsers(Authentication authentication) {
        return ResponseEntity.ok(userService.listAllUsers(requireAgencyId(authentication)));
    }

    @PatchMapping("/users/{id}")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserDTO dto,
            Authentication authentication) {
        UserDTO updated = userService.updateUser(id, dto, requireAgencyId(authentication));
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id, Authentication authentication) {
        userService.deleteUser(id, requireAgencyId(authentication));
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
