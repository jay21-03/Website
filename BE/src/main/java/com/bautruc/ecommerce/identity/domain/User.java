package com.bautruc.ecommerce.identity.domain;

import java.time.Instant;
import java.util.Locale;

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
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "app_global_id_seq_generator")
    @SequenceGenerator(
            name = "app_global_id_seq_generator",
            sequenceName = "app_global_id_seq",
            allocationSize = 1
    )
    private Long id;

    @Column(name = "google_id", nullable = false, length = 128, unique = true)
    private String googleId;

    @Column(name = "email", nullable = false, length = 320, unique = true)
    private String email;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "avatar_url", length = 1024)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 16)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private UserStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
    }

    public User(
            String googleId,
            String email,
            String fullName,
            String avatarUrl,
            UserRole role,
            UserStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.googleId = requireText(googleId, "googleId");
        this.email = normalizeEmail(email);
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.role = requireNonNull(role, "role");
        this.status = requireNonNull(status, "status");
        this.createdAt = requireNonNull(createdAt, "createdAt");
        this.updatedAt = requireNonNull(updatedAt, "updatedAt");
    }

    public Long getId() {
        return id;
    }

    public String getGoogleId() {
        return googleId;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public UserRole getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isActiveAdmin() {
        return role == UserRole.ADMIN && status == UserStatus.ACTIVE;
    }

    public void promote(Instant updatedAt) {
        this.role = UserRole.ADMIN;
        this.updatedAt = requireNonNull(updatedAt, "updatedAt");
    }

    public void demote(Instant updatedAt) {
        this.role = UserRole.USER;
        this.updatedAt = requireNonNull(updatedAt, "updatedAt");
    }

    public void block(Instant updatedAt) {
        this.status = UserStatus.BLOCKED;
        this.updatedAt = requireNonNull(updatedAt, "updatedAt");
    }

    public void unblock(Instant updatedAt) {
        this.status = UserStatus.ACTIVE;
        this.updatedAt = requireNonNull(updatedAt, "updatedAt");
    }

    private static String normalizeEmail(String email) {
        return requireText(email, "email").toLowerCase(Locale.ROOT);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }
}
