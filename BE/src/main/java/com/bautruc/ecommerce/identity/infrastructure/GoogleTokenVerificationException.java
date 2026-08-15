package com.bautruc.ecommerce.identity.infrastructure;

class GoogleTokenVerificationException extends RuntimeException {
    GoogleTokenVerificationException(String message) {
        super(message);
    }

    GoogleTokenVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
