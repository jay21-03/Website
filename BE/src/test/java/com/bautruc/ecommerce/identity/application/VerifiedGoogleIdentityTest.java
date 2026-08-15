package com.bautruc.ecommerce.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class VerifiedGoogleIdentityTest {
    @Test
    void preservesGoogleSubExactly() {
        VerifiedGoogleIdentity identity = new VerifiedGoogleIdentity(
                "GoogleSub-ABC_123",
                " User@Example.com ",
                true,
                "User",
                null
        );

        assertThat(identity.sub()).isEqualTo("GoogleSub-ABC_123");
        assertThat(identity.email()).isEqualTo("user@example.com");
    }

    @Test
    void rejectsBlankSub() {
        assertThatThrownBy(() -> new VerifiedGoogleIdentity(" ", "user@example.com", true, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sub");
    }

    @Test
    void rejectsSubWithLeadingOrTrailingWhitespaceWithoutNormalizingIt() {
        assertThatThrownBy(() -> new VerifiedGoogleIdentity(" google-sub ", "user@example.com", true, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sub");
    }
}
