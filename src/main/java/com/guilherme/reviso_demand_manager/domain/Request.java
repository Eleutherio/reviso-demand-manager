package com.guilherme.reviso_demand_manager.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "requests")
public class Request {

        private static final Map<RequestStatus, Set<RequestStatus>> ALLOWED_TRANSITIONS = Map.of(
            RequestStatus.NEW, Set.of(RequestStatus.IN_PROGRESS, RequestStatus.CANCELED),
            RequestStatus.IN_PROGRESS, Set.of(RequestStatus.IN_REVIEW, RequestStatus.CANCELED),
            RequestStatus.IN_REVIEW, Set.of(RequestStatus.APPROVED, RequestStatus.CHANGES_REQUESTED, RequestStatus.CANCELED),
            RequestStatus.CHANGES_REQUESTED, Set.of(RequestStatus.IN_PROGRESS, RequestStatus.CANCELED),
            RequestStatus.APPROVED, Set.of(RequestStatus.DELIVERED, RequestStatus.CANCELED),
            RequestStatus.DELIVERED, Set.of(RequestStatus.DONE, RequestStatus.CLOSED)
        );

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;
    
    @Column(name = "client_id", nullable = false, columnDefinition = "UUID")
    private UUID clientId;

    @Column(name = "company_id", columnDefinition = "UUID")
    private UUID companyId;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "type", columnDefinition = "request_type")
    private RequestType type;
    
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "priority", columnDefinition = "request_priority")
    private RequestPriority priority;
    
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "request_status")
    private RequestStatus status;

    @Column(name = "assignee_id", columnDefinition = "UUID")
    private UUID assigneeId;
    
    @Column(name = "due_date")
    private OffsetDateTime dueDate;
    
    @Column(name = "revision_count", nullable = false)
    private Integer revisionCount;
    
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Request() {
    }

    public Request(UUID id, UUID clientId, UUID companyId, String title, String description,
                   RequestType type, RequestPriority priority, RequestStatus status,
                   OffsetDateTime dueDate, Integer revisionCount,
                   OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.clientId = clientId;
        this.companyId = companyId;
        this.title = title;
        this.description = description;
        this.type = type;
        this.priority = priority;
        this.status = status;
        this.dueDate = dueDate;
        this.revisionCount = revisionCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getClientId() {
        return clientId;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RequestType getType() {
        return type;
    }

    public void setType(RequestType type) {
        this.type = type;
    }

    public RequestPriority getPriority() {
        return priority;
    }

    public void setPriority(RequestPriority priority) {
        this.priority = priority;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        if (this.status == null) {
            this.status = status;
            return;
        }

        if (this.status == status) {
            return; // idempotent
        }

        Set<RequestStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(this.status, Set.of());
        if (!allowed.contains(status)) {
            throw new IllegalStateException("Transition from " + this.status + " to " + status + " is not allowed");
        }

        this.status = status;
    }

    public UUID getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(UUID assigneeId) {
        this.assigneeId = assigneeId;
    }

    public OffsetDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(OffsetDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public Integer getRevisionCount() {
        return revisionCount;
    }

    public void setRevisionCount(Integer revisionCount) {
        this.revisionCount = revisionCount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
