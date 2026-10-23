package com.bautruc.ecommerce.reporting.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.auth.oauth2.GoogleCredentials;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class Ga4AnalyticsService {
    private static final Duration TTL = Duration.ofMinutes(10);
    private final RestClient client = RestClient.create();
    private final String propertyId;
    private Map<String, Object> cached;
    private Instant cachedAt;

    public Ga4AnalyticsService(@Value("${bautruc.analytics.ga4-property-id:}") String propertyId) {
        this.propertyId = propertyId;
    }

    public synchronized Map<String, Object> dashboard() {
        if (propertyId == null || !propertyId.matches("\\d+")) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "GA4 property is not configured");
        }
        if (cached != null && Instant.now().isBefore(cachedAt.plus(TTL))) return cached;
        try {
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                    .createScoped(List.of("https://www.googleapis.com/auth/analytics.readonly"));
            credentials.refreshIfExpired();
            String token = credentials.getAccessToken().getTokenValue();
            JsonNode summary = report(token, List.of("activeUsers", "sessions", "screenPageViews", "eventCount", "scrolledUsers", "userEngagementDuration", "bounceRate"), null);
            JsonNode daily = report(token, List.of("activeUsers", "sessions"), "date");
            JsonNode channels = report(token, List.of("sessions"), "sessionDefaultChannelGroup");
            Map<String, Object> result = parseReports(summary, daily, channels);
            cached = result;
            cachedAt = Instant.now();
            return result;
        } catch (Exception error) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Google Analytics Data API unavailable", error);
        }
    }

    static Map<String, Object> parseReports(JsonNode summary, JsonNode daily, JsonNode channels) {
        JsonNode totals = summary.path("rows").path(0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activeUsers", metric(totals, 0));
        result.put("sessions", metric(totals, 1));
        result.put("pageViews", metric(totals, 2));
        result.put("eventCount", metric(totals, 3));
        result.put("scrolledUsers", metric(totals, 4));
        result.put("engagementSeconds", metric(totals, 5));
        result.put("bounceRate", totals.path("metricValues").path(6).path("value").asDouble(0));
        List<Map<String, Object>> days = new ArrayList<>();
        for (JsonNode row : daily.path("rows")) {
            String date = row.path("dimensionValues").path(0).path("value").asText();
            if (date.length() == 8) date = date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6);
            days.add(Map.of("date", date, "activeUsers", metric(row, 0), "sessions", metric(row, 1)));
        }
        List<Map<String, Object>> sources = new ArrayList<>();
        for (JsonNode row : channels.path("rows")) {
            sources.add(Map.of("channel", row.path("dimensionValues").path(0).path("value").asText(), "sessions", metric(row, 0)));
        }
        result.put("daily", days);
        result.put("channels", sources);
        result.put("period", "30daysAgo - yesterday");
        return result;
    }

    private JsonNode report(String token, List<String> metrics, String dimension) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dateRanges", List.of(Map.of("startDate", "30daysAgo", "endDate", "yesterday")));
        body.put("metrics", metrics.stream().map(name -> Map.of("name", name)).toList());
        if (dimension != null) {
            body.put("dimensions", List.of(Map.of("name", dimension)));
            if ("date".equals(dimension)) body.put("orderBys", List.of(Map.of("dimension", Map.of("dimensionName", "date"))));
            else {
                body.put("orderBys", List.of(Map.of("metric", Map.of("metricName", "sessions"), "desc", true)));
                body.put("limit", "10");
            }
        }
        return client.post().uri("https://analyticsdata.googleapis.com/v1beta/properties/{id}:runReport", propertyId)
                .contentType(MediaType.APPLICATION_JSON).headers(headers -> headers.setBearerAuth(token))
                .body(body).retrieve().body(JsonNode.class);
    }

    private static long metric(JsonNode row, int index) {
        return row.path("metricValues").path(index).path("value").asLong(0);
    }
}
