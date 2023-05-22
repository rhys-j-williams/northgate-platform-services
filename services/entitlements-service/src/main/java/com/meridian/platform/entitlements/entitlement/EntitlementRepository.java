package com.meridian.platform.entitlements.entitlement;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntitlementRepository extends JpaRepository<Entitlement, String> {

    List<Entitlement> findByOrganisationIdOrderByUserHandle(String organisationId);

    List<Entitlement> findByUserHandleAndStatus(String userHandle, String status);

    Optional<Entitlement> findByOrganisationIdAndUserHandleAndRoleCodeAndStatus(
        String organisationId, String userHandle, String roleCode, String status);
}
