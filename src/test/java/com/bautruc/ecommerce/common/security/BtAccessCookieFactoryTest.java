package com.bautruc.ecommerce.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bautruc.ecommerce.common.config.ApplicationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

class BtAccessCookieFactoryTest {
    @Test
    void prodCookieUsesSecureFlagDomainAndSharedAttributes() {
        Environment environment = mock(Environment.class);
        when(environment.matchesProfiles("prod")).thenReturn(true);
        BtAccessCookieFactory factory = new BtAccessCookieFactory(properties(" example.com "), environment);

        String cookie = factory.create("jwt-value").toString();

        assertThat(cookie)
                .startsWith("BT_ACCESS=jwt-value")
                .contains("Path=/")
                .contains("Domain=example.com")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Lax");
    }

    @Test
    void nonProdCookieOmitsSecureFlagAndBlankDomain() {
        Environment environment = mock(Environment.class);
        when(environment.matchesProfiles("prod")).thenReturn(false);
        BtAccessCookieFactory factory = new BtAccessCookieFactory(properties(" "), environment);

        String cookie = factory.create("jwt-value").toString();

        assertThat(cookie)
                .startsWith("BT_ACCESS=jwt-value")
                .contains("Path=/")
                .contains("HttpOnly")
                .contains("SameSite=Lax")
                .doesNotContain("Secure")
                .doesNotContain("Domain=");
    }

    private static ApplicationProperties properties(String cookieDomain) {
        return new ApplicationProperties(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new ApplicationProperties.Auth(cookieDomain)
        );
    }
}
