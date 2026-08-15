package com.bautruc.ecommerce.identity.infrastructure;

record GoogleTokenPayload(
        String subject,
        String email,
        Boolean emailVerified,
        String name,
        String picture
) {
}
