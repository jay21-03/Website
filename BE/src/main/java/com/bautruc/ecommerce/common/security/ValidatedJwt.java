package com.bautruc.ecommerce.common.security;

import java.time.Instant;

import com.bautruc.ecommerce.identity.domain.UserRole;

public record ValidatedJwt(
        Long userId,
        String email,
        UserRole role,
        String issuer,
        Instant issuedAt,
        Instant expiresAt
) {
}
