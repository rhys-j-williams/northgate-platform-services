package com.meridian.platform.beacon.notification;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByEventId(String eventId);

    List<Notification> findTop100ByCustomerIdOrderByCustomerSequenceDescNotificationIdDesc(String customerId);

    List<Notification> findTop200ByStatusOrderByCreatedAtAsc(Notification.Status status);

    long countByStatus(Notification.Status status);
}
