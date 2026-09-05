package com.meridian.platform.beacon.channel;

import org.springframework.stereotype.Component;

@Component
public class EmailChannelAdapter extends LoggingChannelAdapter {

    @Override
    public String channel() {
        return "EMAIL";
    }
}
