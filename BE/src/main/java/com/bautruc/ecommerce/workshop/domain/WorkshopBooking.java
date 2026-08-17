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
@Table(name = "workshop_bookings")
public class WorkshopBooking {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_seq")
    @SequenceGenerator(name = "global_seq", sequenceName = "app_global_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "workshop_id")
    private Long workshopId;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "phone", nullable = false, length = 32)
    private String phone;

    @Column(name = "preferred_at", nullable = false)
    private Instant preferredAt;

    @Column(name = "participants", nullable = false)
    private int participants;

    @Column(name = "note", length = 1000)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private WorkshopBookingStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WorkshopBooking() {
    }

    public WorkshopBooking(
            String fullName,
            Long workshopId,
            String email,
            String phone,
            Instant preferredAt,
            int participants,
            String note,
            Instant now
    ) {
        this.fullName = required(fullName);
        this.workshopId = workshopId;
        this.email = required(email).toLowerCase(java.util.Locale.ROOT);
        this.phone = required(phone);
        this.preferredAt = required(preferredAt);
        setParticipants(participants);
        this.note = blankToNull(note);
        this.status = WorkshopBookingStatus.NEW;
        this.createdAt = required(now);
        this.updatedAt = now;
    }

    public Long getId() { return id; }
    public Long getWorkshopId() { return workshopId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public Instant getPreferredAt() { return preferredAt; }
    public int getParticipants() { return participants; }
    public String getNote() { return note; }
    public WorkshopBookingStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void changeStatus(WorkshopBookingStatus status, Instant now) {
        this.status = required(status);
        this.updatedAt = required(now);
    }

    private void setParticipants(int participants) {
        if (participants < 1 || participants > 30) {
            throw new IllegalArgumentException("participants must be between 1 and 30");
        }
        this.participants = participants;
    }

    private static String required(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value is required");
        }
        return value.trim();
    }

    private static <T> T required(T value) {
        if (value == null) {
            throw new IllegalArgumentException("value is required");
        }
        return value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
