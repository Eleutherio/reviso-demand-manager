package com.guilherme.reviso_demand_manager.application;

import com.guilherme.reviso_demand_manager.domain.Company;
import com.guilherme.reviso_demand_manager.domain.User;
import com.guilherme.reviso_demand_manager.domain.UserRole;
import com.guilherme.reviso_demand_manager.infra.AccessProfileRepository;
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
    private final AccessProfileRepository accessProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
        UserRepository userRepository,
        CompanyRepository companyRepository,
        AccessProfileRepository accessProfileRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.accessProfileRepository = accessProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserDTO createUser(CreateUserDTO dto, UUID agencyId) {
        // Valida unicidade do email
        if (userRepository.findByEmail(dto.email()).isPresent()) {
            throw new IllegalArgumentException("Email já está em uso");
        }

        if (agencyId == null) {
            throw new IllegalArgumentException("agencyId is required");
        }

        Company company = resolveCompany(dto.role(), agencyId, dto.companyId(), dto.companyCode());
        UUID resolvedCompanyId = company != null ? company.getId() : null;
        UUID resolvedAgencyId = resolveUserAgencyId(dto.role(), agencyId, company);

        UUID resolvedAccessProfileId = null;
        if (dto.accessProfileId() != null) {
            resolvedAccessProfileId = resolveAccessProfileId(dto.role(), dto.accessProfileId(), agencyId);
        }
        // invariável: só AGENCY_USER pode ter accessProfileId
        if (dto.role() != UserRole.AGENCY_USER) {
            resolvedAccessProfileId = null;
        }

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFullName(dto.fullName());
        user.setEmail(dto.email());
        user.setPasswordHash(passwordEncoder.encode(dto.password()));
        user.setRole(dto.role());
        user.setAgencyId(resolvedAgencyId);
        user.setCompanyId(resolvedCompanyId);
        user.setAccessProfileId(resolvedAccessProfileId);
        user.setActive(true);
        user.setCreatedAt(OffsetDateTime.now());

        User saved = userRepository.save(user);
        return toDTO(saved, company != null ? company.getCompanyCode() : null);
    }

    @Transactional(readOnly = true)
    public List<UserDTO> listAllUsers(UUID agencyId) {
        if (agencyId == null) {
            throw new IllegalArgumentException("agencyId is required");
        }
        List<User> users = userRepository.findByAgencyId(agencyId);
        Map<UUID, String> companyCodes = resolveCompanyCodes(agencyId, users);
        return users.stream()
                .map(user -> toDTO(user, companyCodes.get(user.getCompanyId())))
                .toList();
    }

    @Transactional
    public UserDTO updateUser(UUID id, UpdateUserDTO dto, UUID agencyId) {
        if (agencyId == null) {
            throw new IllegalArgumentException("agencyId is required");
        }
        User user = userRepository.findByIdAndAgencyId(id, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));

        if (dto.email() != null && !user.getEmail().equals(dto.email())) {
            userRepository.findByEmail(dto.email())
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Email ja esta em uso");
                    });
        }

        // 1) aplica campos básicos
        if (dto.fullName() != null) {
            user.setFullName(dto.fullName());
        }
        if (dto.email() != null) {
            user.setEmail(dto.email());
        }
        if (dto.role() != null) {
            user.setRole(dto.role());
        }

        boolean roleChanged = dto.role() != null;
        boolean companyProvided = dto.companyId() != null || (dto.companyCode() != null && !dto.companyCode().isBlank());

        Company company = null;

        // 2) Company/Agency invariants
        if (user.getRole() == UserRole.CLIENT_USER) {
            // Para CLIENT_USER, aceita trocar/validar company se o payload trouxe info ou role mudou
            if (roleChanged || companyProvided) {
                company = resolveCompany(user.getRole(), agencyId, dto.companyId(), dto.companyCode());
                user.setCompanyId(company.getId());
            }
        } else {
            // Invariável: roles != CLIENT_USER não carregam company
            user.setCompanyId(null);
        }

        // agencyId pode depender da company quando CLIENT_USER
        // Se o role é CLIENT_USER, precisamos de company para validar agency.
        // Então, se role final é CLIENT_USER e não temos company resolvida aqui,
        // buscamos pela company atual do user (pra não exigir resend no PATCH).
        if (user.getRole() == UserRole.CLIENT_USER) {
            if (company == null) {
                UUID currentCompanyId = user.getCompanyId();
                if (currentCompanyId == null) {
                    throw new IllegalArgumentException("CLIENT_USER deve ter companyId ou companyCode");
                }
                company = findCompanyById(currentCompanyId, agencyId);
            }
            user.setAgencyId(resolveUserAgencyId(user.getRole(), agencyId, company));
        } else {
            user.setAgencyId(agencyId);
        }

        // 3) invariável: se role != AGENCY_USER, zera accessProfileId (sem zumbi)
        if (user.getRole() != UserRole.AGENCY_USER) {
            user.setAccessProfileId(null);
        } else {
            // PATCH-friendly: só muda se vier no DTO
            if (dto.accessProfileId() != null) {
                UUID resolvedAccessProfileId = resolveAccessProfileId(user.getRole(), dto.accessProfileId(), agencyId);
                user.setAccessProfileId(resolvedAccessProfileId);
            }
            // se dto.accessProfileId() == null: preserva o perfil atual
        }

        if (dto.active() != null) {
            user.setActive(dto.active());
        }

        User saved = userRepository.save(user);
        return toDTO(saved, company != null ? company.getCompanyCode() : null);
    }

    @Transactional
    public void deleteUser(UUID id, UUID agencyId) {
        if (agencyId == null) {
            throw new IllegalArgumentException("agencyId is required");
        }
        User user = userRepository.findByIdAndAgencyId(id, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));
        userRepository.delete(user);
    }

    private UserDTO toDTO(User user, String companyCode) {
        return new UserDTO(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getAgencyId(),
                user.getCompanyId(),
                user.getAccessProfileId(),
                companyCode,
                user.getActive(),
                user.getCreatedAt()
        );
    }

    private UserDTO toDTO(User user) {
        return toDTO(user, resolveCompanyCode(user.getAgencyId(), user.getCompanyId()));
    }

    private Company resolveCompany(UserRole role, UUID agencyId, UUID companyId, String companyCode) {
        String normalizedCode = normalizeCompanyCode(companyCode);
        boolean hasCode = normalizedCode != null && !normalizedCode.isBlank();

        Company company = null;
        if (companyId != null && hasCode) {
            company = findCompanyByCode(normalizedCode, agencyId);
            if (!company.getId().equals(companyId)) {
                throw new IllegalArgumentException("companyId e companyCode nao correspondem");
            }
        } else if (companyId != null) {
            company = findCompanyById(companyId, agencyId);
        } else if (hasCode) {
            company = findCompanyByCode(normalizedCode, agencyId);
        }

        if (role == UserRole.CLIENT_USER) {
            if (company == null) {
                throw new IllegalArgumentException("CLIENT_USER deve ter companyId ou companyCode");
            }
            return company;
        }

        return null;
    }

    private UUID resolveUserAgencyId(UserRole role, UUID agencyId, Company company) {
        if (agencyId == null) {
            throw new IllegalArgumentException("agencyId is required");
        }
        if (role == UserRole.CLIENT_USER) {
            UUID companyAgencyId = company != null ? company.getAgencyId() : null;
            if (companyAgencyId == null) {
                throw new IllegalArgumentException("Empresa sem agencia vinculada");
            }
            if (!companyAgencyId.equals(agencyId)) {
                throw new IllegalArgumentException("Empresa nao pertence a agencia atual");
            }
            return companyAgencyId;
        }
        return agencyId;
    }

    private UUID resolveAccessProfileId(UserRole role, UUID accessProfileId, UUID agencyId) {
        if (accessProfileId == null) {
            return null;
        }
        if (role != UserRole.AGENCY_USER) {
            throw new IllegalArgumentException("accessProfileId apenas para AGENCY_USER");
        }
        if (agencyId == null) {
            throw new IllegalArgumentException("agencyId is required");
        }
        accessProfileRepository.findByIdAndAgencyId(accessProfileId, agencyId)
            .orElseThrow(() -> new IllegalArgumentException("Perfil de acesso invalido"));
        return accessProfileId;
    }

    private Company findCompanyById(UUID companyId, UUID agencyId) {
        return companyRepository.findByIdAndAgencyId(companyId, agencyId)
            .orElseThrow(() -> new IllegalArgumentException("Empresa nao encontrada"));
    }

    private Company findCompanyByCode(String companyCode, UUID agencyId) {
        return companyRepository.findByCompanyCodeIgnoreCaseAndAgencyId(companyCode, agencyId)
            .orElseThrow(() -> new IllegalArgumentException("Codigo da empresa invalido"));
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

    private String resolveCompanyCode(UUID agencyId, UUID companyId) {
        if (agencyId == null || companyId == null) {
            return null;
        }
        return companyRepository.findByIdAndAgencyId(companyId, agencyId)
            .map(Company::getCompanyCode)
            .orElse(null);
    }

    private Map<UUID, String> resolveCompanyCodes(UUID agencyId, List<User> users) {
        Map<UUID, String> codes = new HashMap<>();
        if (agencyId == null) {
            return codes;
        }
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
        for (Company company : companyRepository.findByAgencyIdAndIdIn(agencyId, ids)) {
            codes.put(company.getId(), company.getCompanyCode());
        }
        return codes;
    }
}
