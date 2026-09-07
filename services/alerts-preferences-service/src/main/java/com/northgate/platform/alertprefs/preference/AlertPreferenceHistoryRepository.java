package com.northgate.platform.alertprefs.preference;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertPreferenceHistoryRepository extends JpaRepository<AlertPreferenceHistory, Long> {

    List<AlertPreferenceHistory> findByCustomerIdOrderByChangedAtDesc(String customerId);
}
