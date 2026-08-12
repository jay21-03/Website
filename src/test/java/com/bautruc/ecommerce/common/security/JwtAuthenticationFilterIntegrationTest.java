package com.bautruc.ecommerce.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import com.bautruc.ecommerce.identity.domain.User;
import com.bautruc.ecommerce.identity.domain.UserRole;
import com.bautruc.ecommerce.identity.domain.UserStatus;
import com.bautruc.ecommerce.identity.infrastructure.UserJpaRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class JwtAuthenticationFilterIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-08-12T03:30:00Z");

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JwtAuthenticationFilter filter;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noCookieLeavesSecurityContextUnauthenticatedAndContinuesChain() throws Exception {
        MockFilterChain chain = doFilter(new MockHttpServletRequest());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void validUserJwtAuthenticatesActiveUserWithDatabaseAuthority() throws Exception {
        User user = userRepository.saveAndFlush(user("google-user", "user@example.com", UserRole.USER, UserStatus.ACTIVE));
        String token = jwtTokenService.createAccessToken(user);

        doFilter(requestWithToken(token));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(new AuthenticatedUser(user.getId(), user.getEmail(), "USER"));
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    @Test
    void validAdminJwtAuthenticatesActiveAdminWithDatabaseAuthority() throws Exception {
        User admin = userRepository.saveAndFlush(user("google-admin", "admin@example.com", UserRole.ADMIN, UserStatus.ACTIVE));
        String token = jwtTokenService.createAccessToken(admin);

        doFilter(requestWithToken(token));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
    }

    @Test
    void staleAdminJwtUsesCurrentDatabaseUserRoleAfterDemotion() throws Exception {
        User admin = userRepository.saveAndFlush(user("google-stale-admin", "stale-admin@example.com", UserRole.ADMIN, UserStatus.ACTIVE));
        String token = jwtTokenService.createAccessToken(admin);
        jdbcTemplate.update("update users set role = 'USER' where id = ?", admin.getId());

        doFilter(requestWithToken(token));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
        assertThat(((AuthenticatedUser) authentication.getPrincipal()).role()).isEqualTo("USER");
    }

    @Test
    void staleUserJwtUsesCurrentDatabaseAdminRoleAfterPromotion() throws Exception {
        User user = userRepository.saveAndFlush(user("google-stale-user", "stale-user@example.com", UserRole.USER, UserStatus.ACTIVE));
        String token = jwtTokenService.createAccessToken(user);
        jdbcTemplate.update("update users set role = 'ADMIN' where id = ?", user.getId());

        doFilter(requestWithToken(token));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
        assertThat(((AuthenticatedUser) authentication.getPrincipal()).role()).isEqualTo("ADMIN");
    }

    @Test
    void oldValidJwtForBlockedUserIsRejected() throws Exception {
        User user = userRepository.saveAndFlush(user("google-blocked", "blocked@example.com", UserRole.USER, UserStatus.ACTIVE));
        String token = jwtTokenService.createAccessToken(user);
        jdbcTemplate.update("update users set status = 'BLOCKED' where id = ?", user.getId());
        MockHttpServletRequest request = requestWithToken(token);

        doFilter(request);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.AUTH_FAILURE_CODE_ATTRIBUTE))
                .isEqualTo(SecurityErrorCodes.USER_BLOCKED);
    }

    @Test
    void jwtForDeletedUserIsRejected() throws Exception {
        User user = userRepository.saveAndFlush(user("google-deleted", "deleted@example.com", UserRole.USER, UserStatus.ACTIVE));
        String token = jwtTokenService.createAccessToken(user);
        userRepository.deleteAll();
        MockHttpServletRequest request = requestWithToken(token);

        doFilter(request);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.AUTH_FAILURE_CODE_ATTRIBUTE))
                .isEqualTo(SecurityErrorCodes.AUTH_TOKEN_INVALID);
    }

    @Test
    void invalidJwtDoesNotAuthenticate() throws Exception {
        MockHttpServletRequest request = requestWithToken("invalid.jwt.value");

        doFilter(request);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.AUTH_FAILURE_CODE_ATTRIBUTE))
                .isEqualTo(SecurityErrorCodes.AUTH_TOKEN_INVALID);
    }

    @Test
    void expiredJwtDoesNotAuthenticate() throws Exception {
        MockHttpServletRequest request = requestWithToken(expiredToken());

        doFilter(request);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.AUTH_FAILURE_CODE_ATTRIBUTE))
                .isEqualTo(SecurityErrorCodes.AUTH_TOKEN_EXPIRED);
    }

    @Test
    void jwtEmailAndRoleClaimsAreNotRuntimeAuthority() throws Exception {
        User admin = userRepository.saveAndFlush(user("google-current", "old@example.com", UserRole.ADMIN, UserStatus.ACTIVE));
        String token = jwtTokenService.createAccessToken(admin);
        jdbcTemplate.update("update users set email = ?, role = 'USER' where id = ?", "current@example.com", admin.getId());

        doFilter(requestWithToken(token));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        assertThat(principal.email()).isEqualTo("current@example.com");
        assertThat(principal.role()).isEqualTo("USER");
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    private MockFilterChain doFilter(MockHttpServletRequest request) throws java.io.IOException, ServletException {
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return chain;
    }

    private MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(JwtAuthenticationFilter.ACCESS_COOKIE_NAME, token));
        return request;
    }

    private String expiredToken() {
        User transientUser = user("google-expired", "expired@example.com", UserRole.USER, UserStatus.ACTIVE);
        org.springframework.test.util.ReflectionTestUtils.setField(transientUser, "id", 999L);
        return io.jsonwebtoken.Jwts.builder()
                .issuer("bautruc-ecommerce")
                .subject("999")
                .issuedAt(java.util.Date.from(Instant.parse("2000-01-01T00:00:00Z")))
                .expiration(java.util.Date.from(Instant.parse("2000-01-01T01:00:00Z")))
                .claim("userId", 999L)
                .claim("email", transientUser.getEmail())
                .claim("role", transientUser.getRole().name())
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        java.util.Base64.getDecoder().decode("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
                ), io.jsonwebtoken.Jwts.SIG.HS256)
                .compact();
    }

    private User user(String googleId, String email, UserRole role, UserStatus status) {
        return new User(googleId, email, "User", null, role, status, NOW, NOW);
    }
}
