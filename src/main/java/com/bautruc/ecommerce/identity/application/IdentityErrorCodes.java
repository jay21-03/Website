package com.bautruc.ecommerce.identity.application;

import com.bautruc.ecommerce.common.security.SecurityErrorCodes;

public final class IdentityErrorCodes {
    public static final String AUTH_INVALID_GOOGLE_CREDENTIAL = "AUTH_INVALID_GOOGLE_CREDENTIAL";
    public static final String USER_BLOCKED = SecurityErrorCodes.USER_BLOCKED;

    private IdentityErrorCodes() {
    }
}
