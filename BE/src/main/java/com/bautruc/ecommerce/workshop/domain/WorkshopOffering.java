package com.bautruc.ecommerce.workshop.domain;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "workshop_offerings")
public class WorkshopOffering {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_seq")
    @SequenceGenerator(name = "global_seq", sequenceName = "app_global_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    @Column(name = "price_amount", nullable = false)
    private long priceAmount;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "max_participants", nullable = false)
    private int maxParticipants;

    @Column(name = "image_url", length = 1024)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private WorkshopOfferingStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WorkshopOffering() {
    }

    public WorkshopOffering(String title, String description, long priceAmount, int durationMinutes,
                            int maxParticipants, String imageUrl, WorkshopOfferingStatus status, Instant now) {
        update(title, description, priceAmount, durationMinutes, maxParticipants, imageUrl, status, now);
        this.createdAt = required(now);
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public long getPriceAmount() { return priceAmount; }
    public int getDurationMinutes() { return durationMinutes; }
    public int getMaxParticipants() { return maxParticipants; }
    public String getImageUrl() { return imageUrl; }
    public WorkshopOfferingStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public boolean isBookable() {
        return status == WorkshopOfferingStatus.ACTIVE;
    }

    public void update(String title, String description, long priceAmount, int durationMinutes,
                       int maxParticipants, String imageUrl, WorkshopOfferingStatus status, Instant now) {
        this.title = required(title);
        this.description = required(description);
        if (priceAmount < 0) throw new IllegalArgumentException("priceAmount must not be negative");
        if (durationMinutes <= 0) throw new IllegalArgumentException("durationMinutes must be positive");
        if (maxParticipants < 1 || maxParticipants > 100) throw new IllegalArgumentException("maxParticipants must be between 1 and 100");
        this.priceAmount = priceAmount;
        this.durationMinutes = durationMinutes;
        this.maxParticipants = maxParticipants;
        this.imageUrl = blankToNull(imageUrl);
        this.status = required(status);
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
