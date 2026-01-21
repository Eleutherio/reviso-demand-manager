package com.guilherme.reviso_demand_manager.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class AccessProfilePermissionId implements Serializable {

    private UUID profileId;
    private String permission;

    public AccessProfilePermissionId() {
    }

    public AccessProfilePermissionId(UUID profileId, String permission) {
        this.profileId = profileId;
        this.permission = permission;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccessProfilePermissionId that = (AccessProfilePermissionId) o;
        return Objects.equals(profileId, that.profileId)
            && Objects.equals(permission, that.permission);
    }

    @Override
    public int hashCode() {
        return Objects.hash(profileId, permission);
    }
}
