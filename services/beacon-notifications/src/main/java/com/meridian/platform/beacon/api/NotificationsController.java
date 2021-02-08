package com.meridian.platform.beacon.api;

import com.meridian.platform.beacon.notification.Notification;
import com.meridian.platform.beacon.notification.NotificationRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/beacon/v1")
public class NotificationsController {

    private final NotificationRepository repository;

    public NotificationsController(NotificationRepository repository) {
        this.repository = repository;
    }

    /** Newest first, capped at 100. The retail-web inbox pages client side, which is a known issue (MOL-3312). */
    @GetMapping("/customers/{customerId}/notifications")
    public List<Map<String, Object>> forCustomer(@PathVariable String customerId) {
        return repository.findTop100ByCustomerIdOrderByCustomerSequenceDescNotificationIdDesc(customerId)
            .stream().map(NotificationsController::view).collect(Collectors.toList());
    }

    public static Map<String, Object> view(Notification n) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("notificationId", n.getNotificationId());
        m.put("eventId", n.getEventId());
        m.put("customerId", n.getCustomerId());
        m.put("accountId", n.getAccountId());
        m.put("sequence", n.getCustomerSequence());
        m.put("eventType", n.getEventType());
        m.put("templateCode", n.getTemplateCode());
        m.put("channel", n.getChannel());
        m.put("regulatory", n.isRegulatory());
        m.put("subject", n.getRenderedSubject());
        m.put("body", n.getRenderedBody());
        m.put("status", n.getStatus());
        m.put("occurredAt", n.getOccurredAt());
        m.put("dispatchedAt", n.getDispatchedAt());
        return m;
    }
}
