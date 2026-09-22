package com.bautruc.ecommerce.sitecontent.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "homepage_content")
public class HomepageContent {
    public static final short SINGLETON_ID = 1;

    @Id
    private Short id;

    @Column(name = "slogan_vi", nullable = false, length = 255)
    private String sloganVi;

    @Column(name = "slogan_en", nullable = false, length = 255)
    private String sloganEn;

    @Column(name = "copy_json", nullable = false, columnDefinition = "text")
    private String copyJson = "{}";

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected HomepageContent() {
    }

    public String getSloganVi() {
        return sloganVi;
    }

    public String getSloganEn() {
        return sloganEn;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getCopyJson() {
        return copyJson == null ? "{}" : copyJson;
    }

    public void updateSlogan(String sloganVi, String sloganEn, Instant now) {
        this.sloganVi = required(sloganVi);
        this.sloganEn = required(sloganEn);
        this.updatedAt = required(now);
    }

    public void updateCopyJson(String copyJson, Instant now) {
        this.copyJson = required(copyJson);
        this.updatedAt = required(now);
    }

    private static String required(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("value is required");
        return value.trim();
    }

    private static <T> T required(T value) {
        if (value == null) throw new IllegalArgumentException("value is required");
        return value;
    }
}
