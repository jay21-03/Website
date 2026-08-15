package com.bautruc.ecommerce.common.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public interface BusinessClock {
    Instant now();

    ZoneId businessZone();

    default ZonedDateTime businessNow() {
        return now().atZone(businessZone());
    }

    default Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(businessZone()).toInstant();
    }

    default Instant startOfNextDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay(businessZone()).toInstant();
    }
}
