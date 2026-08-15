package com.bautruc.ecommerce.identity.application;

import com.bautruc.ecommerce.common.security.SecurityErrorCodes;

public final class IdentityErrorCodes {
    public static final String AUTH_INVALID_GOOGLE_CREDENTIAL = "AUTH_INVALID_GOOGLE_CREDENTIAL";
    public static final String USER_BLOCKED = SecurityErrorCodes.USER_BLOCKED;
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String USER_INVALID_ROLE_TRANSITION = "USER_INVALID_ROLE_TRANSITION";
    public static final String LAST_ADMIN_PROTECTED = "LAST_ADMIN_PROTECTED";

    private IdentityErrorCodes() {
    }
}
