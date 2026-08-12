package com.bautruc.ecommerce.common.security;

public record AuthenticatedUser(
        Long userId,
        String email,
        String role
) {
}
