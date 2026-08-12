package com.bautruc.ecommerce.identity.infrastructure;

import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.identity.application.GoogleIdentityVerifier;
import com.bautruc.ecommerce.identity.application.IdentityErrorCodes;
import com.bautruc.ecommerce.identity.application.VerifiedGoogleIdentity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class GoogleIdentityVerifierAdapter implements GoogleIdentityVerifier {
    private final GoogleTokenVerifier googleTokenVerifier;

    public GoogleIdentityVerifierAdapter(GoogleTokenVerifier googleTokenVerifier) {
        this.googleTokenVerifier = googleTokenVerifier;
    }

    @Override
    public VerifiedGoogleIdentity verify(String credential) {
        try {
            GoogleTokenPayload payload = googleTokenVerifier.verify(credential);
            if (!Boolean.TRUE.equals(payload.emailVerified())) {
                throw invalidCredential();
            }
            return new VerifiedGoogleIdentity(
                    payload.subject(),
                    payload.email(),
                    true,
                    payload.name(),
                    payload.picture()
            );
        } catch (GoogleTokenVerificationException | IllegalArgumentException exception) {
            throw invalidCredential();
        }
    }

    private BusinessException invalidCredential() {
        return new BusinessException(
                IdentityErrorCodes.AUTH_INVALID_GOOGLE_CREDENTIAL,
                "Invalid Google credential.",
                HttpStatus.UNAUTHORIZED
        );
    }
}
