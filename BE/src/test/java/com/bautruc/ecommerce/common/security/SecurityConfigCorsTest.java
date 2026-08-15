package com.bautruc.ecommerce.common.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.bautruc.ecommerce.common.config.ApplicationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

class SecurityConfigCorsTest {
    @Test
    void wildcardCredentialedCorsOriginIsRejected() {
        SecurityConfig securityConfig = new SecurityConfig(
                null,
                null,
                properties(List.of("*")),
                org.mockito.Mockito.mock(Environment.class)
        );

        assertThatThrownBy(securityConfig::corsConfigurationSource)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ALLOWED_ORIGINS");
    }

    private ApplicationProperties properties(List<String> allowedOrigins) {
        return new ApplicationProperties(
                "http://localhost:5173",
                allowedOrigins,
                "google-client-id",
                List.of(),
                new ApplicationProperties.Jwt("", "bautruc-ecommerce", 7200, "HS256"),
                new ApplicationProperties.Payos("", "", "", ""),
                new ApplicationProperties.Aws("", new ApplicationProperties.S3("", "")),
                new ApplicationProperties.Image(5_242_880),
                new ApplicationProperties.Auth("")
        );
    }
}
