package com.guilherme.reviso_demand_manager.infra;

import com.guilherme.reviso_demand_manager.domain.RequestEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RequestEventRepository extends JpaRepository<RequestEvent, UUID> {
	List<RequestEvent> findByRequestIdOrderByCreatedAtDesc(UUID requestId);
	List<RequestEvent> findByRequestIdAndVisibleToClientTrueOrderByCreatedAtDesc(UUID requestId);
}
