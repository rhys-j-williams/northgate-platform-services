package com.meridian.platform.entitlements.role;

import com.meridian.platform.entitlements.infra.ApiException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Cached view of ROLE_DEF. Roles change a few times a year via Flyway, never at runtime, so a
 * refresh on demand is enough. Permissions are strings of the form area:verb.
 */
@Component
public class RoleCatalogue {

    public static final String MANAGE_ENTITLEMENTS = "entitlements:manage";
    public static final String APPROVE_PAYMENTS = "payments:approve";
    public static final String INITIATE_PAYMENTS = "payments:initiate";

    private final RoleDefRepository repository;
    private final Map<String, RoleDef> cache = new ConcurrentHashMap<>();

    public RoleCatalogue(RoleDefRepository repository) {
        this.repository = repository;
    }

    public RoleDef require(String roleCode) {
        RoleDef def = cache.computeIfAbsent(roleCode, c -> repository.findById(c).orElse(null));
        if (def == null) {
            cache.remove(roleCode);
            throw ApiException.badRequest("ROLE_UNKNOWN", "no such role: " + roleCode);
        }
        return def;
    }

    public Set<String> permissionsOf(String roleCode) {
        return require(roleCode).permissionSet();
    }

    public boolean isSensitive(String roleCode) {
        return require(roleCode).isSensitive();
    }

    public List<RoleDef> all() {
        List<RoleDef> all = repository.findAll();
        all.forEach(r -> cache.put(r.getRoleCode(), r));
        return all;
    }

    public void evict() {
        cache.clear();
    }
}
