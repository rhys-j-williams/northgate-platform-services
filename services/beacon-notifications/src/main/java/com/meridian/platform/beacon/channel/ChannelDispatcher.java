package com.meridian.platform.beacon.channel;

import com.meridian.platform.beacon.notification.Notification;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;

@Component
public class ChannelDispatcher {

    private final Map<String, ChannelAdapter> adapters = new TreeMap<>();

    public ChannelDispatcher(List<ChannelAdapter> adapters) {
        for (ChannelAdapter a : adapters) {
            this.adapters.put(a.channel(), a);
        }
    }

    public void dispatch(String channel, Notification notification) {
        ChannelAdapter adapter = adapters.get(channel.toUpperCase());
        if (adapter == null) {
            throw new IllegalStateException("no adapter for channel " + channel);
        }
        adapter.send(notification);
    }

    public Iterable<String> channels() {
        return adapters.keySet();
    }
}
