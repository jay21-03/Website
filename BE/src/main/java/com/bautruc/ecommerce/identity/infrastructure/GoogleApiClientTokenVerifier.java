package com.bautruc.ecommerce.identity.infrastructure;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.stereotype.Component;

@Component
class GoogleApiClientTokenVerifier implements GoogleTokenVerifier {
    private final String googleClientId;

    GoogleApiClientTokenVerifier(com.bautruc.ecommerce.common.config.ApplicationProperties properties) {
        this.googleClientId = properties.googleClientId();
    }

    @Override
    public GoogleTokenPayload verify(String credential) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new GoogleTokenVerificationException("GOOGLE_CLIENT_ID is not configured.");
        }
        if (credential == null || credential.isBlank()) {
            throw new GoogleTokenVerificationException("Google credential is required.");
        }

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(credential);
            if (idToken == null) {
                throw new GoogleTokenVerificationException("Google credential is invalid.");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            return new GoogleTokenPayload(
                    payload.getSubject(),
                    payload.getEmail(),
                    payload.getEmailVerified(),
                    (String) payload.get("name"),
                    (String) payload.get("picture")
            );
        } catch (GeneralSecurityException | IOException exception) {
            throw new GoogleTokenVerificationException("Google credential verification failed.", exception);
        }
    }
}
