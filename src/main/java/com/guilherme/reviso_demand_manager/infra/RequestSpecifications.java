package com.guilherme.reviso_demand_manager.infra;

import com.guilherme.reviso_demand_manager.domain.Request;
import com.guilherme.reviso_demand_manager.domain.RequestPriority;
import com.guilherme.reviso_demand_manager.domain.RequestStatus;
import com.guilherme.reviso_demand_manager.domain.RequestType;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.UUID;

public class RequestSpecifications {

    public static Specification<Request> hasStatus(RequestStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Request> hasPriority(RequestPriority priority) {
        return (root, query, cb) -> priority == null ? null : cb.equal(root.get("priority"), priority);
    }

    public static Specification<Request> hasType(RequestType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("type"), type);
    }

    public static Specification<Request> hasClientId(UUID clientId) {
        return (root, query, cb) -> clientId == null ? null : cb.equal(root.get("clientId"), clientId);
    }

    public static Specification<Request> dueBefore(OffsetDateTime dueBefore) {
        return (root, query, cb) -> dueBefore == null ? null : cb.lessThanOrEqualTo(root.get("dueDate"), dueBefore);
    }

    public static Specification<Request> createdFrom(OffsetDateTime createdFrom) {
        return (root, query, cb) -> createdFrom == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom);
    }

    public static Specification<Request> createdTo(OffsetDateTime createdTo) {
        return (root, query, cb) -> createdTo == null ? null : cb.lessThanOrEqualTo(root.get("createdAt"), createdTo);
    }
}
