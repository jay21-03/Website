package com.bautruc.ecommerce.identity.infrastructure;

interface GoogleTokenVerifier {
    GoogleTokenPayload verify(String credential);
}
