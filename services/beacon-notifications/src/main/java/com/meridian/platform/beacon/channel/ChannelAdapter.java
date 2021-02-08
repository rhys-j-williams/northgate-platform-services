package com.meridian.platform.beacon.channel;

import com.meridian.platform.beacon.notification.Notification;

public interface ChannelAdapter {

    String channel();

    /** Throws on failure; the caller records the attempt. */
    void send(Notification notification);
}
