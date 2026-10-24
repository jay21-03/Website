package com.bautruc.ecommerce.reporting.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class Ga4AnalyticsServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void mapsMetricsDatesAndChannels() throws Exception {
        var summary = mapper.readTree("""
                {"rows":[{"metricValues":[{"value":"2"},{"value":"3"},{"value":"5"},{"value":"8"},{"value":"1"},{"value":"120"},{"value":"0.25"}]}]}
                """);
        var daily = mapper.readTree("""
                {"rows":[{"dimensionValues":[{"value":"20261022"}],"metricValues":[{"value":"2"},{"value":"3"}]}]}
                """);
        var channels = mapper.readTree("""
                {"rows":[{"dimensionValues":[{"value":"Organic Search"}],"metricValues":[{"value":"3"}]}]}
                """);
        var result = Ga4AnalyticsService.parseReports(summary, daily, channels);
        assertThat(result).containsEntry("scrolledUsers", 1L).containsEntry("engagementSeconds", 120L).containsEntry("bounceRate", 0.25);
        assertThat(result.get("daily").toString()).contains("2026-10-22");
        assertThat(result.get("channels").toString()).contains("Organic Search");
    }

    @Test
    void mapsRealtimeWithoutSummingMinuteUniqueUsers() throws Exception {
        var summary = mapper.readTree("""
                {"rows":[{"metricValues":[{"value":"1"},{"value":"4"},{"value":"7"},{"value":"0"}]}]}
                """);
        var minutes = mapper.readTree("""
                {"rows":[{"dimensionValues":[{"value":"0"}],"metricValues":[{"value":"1"}]},
                         {"dimensionValues":[{"value":"29"}],"metricValues":[{"value":"1"}]}]}
                """);
        var pages = mapper.readTree("""
                {"rows":[{"dimensionValues":[{"value":"Trang chủ"}],"metricValues":[{"value":"4"}]}]}
                """);
        var empty = mapper.readTree("{}");
        var result = Ga4AnalyticsService.parseRealtime(summary, minutes, pages, empty, empty, empty);
        assertThat(result).containsEntry("status", "LIVE");
        assertThat((java.util.Map<String, Object>) result.get("metrics")).containsEntry("activeUsers", 1L).containsEntry("pageViews", 4L);
        assertThat((java.util.List<?>) result.get("trend")).hasSize(30);
        assertThat((java.util.Map<String, Object>) ((java.util.List<?>) result.get("trend")).get(0)).containsEntry("minute", 29L);
        assertThat(result.get("pages").toString()).contains("Trang chủ");
    }

    @Test
    void distinguishesAnEmptyRealtimeWindowFromAnApiFailure() throws Exception {
        var empty = mapper.readTree("{}");
        var result = Ga4AnalyticsService.parseRealtime(empty, empty, empty, empty, empty, empty);
        assertThat(result).containsEntry("status", "EMPTY");
        assertThat(result.get("events")).isEqualTo(java.util.List.of());
    }

    @Test
    void historicalRangeIsWhitelistedAndEmptyIsExplicit() throws Exception {
        var empty = mapper.readTree("{}");
        var result = Ga4AnalyticsService.parseReports(empty, empty, empty, 7);
        assertThat(result).containsEntry("status", "EMPTY").containsEntry("period", "7daysAgo - yesterday");
        assertThatThrownBy(() -> new Ga4AnalyticsService("560090227").historical(8))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void comparisonDoesNotInventGrowthWhenPreviousIsZero() throws Exception {
        var current = mapper.readTree("""
                {"rows":[{"metricValues":[{"value":"3"},{"value":"6"},{"value":"9"}]}]}
                """);
        var previous = mapper.readTree("""
                {"rows":[{"metricValues":[{"value":"0"},{"value":"3"},{"value":"3"}]}]}
                """);
        var result = Ga4AnalyticsService.comparison(current, previous);
        assertThat(result).containsEntry("status", "AVAILABLE");
        assertThat(((java.util.Map<?, ?>) result.get("activeUsers")).get("growthPercent")).isNull();
        assertThat(((java.util.Map<?, ?>) result.get("sessions")).get("growthPercent")).isEqualTo(100.0);
        assertThat(Ga4AnalyticsService.comparison(current, mapper.readTree("{}"))).containsEntry("status", "UNAVAILABLE");
    }
}
