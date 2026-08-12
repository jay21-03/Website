package com.bautruc.ecommerce.identity.application;

import java.time.Instant;

import com.bautruc.ecommerce.identity.domain.User;

public record GoogleAuthenticationResult(User user, boolean created, String accessToken, Instant expiresAt) {
}
