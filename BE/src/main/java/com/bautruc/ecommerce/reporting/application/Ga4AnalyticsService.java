package com.bautruc.ecommerce.reporting.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.auth.oauth2.GoogleCredentials;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class Ga4AnalyticsService {
    private static final Duration TTL = Duration.ofMinutes(10);
    private final RestClient client = RestClient.create();
    private final String propertyId;
    private final Object historicalLock = new Object();
    private final Object realtimeLock = new Object();
    private final Map<Integer, Map<String, Object>> historicalCache = new HashMap<>();
    private final Map<Integer, Instant> historicalCachedAt = new HashMap<>();
    private Map<String, Object> realtimeCached;
    private Instant realtimeCachedAt;

    public Ga4AnalyticsService(@Value("${bautruc.analytics.ga4-property-id:}") String propertyId) {
        this.propertyId = propertyId;
    }

    public Map<String, Object> dashboard() {
        return historical(30);
    }

    public Map<String, Object> historical(int days) {
        synchronized (historicalLock) { return historicalLocked(days); }
    }

    private Map<String, Object> historicalLocked(int days) {
        if (days != 7 && days != 30 && days != 90) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "days must be 7, 30 or 90");
        }
        if (propertyId == null || !propertyId.matches("\\d+")) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "GA4 property is not configured");
        }
        if (historicalCache.containsKey(days) && Instant.now().isBefore(historicalCachedAt.get(days).plus(TTL))) return historicalCache.get(days);
        try {
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                    .createScoped(List.of("https://www.googleapis.com/auth/analytics.readonly"));
            credentials.refreshIfExpired();
            String token = credentials.getAccessToken().getTokenValue();
            JsonNode summary = report(token, List.of("activeUsers", "sessions", "screenPageViews", "eventCount", "scrolledUsers", "userEngagementDuration", "bounceRate"), null, days);
            JsonNode previous = report(token, List.of("activeUsers", "sessions", "screenPageViews"), null,
                    (days * 2) + "daysAgo", (days + 1) + "daysAgo");
            JsonNode daily = report(token, List.of("activeUsers", "sessions"), "date", days);
            JsonNode channels = report(token, List.of("sessions"), "sessionDefaultChannelGroup", days);
            Map<String, Object> result = parseReports(summary, daily, channels, days);
            result.put("comparison", comparison(summary, previous));
            if (!"EMPTY".equals(result.get("status"))) {
                result.put("pageTitles", breakdown(report(token, List.of("screenPageViews"), "pageTitle", days), "title", "pageViews"));
                result.put("pagePaths", breakdown(report(token, List.of("screenPageViews"), "pagePath", days), "path", "pageViews"));
                result.put("devices", breakdown(report(token, List.of("activeUsers"), "deviceCategory", days), "name", "activeUsers"));
                result.put("browsers", breakdown(report(token, List.of("activeUsers"), "browser", days), "name", "activeUsers"));
                result.put("operatingSystems", breakdown(report(token, List.of("activeUsers"), "operatingSystem", days), "name", "activeUsers"));
                result.put("countries", breakdown(report(token, List.of("activeUsers"), "country", days), "name", "activeUsers"));
                result.put("events", breakdown(report(token, List.of("eventCount"), "eventName", days), "name", "eventCount"));
                result.put("sourceMedium", breakdown(report(token, List.of("sessions"), "sessionSourceMedium", days), "name", "sessions"));
            } else {
                for (String key : List.of("pageTitles", "pagePaths", "devices", "browsers", "operatingSystems", "countries", "events", "sourceMedium")) {
                    result.put(key, List.of());
                }
            }
            historicalCache.put(days, result);
            historicalCachedAt.put(days, Instant.now());
            return result;
        } catch (Exception error) {
            throw ga4Failure(error);
        }
    }

    public Map<String, Object> realtime() {
        synchronized (realtimeLock) { return realtimeLocked(); }
    }

    private Map<String, Object> realtimeLocked() {
        if (propertyId == null || !propertyId.matches("\\d+")) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "GA4 property is not configured");
        }
        if (realtimeCached != null && Instant.now().isBefore(realtimeCachedAt.plusSeconds(25))) return realtimeCached;
        try {
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                    .createScoped(List.of("https://www.googleapis.com/auth/analytics.readonly"));
            credentials.refreshIfExpired();
            String token = credentials.getAccessToken().getTokenValue();
            JsonNode summary = realtimeReport(token, List.of("activeUsers", "screenPageViews", "eventCount", "keyEvents"), null, null);
            JsonNode minutes = realtimeReport(token, List.of("activeUsers"), "minutesAgo", null);
            JsonNode pages = realtimeReport(token, List.of("screenPageViews"), "unifiedScreenName", "screenPageViews");
            JsonNode events = realtimeReport(token, List.of("eventCount"), "eventName", "eventCount");
            JsonNode devices = realtimeReport(token, List.of("activeUsers"), "deviceCategory", "activeUsers");
            JsonNode countries = realtimeReport(token, List.of("activeUsers"), "country", "activeUsers");
            Map<String, Object> result = parseRealtime(summary, minutes, pages, events, devices, countries);
            realtimeCached = result;
            realtimeCachedAt = Instant.now();
            return result;
        } catch (Exception error) {
            throw ga4Failure(error);
        }
    }

    static Map<String, Object> parseRealtime(JsonNode summary, JsonNode minutes, JsonNode pages,
            JsonNode events, JsonNode devices, JsonNode countries) {
        JsonNode total = summary.path("rows").path(0);
        boolean empty = summary.path("rows").isEmpty();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("source", "GA4_REALTIME");
        result.put("period", "LAST_30_MINUTES");
        result.put("status", empty ? "EMPTY" : "LIVE");
        result.put("fetchedAt", Instant.now().toString());
        result.put("metrics", Map.of("activeUsers", metric(total, 0), "pageViews", metric(total, 1),
                "eventCount", metric(total, 2), "keyEvents", metric(total, 3)));
        Map<Long, Long> byMinute = new HashMap<>();
        for (Map<String, Object> row : breakdown(minutes, "minute", "activeUsers")) {
            byMinute.put((long) row.get("minute"), (long) row.get("activeUsers"));
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        if (!empty) for (long minute = 29; minute >= 0; minute--) {
            trend.add(Map.of("minute", minute, "activeUsers", byMinute.getOrDefault(minute, 0L)));
        }
        result.put("trend", trend);
        result.put("pages", breakdown(pages, "title", "pageViews"));
        result.put("events", breakdown(events, "name", "eventCount"));
        result.put("devices", breakdown(devices, "name", "activeUsers"));
        result.put("countries", breakdown(countries, "name", "activeUsers"));
        return result;
    }

    private static List<Map<String, Object>> breakdown(JsonNode report, String dimension, String metricName) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (JsonNode row : report.path("rows")) {
            String label = row.path("dimensionValues").path(0).path("value").asText();
            if (label.isBlank()) continue;
            Object value = "minute".equals(dimension) ? Long.parseLong(label) : label;
            rows.add(Map.of(dimension, value, metricName, metric(row, 0)));
        }
        return rows;
    }

    private JsonNode realtimeReport(String token, List<String> metrics, String dimension, String orderMetric) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("minuteRanges", List.of(Map.of("startMinutesAgo", 29, "endMinutesAgo", 0)));
        body.put("metrics", metrics.stream().map(name -> Map.of("name", name)).toList());
        if (dimension != null) {
            body.put("dimensions", List.of(Map.of("name", dimension)));
            if (orderMetric != null) {
                body.put("orderBys", List.of(Map.of("metric", Map.of("metricName", orderMetric), "desc", true)));
                body.put("limit", "10");
            }
        }
        return client.post().uri("https://analyticsdata.googleapis.com/v1beta/properties/{id}:runRealtimeReport", propertyId)
                .contentType(MediaType.APPLICATION_JSON).headers(headers -> headers.setBearerAuth(token))
                .body(body).retrieve().body(JsonNode.class);
    }

    static Map<String, Object> parseReports(JsonNode summary, JsonNode daily, JsonNode channels) {
        return parseReports(summary, daily, channels, 30);
    }

    static Map<String, Object> parseReports(JsonNode summary, JsonNode daily, JsonNode channels, int days) {
        JsonNode totals = summary.path("rows").path(0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("source", "GA4_CORE");
        result.put("status", summary.path("rows").isEmpty() ? "EMPTY" : "READY");
        result.put("fetchedAt", Instant.now().toString());
        String timezone = summary.path("metadata").path("timeZone").asText();
        if (timezone.isBlank()) timezone = "Asia/Saigon";
        result.put("timezone", timezone);
        LocalDate yesterday = LocalDate.now(ZoneId.of((String) result.get("timezone"))).minusDays(1);
        result.put("fromDate", yesterday.minusDays(days - 1L).toString());
        result.put("toDate", yesterday.toString());
        result.put("thresholded", summary.path("metadata").path("subjectToThresholding").asBoolean(false));
        result.put("otherRows", summary.path("dataLossFromOtherRow").asBoolean(false));
        result.put("activeUsers", metric(totals, 0));
        result.put("sessions", metric(totals, 1));
        result.put("pageViews", metric(totals, 2));
        result.put("eventCount", metric(totals, 3));
        result.put("scrolledUsers", metric(totals, 4));
        result.put("engagementSeconds", metric(totals, 5));
        result.put("bounceRate", totals.path("metricValues").path(6).path("value").asDouble(0));
        List<Map<String, Object>> dailyRows = new ArrayList<>();
        for (JsonNode row : daily.path("rows")) {
            String date = row.path("dimensionValues").path(0).path("value").asText();
            if (date.length() == 8) date = date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6);
            dailyRows.add(Map.of("date", date, "activeUsers", metric(row, 0), "sessions", metric(row, 1)));
        }
        List<Map<String, Object>> sources = new ArrayList<>();
        for (JsonNode row : channels.path("rows")) {
            sources.add(Map.of("channel", row.path("dimensionValues").path(0).path("value").asText(), "sessions", metric(row, 0)));
        }
        result.put("daily", dailyRows);
        result.put("channels", sources);
        result.put("period", days + "daysAgo - yesterday");
        return result;
    }

    private JsonNode report(String token, List<String> metrics, String dimension, int days) {
        return report(token, metrics, dimension, days + "daysAgo", "yesterday");
    }

    private JsonNode report(String token, List<String> metrics, String dimension, String startDate, String endDate) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dateRanges", List.of(Map.of("startDate", startDate, "endDate", endDate)));
        body.put("metrics", metrics.stream().map(name -> Map.of("name", name)).toList());
        if (dimension != null) {
            body.put("dimensions", List.of(Map.of("name", dimension)));
            if ("date".equals(dimension)) body.put("orderBys", List.of(Map.of("dimension", Map.of("dimensionName", "date"))));
            else {
                body.put("orderBys", List.of(Map.of("metric", Map.of("metricName", metrics.getFirst()), "desc", true)));
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

    private static ResponseStatusException ga4Failure(Exception error) {
        if (error instanceof RestClientResponseException response) {
            int status = response.getStatusCode().value();
            if (status == 403) return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "GA4 permission denied", error);
            if (status == 429) return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "GA4 quota exceeded", error);
            if (status == 400) return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "GA4 report metric or dimension unsupported", error);
        }
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Google Analytics Data API unavailable", error);
    }

    static Map<String, Object> comparison(JsonNode current, JsonNode previous) {
        Map<String, Object> result = new LinkedHashMap<>();
        boolean available = !current.path("rows").isEmpty() && !previous.path("rows").isEmpty();
        result.put("status", available ? "AVAILABLE" : "UNAVAILABLE");
        if (!available) return result;
        for (int index = 0; index < 3; index++) {
            String name = List.of("activeUsers", "sessions", "pageViews").get(index);
            long now = metric(current.path("rows").path(0), index);
            long before = metric(previous.path("rows").path(0), index);
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("current", now);
            value.put("previous", before);
            value.put("growthPercent", before == 0 ? null : (now - before) * 100.0 / before);
            result.put(name, value);
        }
        return result;
    }
}
