package com.bautruc.ecommerce.common.security;

public final class SecurityErrorCodes {
    public static final String AUTH_TOKEN_MISSING = "AUTH_TOKEN_MISSING";
    public static final String AUTH_TOKEN_INVALID = "AUTH_TOKEN_INVALID";
    public static final String AUTH_TOKEN_EXPIRED = "AUTH_TOKEN_EXPIRED";
    public static final String USER_BLOCKED = "USER_BLOCKED";
    public static final String ACCESS_DENIED = "ACCESS_DENIED";
    public static final String CSRF_INVALID = "CSRF_INVALID";

    private SecurityErrorCodes() {
    }
}
