package com.guilherme.reviso_demand_manager.application;

import com.guilherme.reviso_demand_manager.domain.Company;
import com.guilherme.reviso_demand_manager.domain.User;
import com.guilherme.reviso_demand_manager.domain.UserRole;
import com.guilherme.reviso_demand_manager.infra.CompanyRepository;
import com.guilherme.reviso_demand_manager.infra.UserRepository;
import com.guilherme.reviso_demand_manager.web.CreateUserDTO;
import com.guilherme.reviso_demand_manager.web.ResourceNotFoundException;
import com.guilherme.reviso_demand_manager.web.UpdateUserDTO;
import com.guilherme.reviso_demand_manager.web.UserDTO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
        UserRepository userRepository,
        CompanyRepository companyRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserDTO createUser(CreateUserDTO dto) {
        // Validate email uniqueness
        if (userRepository.findByEmail(dto.email()).isPresent()) {
            throw new IllegalArgumentException("Email já está em uso");
        }

        UUID resolvedCompanyId = resolveCompanyId(dto.role(), dto.companyId(), dto.companyCode());

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFullName(dto.fullName());
        user.setEmail(dto.email());
        user.setPasswordHash(passwordEncoder.encode(dto.password()));
        user.setRole(dto.role());
        user.setCompanyId(resolvedCompanyId);
        user.setActive(true);
        user.setCreatedAt(OffsetDateTime.now());

        User saved = userRepository.save(user);
        return toDTO(saved, resolveCompanyCode(resolvedCompanyId));
    }

    @Transactional(readOnly = true)
    public List<UserDTO> listAllUsers() {
        List<User> users = userRepository.findAll();
        Map<UUID, String> companyCodes = resolveCompanyCodes(users);
        return users.stream()
                .map(user -> toDTO(user, companyCodes.get(user.getCompanyId())))
                .toList();
    }

    @Transactional
    public UserDTO updateUser(UUID id, UpdateUserDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));

        if (!user.getEmail().equals(dto.email())) {
            userRepository.findByEmail(dto.email())
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Email ja esta em uso");
                    });
        }

        UUID resolvedCompanyId = resolveCompanyId(dto.role(), dto.companyId(), dto.companyCode());

        user.setFullName(dto.fullName());
        user.setEmail(dto.email());
        user.setRole(dto.role());
        user.setCompanyId(resolvedCompanyId);
        if (dto.active() != null) {
            user.setActive(dto.active());
        }

        User saved = userRepository.save(user);
        return toDTO(saved, resolveCompanyCode(resolvedCompanyId));
    }

    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));
        userRepository.delete(user);
    }

    private UserDTO toDTO(User user, String companyCode) {
        return new UserDTO(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getCompanyId(),
                companyCode,
                user.getActive(),
                user.getCreatedAt()
        );
    }

    private UserDTO toDTO(User user) {
        return toDTO(user, resolveCompanyCode(user.getCompanyId()));
    }

    private UUID resolveCompanyId(UserRole role, UUID companyId, String companyCode) {
        String normalizedCode = normalizeCompanyCode(companyCode);
        boolean hasCode = normalizedCode != null && !normalizedCode.isBlank();

        if (companyId != null && hasCode) {
            Company company = companyRepository.findByCompanyCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new IllegalArgumentException("Codigo da empresa invalido"));
            if (!company.getId().equals(companyId)) {
                throw new IllegalArgumentException("companyId e companyCode nao correspondem");
            }
            return companyId;
        }

        if (companyId != null) {
            return companyId;
        }

        if (hasCode) {
            Company company = companyRepository.findByCompanyCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new IllegalArgumentException("Codigo da empresa invalido"));
            return company.getId();
        }

        if (role == UserRole.CLIENT_USER) {
            throw new IllegalArgumentException("CLIENT_USER deve ter companyId ou companyCode");
        }

        return null;
    }

    private String normalizeCompanyCode(String rawCode) {
        if (rawCode == null) {
            return null;
        }
        String trimmed = rawCode.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    private String resolveCompanyCode(UUID companyId) {
        if (companyId == null) {
            return null;
        }
        return companyRepository.findById(companyId)
            .map(Company::getCompanyCode)
            .orElse(null);
    }

    private Map<UUID, String> resolveCompanyCodes(List<User> users) {
        Map<UUID, String> codes = new HashMap<>();
        Set<UUID> ids = new HashSet<>();
        for (User user : users) {
            UUID companyId = user.getCompanyId();
            if (companyId != null) {
                ids.add(companyId);
            }
        }
        if (ids.isEmpty()) {
            return codes;
        }
        for (Company company : companyRepository.findAllById(ids)) {
            codes.put(company.getId(), company.getCompanyCode());
        }
        return codes;
    }
}
