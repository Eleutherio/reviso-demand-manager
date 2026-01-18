package com.guilherme.reviso_demand_manager.infra;

import com.guilherme.reviso_demand_manager.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    List<User> findByAgencyId(UUID agencyId);
    Optional<User> findByIdAndAgencyId(UUID id, UUID agencyId);
}
