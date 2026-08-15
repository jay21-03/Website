package com.bautruc.ecommerce.identity.application;

public interface GoogleIdentityVerifier {
    VerifiedGoogleIdentity verify(String credential);
}
