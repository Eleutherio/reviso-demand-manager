package com.guilherme.reviso_demand_manager.infra.spec;

import com.guilherme.reviso_demand_manager.domain.Request;
import com.guilherme.reviso_demand_manager.domain.RequestPriority;
import com.guilherme.reviso_demand_manager.domain.RequestStatus;
import com.guilherme.reviso_demand_manager.domain.RequestType;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class RequestSpecifications {

    private RequestSpecifications() {}

    public static Specification<Request> companyId(UUID companyId) {
        return (root, query, cb) -> companyId == null ? cb.conjunction() : cb.equal(root.get("companyId"), companyId);
    }

    public static Specification<Request> status(RequestStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<Request> type(RequestType type) {
        return (root, query, cb) -> type == null ? cb.conjunction() : cb.equal(root.get("type"), type);
    }

    public static Specification<Request> priority(RequestPriority priority) {
        return (root, query, cb) -> priority == null ? cb.conjunction() : cb.equal(root.get("priority"), priority);
    }

    public static Specification<Request> dueBefore(OffsetDateTime dueBefore) {
        return (root, query, cb) -> dueBefore == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("dueDate"), dueBefore);
    }

    public static Specification<Request> createdFrom(OffsetDateTime createdFrom) {
        return (root, query, cb) -> createdFrom == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom);
    }

    public static Specification<Request> createdTo(OffsetDateTime createdTo) {
        return (root, query, cb) -> createdTo == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("createdAt"), createdTo);
    }

    public static Specification<Request> build(
            UUID companyId,
            RequestStatus status,
            RequestType type,
            RequestPriority priority,
            OffsetDateTime dueBefore,
            OffsetDateTime createdFrom,
            OffsetDateTime createdTo
    ) {
        return Specification
            .where(companyId(companyId))
                .and(status(status))
                .and(type(type))
                .and(priority(priority))
                .and(dueBefore(dueBefore))
                .and(createdFrom(createdFrom))
                .and(createdTo(createdTo));
    }
}
