package com.meridian.platform.entitlements.approval;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, String> {

    List<ApprovalRequest> findByOrganisationIdAndStatusOrderByRequestedAt(String organisationId, String status);

    List<ApprovalRequest> findByStatusAndExpiresAtBefore(String status, Instant cutoff);
}
