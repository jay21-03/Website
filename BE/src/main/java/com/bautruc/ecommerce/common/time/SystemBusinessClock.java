package com.bautruc.ecommerce.common.time;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

@Component
public class SystemBusinessClock implements BusinessClock {
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final Clock clock;

    public SystemBusinessClock() {
        this(Clock.systemUTC());
    }

    public SystemBusinessClock(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Instant now() {
        return clock.instant();
    }

    @Override
    public ZoneId businessZone() {
        return BUSINESS_ZONE;
    }
}
