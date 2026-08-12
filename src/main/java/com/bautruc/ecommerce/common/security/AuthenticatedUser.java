package com.bautruc.ecommerce.common.security;

import java.util.UUID;

public record AuthenticatedUser(
        UUID id,
        String email,
        String role,
        String status
) {
}
