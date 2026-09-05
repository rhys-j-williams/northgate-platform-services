package com.meridian.platform.beacon.admin;

import com.meridian.platform.beacon.api.NotificationsController;
import com.meridian.platform.beacon.channel.ChannelDispatcher;
import com.meridian.platform.beacon.notification.Notification;
import com.meridian.platform.beacon.notification.NotificationRepository;
import com.meridian.platform.beacon.preference.PreferencesClient;
import com.meridian.platform.beacon.sequence.SequenceCoordinator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Behind LDAP basic auth (AdminSecurityConfig). Group cn=beacon-ops. */
@RestController
@RequestMapping("/beacon/v1/admin")
public class AdminController {

    private final NotificationRepository repository;
    private final SequenceCoordinator coordinator;
    private final ChannelDispatcher dispatcher;
    private final PreferencesClient preferences;

    public AdminController(NotificationRepository repository, SequenceCoordinator coordinator,
                           ChannelDispatcher dispatcher, PreferencesClient preferences) {
        this.repository = repository;
        this.coordinator = coordinator;
        this.dispatcher = dispatcher;
        this.preferences = preferences;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("pending", repository.countByStatus(Notification.Status.PENDING));
        m.put("failed", repository.countByStatus(Notification.Status.FAILED));
        m.put("sent", repository.countByStatus(Notification.Status.SENT));
        m.put("parked", coordinator.parkedDepth());
        m.put("channels", dispatcher.channels());
        return m;
    }

    @GetMapping("/failed")
    public List<Map<String, Object>> failed() {
        return repository.findTop200ByStatusOrderByCreatedAtAsc(Notification.Status.FAILED)
            .stream().map(NotificationsController::view).collect(Collectors.toList());
    }

    @DeleteMapping("/preferences-cache/{customerId}")
    public Map<String, String> evict(@PathVariable String customerId) {
        preferences.evict(customerId);
        return java.util.Collections.singletonMap("evicted", customerId);
    }
}
