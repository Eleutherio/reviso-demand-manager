package com.guilherme.reviso_demand_manager.infra;

import com.guilherme.reviso_demand_manager.domain.RequestEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RequestEventRepository extends JpaRepository<RequestEvent, UUID> {
}
