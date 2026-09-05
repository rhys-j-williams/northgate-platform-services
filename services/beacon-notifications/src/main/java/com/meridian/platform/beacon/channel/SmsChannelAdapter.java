package com.meridian.platform.beacon.channel;

import org.springframework.stereotype.Component;

@Component
public class SmsChannelAdapter extends LoggingChannelAdapter {

    @Override
    public String channel() {
        return "SMS";
    }
}
// TODO(PLAT-1655): SMS aggregator rate limits at 30/s per short code. The real adapter needs the
// token bucket that was in the 4.x codebase and did not survive the rewrite.
