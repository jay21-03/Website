package com.bautruc.ecommerce.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import com.bautruc.ecommerce.common.config.ApplicationProperties;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.identity.domain.User;
import com.bautruc.ecommerce.identity.domain.UserRole;
import com.bautruc.ecommerce.identity.domain.UserStatus;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenServiceTest {
    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    private static final String SECRET = base64("0123456789abcdef0123456789abcdef");
    private static final String OTHER_SECRET = base64("abcdef0123456789abcdef0123456789");

    @Test
    void createsValidUserTokenWithRequiredClaims() {
        JwtTokenService service = service(SECRET, NOW);
        User user = user(10L, "user@example.com", UserRole.USER);

        String token = service.createAccessToken(user);
        ValidatedJwt jwt = service.parseAndValidate(token);

        assertThat(jwt.userId()).isEqualTo(10L);
        assertThat(jwt.email()).isEqualTo("user@example.com");
        assertThat(jwt.role()).isEqualTo(UserRole.USER);
        assertThat(jwt.issuer()).isEqualTo("bautruc-ecommerce");
        assertThat(jwt.issuedAt()).isEqualTo(NOW);
        assertThat(jwt.expiresAt()).isEqualTo(NOW.plusSeconds(7200));
        assertThat(decodedHeader(token)).contains("\"alg\":\"HS256\"");
        assertThat(decodedPayload(token))
                .contains("\"sub\":\"10\"")
                .contains("\"userId\":10")
                .contains("\"email\":\"user@example.com\"")
                .contains("\"role\":\"USER\"")
                .contains("\"iss\":\"bautruc-ecommerce\"")
                .contains("\"iat\":")
                .contains("\"exp\":")
                .doesNotContain("status");
    }

    @Test
    void createsValidAdminToken() {
        JwtTokenService service = service(SECRET, NOW);
        User user = user(11L, "admin@example.com", UserRole.ADMIN);

        ValidatedJwt jwt = service.parseAndValidate(service.createAccessToken(user));

        assertThat(jwt.userId()).isEqualTo(11L);
        assertThat(jwt.role()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void rejectsModifiedSignatureOrWrongSecret() {
        String token = service(SECRET, NOW).createAccessToken(user(10L, "user@example.com", UserRole.USER));

        assertInvalid(() -> service(OTHER_SECRET, NOW).parseAndValidate(token));
        assertInvalid(() -> service(SECRET, NOW).parseAndValidate(token.substring(0, token.length() - 2) + "aa"));
    }

    @Test
    void rejectsMalformedToken() {
        assertInvalid(() -> service(SECRET, NOW).parseAndValidate("not-a-jwt"));
    }

    @Test
    void distinguishesExpiredToken() {
        String token = manualToken(SECRET, "bautruc-ecommerce", "10", 10L, "user@example.com", "USER",
                Instant.parse("2000-01-01T00:00:00Z"), Instant.parse("2000-01-01T01:00:00Z"));

        assertThatThrownBy(() -> service(SECRET, NOW).parseAndValidate(token))
                .isInstanceOfSatisfying(JwtAuthenticationException.class,
                        exception -> assertThat(exception.code()).isEqualTo(SecurityErrorCodes.AUTH_TOKEN_EXPIRED));
    }

    @Test
    void rejectsWrongIssuer() {
        String token = manualToken(SECRET, "wrong-issuer", "10", 10L, "user@example.com", "USER",
                NOW, NOW.plusSeconds(7200));

        assertInvalid(() -> service(SECRET, NOW).parseAndValidate(token));
    }

    @Test
    void rejectsMissingRequiredClaim() {
        String token = Jwts.builder()
                .issuer("bautruc-ecommerce")
                .subject("10")
                .issuedAt(Date.from(NOW))
                .expiration(Date.from(NOW.plusSeconds(7200)))
                .claim("userId", 10L)
                .claim("role", "USER")
                .signWith(key(SECRET), Jwts.SIG.HS256)
                .compact();

        assertInvalid(() -> service(SECRET, NOW).parseAndValidate(token));
    }

    @Test
    void rejectsSubjectUserIdMismatch() {
        String token = manualToken(SECRET, "bautruc-ecommerce", "10", 11L, "user@example.com", "USER",
                NOW, NOW.plusSeconds(7200));

        assertInvalid(() -> service(SECRET, NOW).parseAndValidate(token));
    }

    @Test
    void rejectsMalformedUserIdValue() {
        String token = Jwts.builder()
                .issuer("bautruc-ecommerce")
                .subject("10")
                .issuedAt(Date.from(NOW))
                .expiration(Date.from(NOW.plusSeconds(7200)))
                .claim("userId", "abc")
                .claim("email", "user@example.com")
                .claim("role", "USER")
                .signWith(key(SECRET), Jwts.SIG.HS256)
                .compact();

        assertInvalid(() -> service(SECRET, NOW).parseAndValidate(token));
    }

    @Test
    void rejectsStringNumericUserIdClaim() {
        String token = manualToken(SECRET, "bautruc-ecommerce", "10", "10", "user@example.com", "USER",
                NOW, NOW.plusSeconds(7200));

        assertInvalid(() -> service(SECRET, NOW).parseAndValidate(token));
    }

    @Test
    void rejectsFractionalNumericUserIdClaim() {
        String token = manualToken(SECRET, "bautruc-ecommerce", "10", 10.5D, "user@example.com", "USER",
                NOW, NOW.plusSeconds(7200));

        assertInvalid(() -> service(SECRET, NOW).parseAndValidate(token));
    }

    @Test
    void acceptsIntegralNumericUserIdClaim() {
        String token = manualToken(SECRET, "bautruc-ecommerce", "10", 10, "user@example.com", "USER",
                NOW, NOW.plusSeconds(7200));

        assertThat(service(SECRET, NOW).parseAndValidate(token).userId()).isEqualTo(10L);
    }

    @Test
    void rejectsOverflowNumericUserIdClaim() {
        String token = manualToken(
                SECRET,
                "bautruc-ecommerce",
                "9223372036854775808",
                new BigInteger("9223372036854775808"),
                "user@example.com",
                "USER",
                NOW,
                NOW.plusSeconds(7200)
        );

        assertInvalid(() -> service(SECRET, NOW).parseAndValidate(token));
    }

    @Test
    void rejectsInvalidRoleClaim() {
        String token = manualToken(SECRET, "bautruc-ecommerce", "10", 10L, "user@example.com", "SUPER_ADMIN",
                NOW, NOW.plusSeconds(7200));

        assertInvalid(() -> service(SECRET, NOW).parseAndValidate(token));
    }

    @Test
    void rejectsInvalidBase64Secret() {
        assertThatThrownBy(() -> service("not-base64!!", NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("valid Base64");
    }

    @Test
    void rejectsDecodedSecretShorterThan32Bytes() {
        assertThatThrownBy(() -> service(base64("short-secret"), NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void acceptsDecodedSecretAtLeast32Bytes() {
        assertThat(service(SECRET, NOW)).isNotNull();
    }

    private void assertInvalid(ThrowingCall call) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(JwtAuthenticationException.class,
                        exception -> assertThat(exception.code()).isEqualTo(SecurityErrorCodes.AUTH_TOKEN_INVALID));
    }

    private JwtTokenService service(String secretBase64, Instant now) {
        return new JwtTokenService(properties(secretBase64), new FixedBusinessClock(now));
    }

    private ApplicationProperties properties(String secretBase64) {
        return new ApplicationProperties(
                "http://localhost:5173",
                List.of("http://localhost:5173"),
                "google-client-id",
                List.of(),
                new ApplicationProperties.Jwt(secretBase64, "bautruc-ecommerce", 7200, "HS256"),
                new ApplicationProperties.Payos("", "", "", ""),
                new ApplicationProperties.Aws("", new ApplicationProperties.S3("", "")),
                new ApplicationProperties.Image(5_242_880),
                new ApplicationProperties.Auth("")
        );
    }

    private User user(Long id, String email, UserRole role) {
        User user = new User("google-" + id, email, "User", null, role, UserStatus.ACTIVE, NOW, NOW);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private String manualToken(
            String secretBase64,
            String issuer,
            String subject,
            Object userId,
            String email,
            String role,
            Instant issuedAt,
            Instant expiresAt
    ) {
        return Jwts.builder()
                .issuer(issuer)
                .subject(subject)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim("userId", userId)
                .claim("email", email)
                .claim("role", role)
                .signWith(key(secretBase64), Jwts.SIG.HS256)
                .compact();
    }

    private SecretKey key(String secretBase64) {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretBase64));
    }

    private String decodedHeader(String token) {
        return new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]), StandardCharsets.UTF_8);
    }

    private String decodedPayload(String token) {
        return new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
    }

    private static String base64(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    @FunctionalInterface
    private interface ThrowingCall {
        void run();
    }

    private record FixedBusinessClock(Instant now) implements BusinessClock {
        @Override
        public ZoneId businessZone() {
            return ZoneId.of("Asia/Ho_Chi_Minh");
        }

        @Override
        public Instant startOfDay(LocalDate date) {
            return BusinessClock.super.startOfDay(date);
        }
    }
}
