package com.bautruc.ecommerce.seo;

import com.bautruc.ecommerce.catalog.infrastructure.CollectionJpaRepository;
import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.common.storage.ObjectStoragePort;
import com.bautruc.ecommerce.sitecontent.domain.SiteMediaSlot;
import com.bautruc.ecommerce.sitecontent.infrastructure.SiteMediaJpaRepository;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeoMetadataService {
    private static final Set<String> STATIC_PATHS = Set.of("default", "/", "/san-pham", "/trai-nghiem",
            "/ve-chung-toi", "/lien-he", "/faq", "/chinh-sach", "/blog");

    public record Metadata(@NotBlank @Size(max = 100) String path, @Size(max = 180) String title, @Size(max = 500) String description,
                           @Size(max = 180) String ogTitle, @Size(max = 500) String ogDescription,
                           @Size(max = 64) String imageSlot) {}

    private final JdbcTemplate jdbc;
    private final CollectionJpaRepository collections;
    private final SiteMediaJpaRepository media;
    private final ObjectStoragePort storage;

    public SeoMetadataService(JdbcTemplate jdbc, CollectionJpaRepository collections,
                              SiteMediaJpaRepository media, ObjectStoragePort storage) {
        this.jdbc = jdbc;
        this.collections = collections;
        this.media = media;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public Map<String, Metadata> all() {
        return jdbc.query("SELECT path,title,description,og_title,og_description,image_slot FROM seo_metadata",
                (rs, row) -> new Metadata(rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6))).stream()
                .collect(Collectors.toMap(Metadata::path, value -> value));
    }

    @Transactional(readOnly = true)
    public Metadata forPath(String path) {
        return jdbc.query("SELECT path,title,description,og_title,og_description,image_slot FROM seo_metadata WHERE path = ?",
                (rs, row) -> new Metadata(rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6)), path)
                .stream().findFirst().orElse(new Metadata(path, null, null, null, null, null));
    }

    @Transactional
    public Metadata save(Metadata value) {
        if (value == null || value.path() == null ||
                !STATIC_PATHS.contains(value.path()) && !(value.path().startsWith("/san-pham/") &&
                        collections.existsBySlug(value.path().substring("/san-pham/".length()))))
            throw new BusinessException("SEO_PATH_INVALID", "Only public page and collection metadata can be updated.");
        if (value.imageSlot() != null && !value.imageSlot().isBlank()) {
            try {
                SiteMediaSlot slot = SiteMediaSlot.valueOf(value.imageSlot());
                if (media.findBySlot(slot).filter(item -> item.getObjectKey() != null).isEmpty()) throw new IllegalArgumentException();
            } catch (IllegalArgumentException ex) {
                throw new BusinessException("SEO_IMAGE_INVALID", "Choose an existing site image.");
            }
        }
        jdbc.update("""
                INSERT INTO seo_metadata(path,title,description,og_title,og_description,image_slot,updated_at)
                VALUES (?,?,?,?,?,?,now())
                ON CONFLICT (path) DO UPDATE SET title=EXCLUDED.title, description=EXCLUDED.description,
                    og_title=EXCLUDED.og_title, og_description=EXCLUDED.og_description,
                    image_slot=EXCLUDED.image_slot, updated_at=now()
                """, value.path(), blank(value.title()), blank(value.description()), blank(value.ogTitle()),
                blank(value.ogDescription()), blank(value.imageSlot()));
        return forPath(value.path());
    }

    @Transactional(readOnly = true)
    public String imageUrl(String slotName) {
        if (slotName == null || slotName.isBlank()) return null;
        return media.findBySlot(SiteMediaSlot.valueOf(slotName)).filter(item -> item.getObjectKey() != null)
                .map(item -> storage.publicUrl(item.getObjectKey())).orElse(null);
    }

    private static String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
