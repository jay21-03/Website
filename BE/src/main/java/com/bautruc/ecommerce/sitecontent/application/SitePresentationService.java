package com.bautruc.ecommerce.sitecontent.application;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.common.storage.ObjectStoragePort;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.sitecontent.api.request.SitePresentationRequest;
import com.bautruc.ecommerce.sitecontent.api.response.SitePresentationResponse;
import com.bautruc.ecommerce.sitecontent.domain.SiteMediaSlot;
import com.bautruc.ecommerce.sitecontent.infrastructure.SiteMediaJpaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SitePresentationService {
    private static final Set<String> PAGES = Set.of("brand", "home", "workshop", "support", "catalog", "cart", "orders", "faq", "policy", "checkout");
    private static final Set<String> FIELDS = Set.of("receiverName", "phone", "email", "address", "note");
    private static final Set<String> SECTIONS = Set.of("form", "summary");
    private static final Set<String> COLORS = Set.of("clay", "royal", "minimal", "nature", "sunset", "night", "sage", "ocean");
    private static final Set<String> FONTS = Set.of("heritage", "vietnamese", "editorial", "commerce", "minimal", "modern");

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final SiteMediaJpaRepository media;
    private final ObjectStoragePort storage;
    private final BusinessClock clock;

    public SitePresentationService(JdbcTemplate jdbc, ObjectMapper mapper, SiteMediaJpaRepository media,
                                   ObjectStoragePort storage, BusinessClock clock) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.media = media;
        this.storage = storage;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public SitePresentationResponse current() {
        List<String> saved = jdbc.query("SELECT content_json::text FROM site_presentation WHERE id = 1",
                (rs, row) -> rs.getString(1));
        SitePresentationRequest content = null;
        if (!saved.isEmpty()) {
            try {
                content = mapper.readValue(saved.get(0), SitePresentationRequest.class);
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Stored site presentation is invalid.", exception);
            }
        }
        Map<String, String> urls = Set.of(SiteMediaSlot.CATALOG_HERO, SiteMediaSlot.WORKSHOP_HERO, SiteMediaSlot.SUPPORT_HERO,
                SiteMediaSlot.CHECKOUT_HERO, SiteMediaSlot.SITE_LOGO).stream().collect(Collectors.toMap(SiteMediaSlot::name,
                slot -> media.findBySlot(slot).map(item -> item.getObjectKey())
                        .filter(key -> key != null && !key.isBlank()).map(storage::publicUrl).orElse("")));
        return new SitePresentationResponse(content, urls);
    }

    @Transactional
    public SitePresentationResponse save(SitePresentationRequest request) {
        validate(request);
        try {
            jdbc.update("""
                    INSERT INTO site_presentation (id, content_json, updated_at)
                    VALUES (1, CAST(? AS jsonb), ?)
                    ON CONFLICT (id) DO UPDATE SET content_json = EXCLUDED.content_json,
                        updated_at = EXCLUDED.updated_at
                    """, mapper.writeValueAsString(request), Timestamp.from(clock.now()));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize site presentation.", exception);
        }
        return current();
    }

    private void validate(SitePresentationRequest request) {
        if (!PAGES.containsAll(request.pages().keySet()) || request.pages().values().stream().anyMatch(page ->
                page == null || page.size() > 50 || page.keySet().stream().anyMatch(key ->
                        !key.matches("[a-z][A-Za-z0-9]{0,63}"))) ||
                request.pages().values().stream().mapToInt(Map::size).sum() > 200 ||
                request.pages().values().stream().flatMap(page -> page.values().stream()).anyMatch(value ->
                        value == null || (value.font() != null && !value.font().isBlank() && !FONTS.contains(value.font())) ||
                        (value.color() != null && !value.color().isBlank() && !value.color().matches("#[0-9a-fA-F]{6}"))) ||
                !COLORS.contains(request.colorPreset()) || !FONTS.contains(request.fontPreset()) ||
                !isPermutation(request.checkout().fieldOrder(), FIELDS) ||
                !isPermutation(request.checkout().sectionOrder(), SECTIONS)) {
            throw new BusinessException("SITE_PRESENTATION_INVALID", "Invalid site presentation settings.");
        }
    }

    private boolean isPermutation(List<String> values, Set<String> expected) {
        return values != null && values.size() == expected.size() && values.stream().noneMatch(Objects::isNull)
                && Set.copyOf(values).equals(expected);
    }
}
