package com.bautruc.ecommerce.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.identity.application.IdentityErrorCodes;
import com.bautruc.ecommerce.identity.application.VerifiedGoogleIdentity;
import org.junit.jupiter.api.Test;

class GoogleIdentityVerifierAdapterTest {
    @Test
    void mapsVerifiedGooglePayload() {
        GoogleIdentityVerifierAdapter adapter = adapterReturning(new GoogleTokenPayload(
                "google-sub-1",
                " Admin@Example.com ",
                true,
                "Admin User",
                "https://example.com/avatar.png"
        ));

        VerifiedGoogleIdentity identity = adapter.verify("credential");

        assertThat(identity.sub()).isEqualTo("google-sub-1");
        assertThat(identity.email()).isEqualTo("admin@example.com");
        assertThat(identity.emailVerified()).isTrue();
        assertThat(identity.name()).isEqualTo("Admin User");
        assertThat(identity.picture()).isEqualTo("https://example.com/avatar.png");
    }

    @Test
    void invalidCredentialIsRejected() {
        assertInvalid(adapterThrowing("invalid credential"));
    }

    @Test
    void invalidSignatureIsRejected() {
        assertInvalid(adapterThrowing("invalid signature"));
    }

    @Test
    void wrongAudienceIsRejected() {
        assertInvalid(adapterThrowing("wrong audience"));
    }

    @Test
    void invalidIssuerIsRejected() {
        assertInvalid(adapterThrowing("invalid issuer"));
    }

    @Test
    void expiredTokenIsRejected() {
        assertInvalid(adapterThrowing("expired token"));
    }

    @Test
    void unverifiedEmailIsRejected() {
        assertInvalid(adapterReturning(new GoogleTokenPayload(
                "google-sub-1",
                "user@example.com",
                false,
                "User",
                null
        )));
    }

    @Test
    void missingEmailIsRejected() {
        assertInvalid(adapterReturning(new GoogleTokenPayload(
                "google-sub-1",
                null,
                true,
                "User",
                null
        )));
    }

    @Test
    void missingSubjectIsRejected() {
        assertInvalid(adapterReturning(new GoogleTokenPayload(
                null,
                "user@example.com",
                true,
                "User",
                null
        )));
    }

    private GoogleIdentityVerifierAdapter adapterReturning(GoogleTokenPayload payload) {
        return new GoogleIdentityVerifierAdapter(credential -> payload);
    }

    private GoogleIdentityVerifierAdapter adapterThrowing(String message) {
        return new GoogleIdentityVerifierAdapter(credential -> {
            throw new GoogleTokenVerificationException(message);
        });
    }

    private void assertInvalid(GoogleIdentityVerifierAdapter adapter) {
        assertThatThrownBy(() -> adapter.verify("credential"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.code()).isEqualTo(IdentityErrorCodes.AUTH_INVALID_GOOGLE_CREDENTIAL);
                    assertThat(exception.status().value()).isEqualTo(401);
                });
    }
}
