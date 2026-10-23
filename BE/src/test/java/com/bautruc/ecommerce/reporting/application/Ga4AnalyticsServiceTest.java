package com.bautruc.ecommerce.reporting.application;

import static org.assertj.core.api.Assertions.assertThat;

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
}
