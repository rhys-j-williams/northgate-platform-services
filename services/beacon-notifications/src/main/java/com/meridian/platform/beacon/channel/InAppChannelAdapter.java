package com.meridian.platform.beacon.channel;

import org.springframework.stereotype.Component;

@Component
public class InAppChannelAdapter extends LoggingChannelAdapter {

    @Override
    public String channel() {
        return "IN-APP";
    }
}
