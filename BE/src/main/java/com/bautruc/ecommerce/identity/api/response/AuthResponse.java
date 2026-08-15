package com.bautruc.ecommerce.identity.api.response;

import java.time.Instant;

public record AuthResponse(
        CurrentUserResponse user,
        Instant expiresAt
) {
}
