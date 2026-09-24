package com.bautruc.ecommerce.sitecontent.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "homepage_settings")
public class HomepageSettings {
    public static final short SINGLETON_ID = 1;

    @Id
    private Short id;

    @Column(name = "slogan_vi", nullable = false, length = 200)
    private String sloganVi;

    @Column(name = "slogan_en", nullable = false, length = 200)
    private String sloganEn;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected HomepageSettings() {
    }

    public HomepageSettings(String sloganVi, String sloganEn, Instant updatedAt) {
        this.id = SINGLETON_ID;
        update(sloganVi, sloganEn, updatedAt);
    }

    public void update(String sloganVi, String sloganEn, Instant updatedAt) {
        this.sloganVi = sloganVi;
        this.sloganEn = sloganEn;
        this.updatedAt = updatedAt;
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
}
