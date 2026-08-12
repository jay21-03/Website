package com.bautruc.ecommerce.common.security;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import com.bautruc.ecommerce.common.config.ApplicationProperties;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.identity.domain.User;
import com.bautruc.ecommerce.identity.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {
    private static final String EXPECTED_ALGORITHM = "HS256";
    private static final int MINIMUM_SECRET_BYTES = 32;

    private final SecretKey signingKey;
    private final String issuer;
    private final long ttlSeconds;
    private final BusinessClock businessClock;

    public JwtTokenService(ApplicationProperties properties, BusinessClock businessClock) {
        ApplicationProperties.Jwt jwt = properties.jwt();
        this.signingKey = signingKey(jwt == null ? null : jwt.secretBase64());
        this.issuer = normalizeIssuer(jwt == null ? null : jwt.issuer());
        this.ttlSeconds = jwt == null ? 7200 : jwt.ttlSeconds();
        this.businessClock = businessClock;
    }

    public String createAccessToken(User user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("user.id is required for JWT issuance");
        }
        Instant issuedAt = businessClock.now();
        Instant expiresAt = issuedAt.plusSeconds(ttlSeconds);

        return Jwts.builder()
                .header()
                .type("JWT")
                .and()
                .issuer(issuer)
                .subject(user.getId().toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim("userId", user.getId())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public ValidatedJwt parseAndValidate(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtAuthenticationException(SecurityErrorCodes.AUTH_TOKEN_MISSING, "JWT is missing.");
        }

        Jws<Claims> jws = parseSignedClaims(token);
        String algorithm = jws.getHeader().getAlgorithm();
        if (!EXPECTED_ALGORITHM.equals(algorithm)) {
            throw invalid();
        }

        Claims claims = jws.getPayload();
        Long userId = requiredLongClaim(claims, "userId");
        String subject = requiredString(claims.getSubject());
        if (!subject.equals(userId.toString())) {
            throw invalid();
        }
        String email = requiredString(claims.get("email", String.class));
        UserRole role = parseRole(requiredString(claims.get("role", String.class)));
        Instant issuedAt = requiredDate(claims.getIssuedAt());
        Instant expiresAt = requiredDate(claims.getExpiration());

        return new ValidatedJwt(userId, email, role, issuer, issuedAt, expiresAt);
    }

    private Jws<Claims> parseSignedClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token);
        } catch (ExpiredJwtException exception) {
            throw new JwtAuthenticationException(SecurityErrorCodes.AUTH_TOKEN_EXPIRED, "JWT is expired.");
        } catch (JwtException | IllegalArgumentException exception) {
            throw invalid();
        }
    }

    private Long requiredLongClaim(Claims claims, String claimName) {
        Object value = claims.get(claimName);
        if (value instanceof Integer integer) {
            return integer.longValue();
        }
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Short shortValue) {
            return shortValue.longValue();
        }
        if (value instanceof Byte byteValue) {
            return byteValue.longValue();
        }
        if (value instanceof BigInteger bigInteger) {
            try {
                return bigInteger.longValueExact();
            } catch (ArithmeticException exception) {
                throw invalid();
            }
        }
        if (value instanceof BigDecimal bigDecimal) {
            try {
                return bigDecimal.longValueExact();
            } catch (ArithmeticException exception) {
                throw invalid();
            }
        }
        throw invalid();
    }

    private String requiredString(String value) {
        if (value == null || value.isBlank()) {
            throw invalid();
        }
        return value;
    }

    private Instant requiredDate(Date value) {
        if (value == null) {
            throw invalid();
        }
        return value.toInstant();
    }

    private UserRole parseRole(String value) {
        try {
            return UserRole.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw invalid();
        }
    }

    private JwtAuthenticationException invalid() {
        return new JwtAuthenticationException(SecurityErrorCodes.AUTH_TOKEN_INVALID, "JWT is invalid.");
    }

    private SecretKey signingKey(String secretBase64) {
        if (secretBase64 == null || secretBase64.isBlank()) {
            throw new IllegalStateException("JWT_SECRET_BASE64 is required.");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(secretBase64.getBytes(StandardCharsets.US_ASCII));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_SECRET_BASE64 must be valid Base64.", exception);
        }
        if (decoded.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET_BASE64 must decode to at least 32 bytes.");
        }
        try {
            return Keys.hmacShaKeyFor(decoded);
        } catch (WeakKeyException exception) {
            throw new IllegalStateException("JWT_SECRET_BASE64 is too weak for HS256.", exception);
        }
    }

    private String normalizeIssuer(String configuredIssuer) {
        if (configuredIssuer == null || configuredIssuer.isBlank()) {
            return "bautruc-ecommerce";
        }
        return configuredIssuer;
    }
}
