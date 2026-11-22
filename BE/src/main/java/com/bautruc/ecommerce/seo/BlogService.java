package com.bautruc.ecommerce.seo;

import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.common.exception.ResourceNotFoundException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.bautruc.ecommerce.common.storage.ObjectStoragePort;
import com.bautruc.ecommerce.sitecontent.application.SiteMediaImageValidator;
import com.bautruc.ecommerce.common.config.ApplicationProperties;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BlogService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BlogService.class);
    public record Post(Long id, String slug, String title, String bodyMarkdown, String seoTitle,
                       String seoDescription, String ogTitle, String ogDescription, String imageSlot,
                       String status, Instant publishAt, String imageUrl) {}

    public record Input(@NotBlank @Pattern(regexp = "[a-z][a-z0-9]*(?:-[a-z0-9]+)*") @Size(max = 160) String slug,
                        @NotBlank @Size(max = 180) String title,
                        @NotBlank @Size(max = 50000) String bodyMarkdown,
                        @Size(max = 180) String seoTitle, @Size(max = 500) String seoDescription,
                        @Size(max = 180) String ogTitle, @Size(max = 500) String ogDescription,
                        @Size(max = 64) @Pattern(regexp = "[A-Z][A-Z0-9_]*") String imageSlot,
                        @NotBlank String status, Instant publishAt) {}

    private final JdbcTemplate jdbc;
    private final SeoMetadataService metadata;
    private final ObjectStoragePort storage;
    private final SiteMediaImageValidator images;
    private final long maxImageBytes;

    public BlogService(JdbcTemplate jdbc, SeoMetadataService metadata, ObjectStoragePort storage,
                       SiteMediaImageValidator images, ApplicationProperties properties) {
        this.jdbc = jdbc;
        this.metadata = metadata;
        this.storage = storage;
        this.images = images;
        this.maxImageBytes = properties.image() == null || properties.image().maxBytes() <= 0
                ? 5L * 1024 * 1024 : properties.image().maxBytes();
    }

    @Transactional(readOnly = true)
    public List<Post> publicPosts() {
        return jdbc.query("""
                SELECT id,slug,title,body_markdown,seo_title,seo_description,og_title,og_description,image_slot,status,publish_at,image_key
                FROM blog_posts WHERE (status='PUBLISHED' OR status='SCHEDULED' AND publish_at <= now())
                  AND (slug <> 'mua-gom-bau-truc-lam-qua-tet' OR now() >= timestamptz '2026-12-01 00:00:00+07')
                ORDER BY publish_at DESC NULLS LAST, id DESC
                """, this::map);
    }

    @Transactional(readOnly = true)
    public Post publicPost(String slug) {
        return publicPosts().stream().filter(post -> post.slug().equals(slug)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("BLOG_NOT_FOUND", "Blog post not found."));
    }

    @Transactional(readOnly = true)
    public List<Post> adminPosts() {
        return jdbc.query("""
                SELECT id,slug,title,body_markdown,seo_title,seo_description,og_title,og_description,image_slot,status,publish_at,image_key
                FROM blog_posts ORDER BY id DESC
                """, this::map);
    }

    @Transactional
    public Post save(Long id, Input input) {
        if (!List.of("DRAFT", "SCHEDULED", "PUBLISHED").contains(input.status()))
            throw new BusinessException("BLOG_STATUS_INVALID", "Invalid publication status.");
        if ("SCHEDULED".equals(input.status()) && input.publishAt() == null)
            throw new BusinessException("BLOG_SCHEDULE_REQUIRED", "Scheduled posts need a publication time.");
        if ("mua-gom-bau-truc-lam-qua-tet".equals(input.slug()) &&
                "PUBLISHED".equals(input.status()) && Instant.now().isBefore(Instant.parse("2026-11-30T17:00:00Z")))
            throw new BusinessException("BLOG_PUBLICATION_TOO_EARLY", "This article cannot publish before December 2026.");
        if (input.imageSlot() != null && !input.imageSlot().isBlank()) {
            try {
                if (metadata.imageUrl(input.imageSlot()) == null) throw new IllegalArgumentException();
            } catch (IllegalArgumentException ex) {
                throw new BusinessException("BLOG_IMAGE_INVALID", "Choose an uploaded site image.");
            }
        }
        if (id == null) {
            id = jdbc.queryForObject("""
                    INSERT INTO blog_posts(slug,title,body_markdown,seo_title,seo_description,og_title,og_description,image_slot,status,publish_at)
                    VALUES (?,?,?,?,?,?,?,?,?,?) RETURNING id
                    """, Long.class, input.slug(), input.title(), input.bodyMarkdown(), input.seoTitle(),
                    input.seoDescription(), input.ogTitle(), input.ogDescription(), input.imageSlot(), input.status(), input.publishAt());
        } else {
            long existingId = id;
            var existing = adminPosts().stream().filter(post -> post.id() == existingId).findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("BLOG_NOT_FOUND", "Blog post not found."));
            if (!existing.slug().equals(input.slug()))
                throw new BusinessException("BLOG_SLUG_IMMUTABLE", "Published links are stable; create a new draft for a new URL.");
            jdbc.update("""
                    UPDATE blog_posts SET title=?,body_markdown=?,seo_title=?,seo_description=?,og_title=?,og_description=?,
                        image_slot=?,status=?,publish_at=?,updated_at=now() WHERE id=?
                    """, input.title(), input.bodyMarkdown(), input.seoTitle(), input.seoDescription(), input.ogTitle(),
                    input.ogDescription(), input.imageSlot(), input.status(), input.publishAt(), id);
        }
        Long savedId = id;
        return adminPosts().stream().filter(post -> post.id().equals(savedId)).findFirst().orElseThrow();
    }

    @Transactional
    public Post uploadImage(Long id, MultipartFile file) throws java.io.IOException {
        var current = adminPosts().stream().filter(post -> post.id().equals(id)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("BLOG_NOT_FOUND", "Blog post not found."));
        if (file == null || file.isEmpty() || file.getSize() > maxImageBytes)
            throw new BusinessException("BLOG_IMAGE_INVALID", "Image is empty or too large.");
        var checked = images.validate(file.getBytes(), file.getContentType());
        String key = "site/blog/" + id + "/" + UUID.randomUUID() + "." + checked.extension();
        long postId = id;
        storage.put(key, checked.content(), checked.contentType());
        try {
            String oldKey = jdbc.queryForObject("SELECT image_key FROM blog_posts WHERE id=?", String.class, id);
            jdbc.update("UPDATE blog_posts SET image_key=?,image_slot=NULL,updated_at=now() WHERE id=?", key, id);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    try {
                        if (status == STATUS_ROLLED_BACK) storage.delete(key);
                        else if (oldKey != null) storage.delete(oldKey);
                    } catch (RuntimeException ex) {
                        log.error("Blog image cleanup needed after transaction for post {}", postId, ex);
                    }
                }
            });
        } catch (RuntimeException exception) {
            storage.delete(key);
            throw exception;
        }
        return new Post(current.id(), current.slug(), current.title(), current.bodyMarkdown(), current.seoTitle(),
                current.seoDescription(), current.ogTitle(), current.ogDescription(), null,
                current.status(), current.publishAt(), storage.publicUrl(key));
    }

    private Post map(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new Post(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10),
                rs.getTimestamp(11) == null ? null : rs.getTimestamp(11).toInstant(),
                rs.getString(12) == null ? metadata.imageUrl(rs.getString(9)) : storage.publicUrl(rs.getString(12)));
    }
}
