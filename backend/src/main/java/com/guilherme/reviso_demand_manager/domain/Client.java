package com.guilherme.reviso_demand_manager.domain;

import java.time.OffsetDateTime;
import java.util.UUID;
public class Client {

    private UUID id;
    
    private String name;
    
    private String segment;
    
    private Boolean active;
    
    private OffsetDateTime createdAt;

    public Client() {
    }

    public Client(UUID id, String name, String segment, Boolean active, OffsetDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.segment = segment;
        this.active = active;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSegment() {
        return segment;
    }

    public void setSegment(String segment) {
        this.segment = segment;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
