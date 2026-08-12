package com.bautruc.ecommerce.common.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class SystemBusinessClockTest {

    @Test
    void usesAsiaHoChiMinhBusinessZone() {
        SystemBusinessClock businessClock = new SystemBusinessClock(Clock.fixed(
                Instant.parse("2026-08-11T17:30:00Z"),
                ZoneOffset.UTC
        ));

        assertThat(businessClock.now()).isEqualTo(Instant.parse("2026-08-11T17:30:00Z"));
        assertThat(businessClock.businessZone().getId()).isEqualTo("Asia/Ho_Chi_Minh");
        assertThat(businessClock.businessNow().toLocalDate()).isEqualTo(LocalDate.of(2026, 8, 12));
        assertThat(businessClock.businessNow().getOffset().getId()).isEqualTo("+07:00");
    }

    @Test
    void convertsBusinessDateRangeToUtcInstants() {
        SystemBusinessClock businessClock = new SystemBusinessClock(Clock.fixed(
                Instant.parse("2026-08-12T00:00:00Z"),
                ZoneOffset.UTC
        ));

        assertThat(businessClock.startOfDay(LocalDate.of(2026, 8, 12)))
                .isEqualTo(Instant.parse("2026-08-11T17:00:00Z"));
        assertThat(businessClock.startOfNextDay(LocalDate.of(2026, 8, 12)))
                .isEqualTo(Instant.parse("2026-08-12T17:00:00Z"));
    }
}
