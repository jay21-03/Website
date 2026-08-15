package com.bautruc.ecommerce.identity.application;

import java.util.Locale;

public record VerifiedGoogleIdentity(
        String sub,
        String email,
        boolean emailVerified,
        String name,
        String picture
) {
    public VerifiedGoogleIdentity {
        sub = requireExactSub(sub);
        email = normalizeEmail(email);
    }

    private static String normalizeEmail(String email) {
        return requireText(email, "email").toLowerCase(Locale.ROOT);
    }

    private static String requireExactSub(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("sub is required");
        }
        if (!value.equals(value.trim())) {
            throw new IllegalArgumentException("sub must not contain leading or trailing whitespace");
        }
        return value;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
