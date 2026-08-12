package com.bautruc.ecommerce.common.security;

public class JwtAuthenticationException extends RuntimeException {
    private final String code;

    public JwtAuthenticationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
