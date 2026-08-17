package com.bautruc.ecommerce.support.domain;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "support_settings")
public class SupportSettings {
    public static final short SINGLETON_ID = 1;

    @Id
    private Short id;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(name = "zalo_phone", nullable = false, length = 32)
    private String zaloPhone;

    @Column(name = "secondary_phone", length = 32)
    private String secondaryPhone;

    @Column(name = "facebook_url", length = 1024)
    private String facebookUrl;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(name = "map_url", length = 1024)
    private String mapUrl;

    @Column(name = "opening_hours", length = 255)
    private String openingHours;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SupportSettings() {
    }

    public Short getId() { return id; }
    public String getEmail() { return email; }
    public String getZaloPhone() { return zaloPhone; }
    public String getSecondaryPhone() { return secondaryPhone; }
    public String getFacebookUrl() { return facebookUrl; }
    public String getAddress() { return address; }
    public String getMapUrl() { return mapUrl; }
    public String getOpeningHours() { return openingHours; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(String email, String zaloPhone, String secondaryPhone, String facebookUrl,
                       String address, String mapUrl, String openingHours, Instant now) {
        this.email = required(email);
        this.zaloPhone = required(zaloPhone);
        this.secondaryPhone = blankToNull(secondaryPhone);
        this.facebookUrl = blankToNull(facebookUrl);
        this.address = required(address);
        this.mapUrl = blankToNull(mapUrl);
        this.openingHours = blankToNull(openingHours);
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

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
