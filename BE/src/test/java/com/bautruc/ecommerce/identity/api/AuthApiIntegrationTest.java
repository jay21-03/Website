package com.bautruc.ecommerce.identity.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.common.security.JwtAuthenticationFilter;
import com.bautruc.ecommerce.common.security.JwtTokenService;
import com.bautruc.ecommerce.common.security.SecurityErrorCodes;
import com.bautruc.ecommerce.common.security.ValidatedJwt;
import com.bautruc.ecommerce.identity.application.GoogleIdentityVerifier;
import com.bautruc.ecommerce.identity.application.IdentityErrorCodes;
import com.bautruc.ecommerce.identity.application.VerifiedGoogleIdentity;
import com.bautruc.ecommerce.identity.domain.User;
import com.bautruc.ecommerce.identity.domain.UserRole;
import com.bautruc.ecommerce.identity.domain.UserStatus;
import com.bautruc.ecommerce.identity.infrastructure.UserJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AuthApiIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-08-12T03:30:00Z");

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("bautruc.admin-emails", () -> "admin@example.com,demoted@example.com");
        registry.add("bautruc.auth.cookie-domain", () -> "example.com");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FakeGoogleIdentityVerifier googleIdentityVerifier;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        googleIdentityVerifier.clear();
        userRepository.deleteAll();
    }

    @Test
    void requestValidationAndCsrfProtectionWork() throws Exception {
        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"credential\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value(SecurityErrorCodes.CSRF_INVALID));

        BrowserCsrf csrf = csrf();
        mockMvc.perform(post("/api/v1/auth/google")
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

        mockMvc.perform(post("/api/v1/auth/google")
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

        mockMvc.perform(post("/api/v1/auth/google")
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

        mockMvc.perform(post("/api/v1/auth/google")
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void firstUserLoginCreatesUserSetsCookieAndDoesNotExposeJwtInJson() throws Exception {
        googleIdentityVerifier.register("credential-user", identity(
                "google-user-1",
                "User@Example.com",
                "Normal User",
                "https://example.com/user.png"
        ));
        BrowserCsrf csrf = csrf();

        MvcResult result = login("credential-user", csrf)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.user.fullName").value("Normal User"))
                .andExpect(jsonPath("$.data.user.avatarUrl").value("https://example.com/user.png"))
                .andExpect(jsonPath("$.data.user.role").value("USER"))
                .andExpect(jsonPath("$.data.user.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.token").doesNotExist())
                .andExpect(jsonPath("$.data.jwt").doesNotExist())
                .andReturn();

        String accessToken = btAccessToken(result);
        ValidatedJwt jwt = jwtTokenService.parseAndValidate(accessToken);
        assertThat(authResponseExpiresAt(result)).isEqualTo(jwt.expiresAt().toString());
        assertThat(btAccessCreationCookies(result)).hasSize(1);
        assertThat(btAccessCreationCookies(result).getFirst())
                .contains("BT_ACCESS=")
                .contains("Path=/")
                .contains("HttpOnly")
                .contains("SameSite=Lax")
                .contains("Domain=example.com")
                .doesNotContain("Secure");
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void adminFirstLoginUsesAdminEmailsBootstrap() throws Exception {
        googleIdentityVerifier.register("credential-admin", identity("google-admin-1", "Admin@Example.com"));

        login("credential-admin", csrf())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.user.status").value("ACTIVE"));
    }

    @Test
    void existingUserLoginUsesDatabaseStateAndDoesNotReapplyAdminEmails() throws Exception {
        User existing = userRepository.saveAndFlush(new User(
                "google-demoted",
                "demoted@example.com",
                "Stored Name",
                "https://example.com/stored.png",
                UserRole.USER,
                UserStatus.ACTIVE,
                NOW,
                NOW
        ));
        googleIdentityVerifier.register("credential-demoted", identity(
                existing.getGoogleId(),
                existing.getEmail(),
                "Google Name",
                "https://example.com/google.png"
        ));

        login("credential-demoted", csrf())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.id").value(existing.getId()))
                .andExpect(jsonPath("$.data.user.role").value("USER"))
                .andExpect(jsonPath("$.data.user.fullName").value("Stored Name"))
                .andExpect(jsonPath("$.data.user.avatarUrl").value("https://example.com/stored.png"));
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void blockedAndInvalidGoogleLoginDoNotSetBtAccessOrDestroyAnonymousCsrf() throws Exception {
        User blocked = userRepository.saveAndFlush(new User(
                "google-blocked",
                "blocked@example.com",
                "Blocked",
                null,
                UserRole.USER,
                UserStatus.BLOCKED,
                NOW,
                NOW
        ));
        googleIdentityVerifier.register("credential-blocked", identity(blocked.getGoogleId(), blocked.getEmail()));
        BrowserCsrf csrf = csrf();

        login("credential-blocked", csrf)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value(SecurityErrorCodes.USER_BLOCKED))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        googleIdentityVerifier.reject("credential-invalid");
        BrowserCsrf csrf2 = csrf();
        login("credential-invalid", csrf2)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value(IdentityErrorCodes.AUTH_INVALID_GOOGLE_CREDENTIAL))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        googleIdentityVerifier.register("credential-after-failure", identity("google-after-failure", "after@example.com"));
        login("credential-after-failure", csrf2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.email").value("after@example.com"));
    }

    @Test
    void emailConflictReturns409() throws Exception {
        userRepository.saveAndFlush(new User(
                "google-existing",
                "same@example.com",
                "Existing",
                null,
                UserRole.USER,
                UserStatus.ACTIVE,
                NOW,
                NOW
        ));
        googleIdentityVerifier.register("credential-conflict", identity("google-new", "same@example.com"));

        login("credential-conflict", csrf())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void csrfRotatesOnSuccessfulLoginAndOldTokenIsRejected() throws Exception {
        googleIdentityVerifier.register("credential-rotation", identity("google-rotation", "rotation@example.com"));
        BrowserCsrf tokenA = csrf();

        MvcResult login = login("credential-rotation", tokenA)
                .andExpect(status().isOk())
                .andReturn();
        Cookie btAccess = btAccessCookie(login);
        assertThat(login.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                .anySatisfy(header -> assertThat(header).contains("XSRF-TOKEN=").contains("Max-Age=0"));

        BrowserCsrf tokenB = csrf(btAccess);
        assertThat(tokenB.token()).isNotEqualTo(tokenA.token());

        mockMvc.perform(post("/api/v1/cart/test")
                        .cookie(btAccess, tokenB.cookie())
                        .header("X-XSRF-TOKEN", tokenA.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value(SecurityErrorCodes.CSRF_INVALID));

        mockMvc.perform(post("/api/v1/cart/test")
                        .cookie(btAccess, tokenB.cookie())
                        .header("X-XSRF-TOKEN", tokenB.token()))
                .andExpect(status().isOk());
    }

    @Test
    void meReflectsCurrentDatabaseStateForUserAndAdminAndRejectsInvalidStates() throws Exception {
        googleIdentityVerifier.register("credential-me-user", identity(
                "google-me-user",
                "me-user@example.com",
                "Me User",
                "https://example.com/me.png"
        ));
        MvcResult login = login("credential-me-user", csrf()).andExpect(status().isOk()).andReturn();
        Cookie btAccess = btAccessCookie(login);
        Long userId = userRepository.findByEmail("me-user@example.com").orElseThrow().getId();

        mockMvc.perform(get("/api/v1/me").cookie(btAccess))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(userId))
                .andExpect(jsonPath("$.data.email").value("me-user@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("Me User"))
                .andExpect(jsonPath("$.data.avatarUrl").value("https://example.com/me.png"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.jwt").doesNotExist());

        jdbcTemplate.update("update users set email = ?, full_name = ?, avatar_url = ?, role = 'ADMIN' where id = ?",
                "current@example.com", "Current Name", "https://example.com/current.png", userId);
        mockMvc.perform(get("/api/v1/me").cookie(btAccess))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("current@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("Current Name"))
                .andExpect(jsonPath("$.data.avatarUrl").value("https://example.com/current.png"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));

        jdbcTemplate.update("update users set status = 'BLOCKED' where id = ?", userId);
        mockMvc.perform(get("/api/v1/me").cookie(btAccess))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value(SecurityErrorCodes.USER_BLOCKED));

        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value(SecurityErrorCodes.AUTH_TOKEN_MISSING));
        mockMvc.perform(get("/api/v1/me").cookie(new Cookie(JwtAuthenticationFilter.ACCESS_COOKIE_NAME, "bad.jwt")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value(SecurityErrorCodes.AUTH_TOKEN_INVALID));
    }

    @Test
    void meReflectsDemotedDatabaseRoleWhenAdminJwtIsStale() throws Exception {
        googleIdentityVerifier.register("credential-me-admin", identity(
                "google-me-admin",
                "admin@example.com",
                "Me Admin",
                "https://example.com/admin.png"
        ));
        MvcResult login = login("credential-me-admin", csrf())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.role").value("ADMIN"))
                .andReturn();
        Cookie adminIssuedBtAccess = btAccessCookie(login);
        Long userId = userRepository.findByEmail("admin@example.com").orElseThrow().getId();

        jdbcTemplate.update("update users set role = 'USER' where id = ?", userId);

        mockMvc.perform(get("/api/v1/me").cookie(adminIssuedBtAccess))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(userId))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void completeLoginFreshCsrfMeAndLogoutFlowWorks() throws Exception {
        googleIdentityVerifier.register("credential-flow", identity("google-flow", "flow@example.com"));
        BrowserCsrf tokenA = csrf();

        MvcResult login = login("credential-flow", tokenA)
                .andExpect(status().isOk())
                .andReturn();
        Cookie btAccess = btAccessCookie(login);
        BrowserCsrf tokenB = csrf(btAccess);

        mockMvc.perform(get("/api/v1/me").cookie(btAccess))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("flow@example.com"));

        MvcResult logout = mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(btAccess, tokenB.cookie())
                        .header("X-XSRF-TOKEN", tokenB.token()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(logout.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                .anySatisfy(header -> assertThat(header).contains("BT_ACCESS=").contains("Max-Age=0"));
        assertThat(logout.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                .anySatisfy(header -> assertThat(header).contains("XSRF-TOKEN=").contains("Max-Age=0"));

        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value(SecurityErrorCodes.AUTH_TOKEN_MISSING));
    }

    private org.springframework.test.web.servlet.ResultActions login(String credential, BrowserCsrf csrf) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/google")
                .cookie(csrf.cookie())
                .header("X-XSRF-TOKEN", csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credential\":\"" + credential + "\"}"));
    }

    private BrowserCsrf csrf(Cookie... cookies) throws Exception {
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request = get("/api/v1/auth/csrf");
        if (cookies.length > 0) {
            request.cookie(cookies);
        }
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();
        String token = (String) data(result).get("token");
        String cookieHeader = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE)
                .stream()
                .filter(header -> header.startsWith("XSRF-TOKEN="))
                .filter(header -> !header.contains("Max-Age=0"))
                .findFirst()
                .orElseThrow();
        String cookieValue = cookieHeader.substring("XSRF-TOKEN=".length(), cookieHeader.indexOf(';'));
        assertThat(cookieValue).isEqualTo(token);
        return new BrowserCsrf(token, new Cookie("XSRF-TOKEN", cookieValue));
    }

    private Cookie btAccessCookie(MvcResult result) {
        return new Cookie(JwtAuthenticationFilter.ACCESS_COOKIE_NAME, btAccessToken(result));
    }

    private String btAccessToken(MvcResult result) {
        String header = btAccessCreationCookies(result).getFirst();
        return header.substring("BT_ACCESS=".length(), header.indexOf(';'));
    }

    private List<String> btAccessCreationCookies(MvcResult result) {
        return result.getResponse().getHeaders(HttpHeaders.SET_COOKIE)
                .stream()
                .filter(header -> header.startsWith("BT_ACCESS="))
                .filter(header -> !header.contains("Max-Age=0"))
                .toList();
    }

    private String authResponseExpiresAt(MvcResult result) throws Exception {
        return (String) data(result).get("expiresAt");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(MvcResult result) throws Exception {
        Map<String, Object> body = objectMapper.readValue(result.getResponse().getContentAsByteArray(), Map.class);
        return (Map<String, Object>) body.get("data");
    }

    private VerifiedGoogleIdentity identity(String sub, String email) {
        return identity(sub, email, "Google User", null);
    }

    private VerifiedGoogleIdentity identity(String sub, String email, String name, String picture) {
        return new VerifiedGoogleIdentity(sub, email, true, name, picture);
    }

    private record BrowserCsrf(String token, Cookie cookie) {
    }

    @TestConfiguration
    static class FakeVerifierConfiguration {
        @Bean
        @Primary
        FakeGoogleIdentityVerifier fakeGoogleIdentityVerifier() {
            return new FakeGoogleIdentityVerifier();
        }

        @Bean
        TestCartController testCartController() {
            return new TestCartController();
        }
    }

    static class FakeGoogleIdentityVerifier implements GoogleIdentityVerifier {
        private final Map<String, VerifiedGoogleIdentity> identities = new ConcurrentHashMap<>();
        private final java.util.Set<String> rejected = ConcurrentHashMap.newKeySet();

        @Override
        public VerifiedGoogleIdentity verify(String credential) {
            if (rejected.contains(credential)) {
                throw new BusinessException(
                        IdentityErrorCodes.AUTH_INVALID_GOOGLE_CREDENTIAL,
                        "Invalid Google credential.",
                        HttpStatus.UNAUTHORIZED
                );
            }
            VerifiedGoogleIdentity identity = identities.get(credential);
            if (identity == null) {
                throw new BusinessException(
                        IdentityErrorCodes.AUTH_INVALID_GOOGLE_CREDENTIAL,
                        "Invalid Google credential.",
                        HttpStatus.UNAUTHORIZED
                );
            }
            return identity;
        }

        void register(String credential, VerifiedGoogleIdentity identity) {
            identities.put(credential, identity);
            rejected.remove(credential);
        }

        void reject(String credential) {
            rejected.add(credential);
        }

        void clear() {
            identities.clear();
            rejected.clear();
        }
    }

    @RestController
    static class TestCartController {
        @PostMapping("/api/v1/cart/test")
        ResponseEntity<String> cart() {
            return ResponseEntity.ok("cart");
        }
    }
}
