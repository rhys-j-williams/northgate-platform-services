package com.meridian.platform.entitlements.role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "ROLE_DEF")
public class RoleDef {

    @Id
    @Column(name = "ROLE_CODE")
    private String roleCode;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "PERMISSIONS")
    private String permissions;

    @Column(name = "SENSITIVE")
    private boolean sensitive;

    public String getRoleCode() {
        return roleCode;
    }

    public String getDescription() {
        return description;
    }

    public Set<String> permissionSet() {
        return new LinkedHashSet<>(Arrays.asList(permissions.split(",")));
    }

    public boolean isSensitive() {
        return sensitive;
    }
}
