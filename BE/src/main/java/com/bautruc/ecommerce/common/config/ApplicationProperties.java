package com.bautruc.ecommerce.common.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bautruc")
public record ApplicationProperties(
        String frontendBaseUrl,
        List<String> allowedOrigins,
        String googleClientId,
        List<String> adminEmails,
        Jwt jwt,
        Payos payos,
        Aws aws,
        Image image,
        Auth auth
) {
    public record Jwt(
            String secretBase64,
            String issuer,
            long ttlSeconds,
            String algorithm
    ) {
    }

    public record Payos(
            String clientId,
            String apiKey,
            String checksumKey,
            String baseUrl
    ) {
    }

    public record Aws(
            String region,
            S3 s3
    ) {
    }

    public record S3(
            String bucketName,
            String publicBaseUrl
    ) {
    }

    public record Image(
            long maxBytes
    ) {
    }

    public record Auth(
            String cookieDomain
    ) {
    }
}
