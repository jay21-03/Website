package com.bautruc.ecommerce.common.security;

import com.bautruc.ecommerce.common.config.ApplicationProperties;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class BtAccessCookieFactory {
    private final ApplicationProperties properties;
    private final Environment environment;

    public BtAccessCookieFactory(ApplicationProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    public ResponseCookie create(String token) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(JwtAuthenticationFilter.ACCESS_COOKIE_NAME, token)
                .path("/")
                .httpOnly(true)
                .secure(isProd())
                .sameSite("Lax");
        String domain = properties.auth() == null ? null : properties.auth().cookieDomain();
        if (domain != null && !domain.isBlank()) {
            builder.domain(domain.trim());
        }
        return builder.build();
    }

    private boolean isProd() {
        return environment.matchesProfiles("prod");
    }
}
