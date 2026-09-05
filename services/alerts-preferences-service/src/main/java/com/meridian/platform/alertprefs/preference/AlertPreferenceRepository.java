package com.meridian.platform.alertprefs.preference;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertPreferenceRepository extends JpaRepository<AlertPreference, Long> {

    List<AlertPreference> findByCustomerIdOrderByAlertCodeAsc(String customerId);

    Optional<AlertPreference> findByCustomerIdAndAlertCode(String customerId, String alertCode);

    long countByCustomerId(String customerId);
}
