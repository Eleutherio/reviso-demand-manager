package com.guilherme.reviso_demand_manager.web;

import com.guilherme.reviso_demand_manager.application.CompanyService;
import com.guilherme.reviso_demand_manager.application.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final CompanyService companyService;
    private final UserService userService;

    public AdminController(CompanyService companyService, UserService userService) {
        this.companyService = companyService;
        this.userService = userService;
    }

    @PostMapping("/companies")
    public ResponseEntity<CompanyDTO> createCompany(@Valid @RequestBody CreateCompanyDTO dto) {
        CompanyDTO created = companyService.createCompany(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/companies")
    public ResponseEntity<List<CompanyDTO>> listCompanies() {
        return ResponseEntity.ok(companyService.listAllCompanies());
    }

    @PatchMapping("/companies/{id}")
    public ResponseEntity<CompanyDTO> updateCompany(@PathVariable UUID id, @Valid @RequestBody UpdateCompanyDTO dto) {
        CompanyDTO updated = companyService.updateCompany(id, dto);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/users")
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody CreateUserDTO dto) {
        UserDTO created = userService.createUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> listUsers() {
        return ResponseEntity.ok(userService.listAllUsers());
    }
}
