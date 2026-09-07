package com.northgate.platform.beacon.channel;

import org.springframework.stereotype.Component;

@Component
public class LetterChannelAdapter extends LoggingChannelAdapter {

    @Override
    public String channel() {
        return "LETTER";
    }
}
