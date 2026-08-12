package com.bautruc.ecommerce.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.bautruc.ecommerce.common.response.ApiResponse;
import com.bautruc.ecommerce.identity.domain.User;
import com.bautruc.ecommerce.identity.domain.UserRole;
import com.bautruc.ecommerce.identity.domain.UserStatus;
import com.bautruc.ecommerce.identity.infrastructure.UserJpaRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class SecurityConfigIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-08-12T03:30:00Z");

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("bautruc.allowed-origins", () -> "http://localhost:5173, https://shop.example.com");
        registry.add("bautruc.auth.cookie-domain", () -> "example.com");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CsrfTokenRepository csrfTokenRepository;

    @Autowired
    private CsrfAuthenticationStrategy csrfAuthenticationStrategy;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        userRepository.deleteAll();
    }

    @Test
    void csrfEndpointIsPublicAndReturnsTokenAndHttpOnlyCookie() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("XSRF-TOKEN=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("SameSite=Lax")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Secure"))));
    }

    @Test
    void csrfRepositoryAndAuthenticationStrategyAreSharedBeans() {
        assertThat(csrfTokenRepository).isNotNull();
        assertThat(csrfAuthenticationStrategy).isNotNull();
    }

    @Test
    void missingAndInvalidCsrfAreRejectedWithJson() throws Exception {
        User user = userRepository.saveAndFlush(user("csrf-user", "csrf@example.com", UserRole.USER, UserStatus.ACTIVE));

        mockMvc.perform(post("/api/v1/cart/test").cookie(accessCookie(user)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value(SecurityErrorCodes.CSRF_INVALID));

        mockMvc.perform(post("/api/v1/cart/test")
                        .cookie(accessCookie(user))
                        .header("X-XSRF-TOKEN", "bad-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value(SecurityErrorCodes.CSRF_INVALID));
    }

    @Test
    void validCsrfAllowsMutationAndGoogleLoginIsNotCsrfExempt() throws Exception {
        User user = userRepository.saveAndFlush(user("csrf-valid-user", "csrf-valid@example.com", UserRole.USER, UserStatus.ACTIVE));

        mockMvc.perform(post("/api/v1/cart/test").cookie(accessCookie(user)).with(csrf().asHeader()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/google"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value(SecurityErrorCodes.CSRF_INVALID));

        mockMvc.perform(post("/api/v1/auth/google").with(csrf().asHeader()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void realCookieCsrfBrowserFlowAllowsGoogleLoginMutation() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        String csrfToken = csrfTokenFromBody(csrfResult);
        Cookie csrfCookie = xsrfCookieFromSetCookieHeader(csrfResult);
        List<String> xsrfCreationCookies = xsrfCreationCookies(csrfResult);

        assertThat(csrfToken).isNotBlank();
        assertThat(csrfCookie).isNotNull();
        assertThat(csrfCookie.getValue()).isEqualTo(csrfToken);
        assertThat(xsrfCreationCookies).hasSize(1);
        assertThat(csrfResult.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .contains("XSRF-TOKEN=")
                .contains("HttpOnly");

        mockMvc.perform(post("/api/v1/auth/google")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void payosWebhookIsCsrfExempt() throws Exception {
        mockMvc.perform(post("/api/v1/payments/webhook/payos"))
                .andExpect(status().isOk());
    }

    @Test
    void corsAllowsConfiguredCredentialedOriginAndPreflight() throws Exception {
        mockMvc.perform(options("/api/v1/cart/test")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                "Content-Type,X-XSRF-TOKEN,X-Correlation-Id,Idempotency-Key"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, org.hamcrest.Matchers.containsString("POST")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, org.hamcrest.Matchers.containsString("X-XSRF-TOKEN")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, org.hamcrest.Matchers.containsString("X-Correlation-Id")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, org.hamcrest.Matchers.containsString("Idempotency-Key")));

        mockMvc.perform(get("/api/v1/products/test").header(HttpHeaders.ORIGIN, "https://evil.example"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void publicAndProtectedRouteAuthorizationMatrixWorks() throws Exception {
        User user = userRepository.saveAndFlush(user("matrix-user", "matrix-user@example.com", UserRole.USER, UserStatus.ACTIVE));
        User admin = userRepository.saveAndFlush(user("matrix-admin", "matrix-admin@example.com", UserRole.ADMIN, UserStatus.ACTIVE));

        mockMvc.perform(get("/api/v1/products/test")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/collections/test")).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value(SecurityErrorCodes.AUTH_TOKEN_MISSING));

        mockMvc.perform(get("/api/v1/me").cookie(new Cookie(JwtAuthenticationFilter.ACCESS_COOKIE_NAME, "invalid.jwt")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value(SecurityErrorCodes.AUTH_TOKEN_INVALID));

        mockMvc.perform(get("/api/v1/me").cookie(new Cookie(JwtAuthenticationFilter.ACCESS_COOKIE_NAME, expiredToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value(SecurityErrorCodes.AUTH_TOKEN_EXPIRED));

        mockMvc.perform(get("/api/v1/admin/test").cookie(accessCookie(user)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value(SecurityErrorCodes.ACCESS_DENIED));

        mockMvc.perform(get("/api/v1/admin/test").cookie(accessCookie(admin)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/cart/test").cookie(accessCookie(user)).with(csrf().asHeader()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/cart/test").cookie(accessCookie(admin)).with(csrf().asHeader()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value(SecurityErrorCodes.ACCESS_DENIED));
    }

    @Test
    void runtimeDatabaseAuthorityStillWinsForStaleJwtAndBlockedUser() throws Exception {
        User admin = userRepository.saveAndFlush(user("stale-admin", "stale-admin@example.com", UserRole.ADMIN, UserStatus.ACTIVE));
        Cookie staleAdminCookie = accessCookie(admin);
        jdbcTemplate.update("update users set role = 'USER' where id = ?", admin.getId());

        mockMvc.perform(get("/api/v1/admin/test").cookie(staleAdminCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value(SecurityErrorCodes.ACCESS_DENIED));

        User user = userRepository.saveAndFlush(user("stale-user", "stale-user@example.com", UserRole.USER, UserStatus.ACTIVE));
        Cookie staleUserCookie = accessCookie(user);
        jdbcTemplate.update("update users set role = 'ADMIN' where id = ?", user.getId());

        mockMvc.perform(get("/api/v1/admin/test").cookie(staleUserCookie))
                .andExpect(status().isOk());

        User blocked = userRepository.saveAndFlush(user("blocked-old", "blocked-old@example.com", UserRole.USER, UserStatus.ACTIVE));
        Cookie blockedCookie = accessCookie(blocked);
        jdbcTemplate.update("update users set status = 'BLOCKED' where id = ?", blocked.getId());

        mockMvc.perform(get("/api/v1/me").cookie(blockedCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value(SecurityErrorCodes.USER_BLOCKED));
    }

    @Test
    void authenticatedLogoutUsesLogoutFilterAndDeletesCookies() throws Exception {
        User user = userRepository.saveAndFlush(user("logout-user", "logout@example.com", UserRole.USER, UserStatus.ACTIVE));

        MvcResult result = mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(accessCookie(user))
                        .with(csrf().asHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        List<String> setCookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(setCookies).anySatisfy(header -> assertThat(header)
                .contains("BT_ACCESS=")
                .contains("Path=/")
                .contains("Max-Age=0")
                .contains("HttpOnly")
                .contains("SameSite=Lax")
                .contains("Domain=example.com"));
        assertThat(setCookies).anySatisfy(header -> assertThat(header)
                .contains("XSRF-TOKEN=")
                .contains("Max-Age=0"));
        assertThat(setCookies.stream()
                .filter(header -> header.startsWith("XSRF-TOKEN="))
                .count()).isEqualTo(1);

        mockMvc.perform(post("/api/v1/auth/logout").with(csrf().asHeader()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value(SecurityErrorCodes.AUTH_TOKEN_MISSING));
    }

    @Test
    void logoutRequiresValidCsrfAndFreshCsrfCanBeFetchedAfterLogout() throws Exception {
        User user = userRepository.saveAndFlush(user("logout-csrf-user", "logout-csrf@example.com", UserRole.USER, UserStatus.ACTIVE));

        mockMvc.perform(post("/api/v1/auth/logout").cookie(accessCookie(user)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value(SecurityErrorCodes.CSRF_INVALID));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(accessCookie(user))
                        .header("X-XSRF-TOKEN", "bad-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value(SecurityErrorCodes.CSRF_INVALID));

        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    private Cookie accessCookie(User user) {
        return new Cookie(JwtAuthenticationFilter.ACCESS_COOKIE_NAME, jwtTokenService.createAccessToken(user));
    }

    private Cookie xsrfCookieFromSetCookieHeader(MvcResult result) {
        String setCookie = xsrfCreationCookies(result)
                .stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("XSRF-TOKEN creation cookie not found"));
        String value = setCookie.substring("XSRF-TOKEN=".length(), setCookie.indexOf(';'));
        return new Cookie("XSRF-TOKEN", value);
    }

    private List<String> xsrfCreationCookies(MvcResult result) {
        return result.getResponse().getHeaders(HttpHeaders.SET_COOKIE)
                .stream()
                .filter(header -> header.startsWith("XSRF-TOKEN="))
                .filter(header -> !header.contains("Max-Age=0"))
                .filter(header -> !header.startsWith("XSRF-TOKEN=;"))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private String csrfTokenFromBody(MvcResult result) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        Map<String, Object> body = objectMapper.readValue(result.getResponse().getContentAsByteArray(), Map.class);
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        return (String) data.get("token");
    }

    private String expiredToken() {
        return io.jsonwebtoken.Jwts.builder()
                .issuer("bautruc-ecommerce")
                .subject("999")
                .issuedAt(java.util.Date.from(Instant.parse("2000-01-01T00:00:00Z")))
                .expiration(java.util.Date.from(Instant.parse("2000-01-01T01:00:00Z")))
                .claim("userId", 999L)
                .claim("email", "expired@example.com")
                .claim("role", "USER")
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        java.util.Base64.getDecoder().decode("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
                ), io.jsonwebtoken.Jwts.SIG.HS256)
                .compact();
    }

    private User user(String googleId, String email, UserRole role, UserStatus status) {
        return new User(googleId, email, "User", null, role, status, NOW, NOW);
    }

    @TestConfiguration
    static class TestControllers {
        @Bean
        RouteBoundaryController routeBoundaryController() {
            return new RouteBoundaryController();
        }
    }

    @RestController
    static class RouteBoundaryController {
        @GetMapping("/api/v1/products/test")
        ResponseEntity<String> product() {
            return ResponseEntity.ok("product");
        }

        @GetMapping("/api/v1/collections/test")
        ResponseEntity<String> collection() {
            return ResponseEntity.ok("collection");
        }

        @PostMapping("/api/v1/payments/webhook/payos")
        ResponseEntity<String> payos() {
            return ResponseEntity.ok("payos");
        }

        @PostMapping("/api/v1/cart/test")
        ResponseEntity<String> cart() {
            return ResponseEntity.ok("cart");
        }

        @GetMapping("/api/v1/admin/test")
        ResponseEntity<String> admin() {
            return ResponseEntity.ok("admin");
        }
    }
}
