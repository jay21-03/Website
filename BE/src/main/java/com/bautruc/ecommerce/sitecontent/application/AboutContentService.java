package com.bautruc.ecommerce.sitecontent.application;

import java.sql.Timestamp;
import java.util.List;

import com.bautruc.ecommerce.common.storage.ObjectStoragePort;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.sitecontent.api.request.AboutContentRequest;
import com.bautruc.ecommerce.sitecontent.api.response.AboutContentResponse;
import com.bautruc.ecommerce.sitecontent.domain.SiteMediaSlot;
import com.bautruc.ecommerce.sitecontent.infrastructure.SiteMediaJpaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AboutContentService {

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final SiteMediaJpaRepository media;
    private final ObjectStoragePort storage;
    private final BusinessClock clock;

    public AboutContentService(
            JdbcTemplate jdbc,
            ObjectMapper mapper,
            SiteMediaJpaRepository media,
            ObjectStoragePort storage,
            BusinessClock clock
    ) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.media = media;
        this.storage = storage;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AboutContentResponse current() {
        List<String> saved = jdbc.query(
                """
                SELECT content_json::text
                FROM about_page_content
                WHERE id = 1
                """,
                (rs, rowNum) -> rs.getString(1)
        );

        AboutContentRequest content = null;

        if (!saved.isEmpty()) {
            try {
                content = mapper.readValue(
                        saved.get(0),
                        AboutContentRequest.class
                );
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException(
                        "Stored About content is invalid.",
                        exception
                );
            }
        }

        return new AboutContentResponse(
                content,
                url(SiteMediaSlot.ABOUT_HERO),
                url(SiteMediaSlot.ABOUT_PORTRAIT),
                url(SiteMediaSlot.ABOUT_CERTIFICATE)
        );
    }

    @Transactional
    public AboutContentResponse save(AboutContentRequest request) {
        String json;
        try {
            json = mapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Could not serialize About content.",
                    exception
            );
        }

        jdbc.update(
                """
                INSERT INTO about_page_content(
                    id, content_json, updated_at
                )
                VALUES (1, CAST(? AS jsonb), ?)
                ON CONFLICT (id)
                DO UPDATE SET
                    content_json = EXCLUDED.content_json,
                    updated_at = EXCLUDED.updated_at
                """,
                json,
                Timestamp.from(clock.now())
        );

        return current();
    }

    private String url(SiteMediaSlot slot) {
        return media.findBySlot(slot)
                .map(item -> item.getObjectKey())
                .filter(key -> key != null && !key.isBlank())
                .map(storage::publicUrl)
                .orElse(null);
    }
}
