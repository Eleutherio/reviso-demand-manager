package com.guilherme.reviso_demand_manager.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "access_profile_permissions")
@IdClass(AccessProfilePermissionId.class)
public class AccessProfilePermission {

    @Id
    @Column(name = "profile_id", columnDefinition = "UUID")
    private UUID profileId;

    @Id
    @Column(nullable = false, length = 120)
    private String permission;

    public UUID getProfileId() {
        return profileId;
    }

    public void setProfileId(UUID profileId) {
        this.profileId = profileId;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }
}
