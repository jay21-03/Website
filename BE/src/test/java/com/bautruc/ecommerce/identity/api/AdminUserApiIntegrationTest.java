package com.bautruc.ecommerce.identity.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;

import com.bautruc.ecommerce.common.security.JwtAuthenticationFilter;
import com.bautruc.ecommerce.common.security.JwtTokenService;
import com.bautruc.ecommerce.common.security.SecurityErrorCodes;
import com.bautruc.ecommerce.identity.application.IdentityErrorCodes;
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
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AdminUserApiIntegrationTest {
    private static final Instant BASE_TIME = Instant.parse("2026-08-12T03:30:00Z");

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void adminRoutesRequireAdminRoleAndMutationsRequireCsrf() throws Exception {
        User user = save("user-route", "user-route@example.com", "User Route", UserRole.USER, UserStatus.ACTIVE, 1);
        User admin = save("admin-route", "admin-route@example.com", "Admin Route", UserRole.ADMIN, UserStatus.ACTIVE, 2);
        User target = save("target-route", "target-route@example.com", "Target Route", UserRole.USER, UserStatus.ACTIVE, 3);

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value(SecurityErrorCodes.AUTH_TOKEN_MISSING));

        mockMvc.perform(get("/api/v1/admin/users").cookie(accessCookie(user)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value(SecurityErrorCodes.ACCESS_DENIED));

        mockMvc.perform(get("/api/v1/admin/users").cookie(accessCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3));

        BrowserCsrf userCsrf = csrf(accessCookie(user));
        mockMvc.perform(post("/api/v1/admin/users/{id}/promote", target.getId())
                        .cookie(accessCookie(user), userCsrf.cookie())
                        .header("X-XSRF-TOKEN", userCsrf.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value(SecurityErrorCodes.ACCESS_DENIED));

        mockMvc.perform(post("/api/v1/admin/users/{id}/promote", target.getId()).cookie(accessCookie(admin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value(SecurityErrorCodes.CSRF_INVALID));

        BrowserCsrf adminCsrf = csrf(accessCookie(admin));
        mockMvc.perform(post("/api/v1/admin/users/{id}/promote", target.getId())
                        .cookie(accessCookie(admin), adminCsrf.cookie())
                        .header("X-XSRF-TOKEN", adminCsrf.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void listSupportsPaginationKeywordFiltersAndSortWhitelist() throws Exception {
        User admin = save("admin-list", "admin-list@example.com", "Admin List", UserRole.ADMIN, UserStatus.ACTIVE, 1);
        for (int index = 0; index < 24; index++) {
            UserRole role = index % 3 == 0 ? UserRole.ADMIN : UserRole.USER;
            UserStatus status = index % 4 == 0 ? UserStatus.BLOCKED : UserStatus.ACTIVE;
            save(
                    "google-list-" + index,
                    "person%02d@example.com".formatted(index),
                    index == 7 ? "Special Artisan" : "Person %02d".formatted(index),
                    role,
                    status,
                    index + 2
            );
        }
        Cookie adminCookie = accessCookie(admin);

        mockMvc.perform(get("/api/v1/admin/users").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(25))
                .andExpect(jsonPath("$.data.content.length()").value(20));

        mockMvc.perform(get("/api/v1/admin/users?page=1&size=5&sort=email,asc").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.content.length()").value(5));

        mockMvc.perform(get("/api/v1/admin/users?keyword=SPECIAL").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].fullName").value("Special Artisan"));

        mockMvc.perform(get("/api/v1/admin/users?keyword=PERSON07").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].email").value("person07@example.com"));

        mockMvc.perform(get("/api/v1/admin/users?role=USER&status=BLOCKED").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(4));

        mockMvc.perform(get("/api/v1/admin/users?sort=createdAt,asc").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].email").value("admin-list@example.com"));

        mockMvc.perform(get("/api/v1/admin/users?sort=googleId,asc").cookie(adminCookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

        mockMvc.perform(get("/api/v1/admin/users?size=101").cookie(adminCookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void detailReturnsSafeUserDtoAndNotFoundUsesUserCode() throws Exception {
        User admin = save("admin-detail", "admin-detail@example.com", "Admin Detail", UserRole.ADMIN, UserStatus.ACTIVE, 1);
        User target = save("target-detail", "target-detail@example.com", "Target Detail", UserRole.USER, UserStatus.ACTIVE, 2);

        mockMvc.perform(get("/api/v1/admin/users/{id}", target.getId()).cookie(accessCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(target.getId()))
                .andExpect(jsonPath("$.data.email").value("target-detail@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("Target Detail"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.googleId").doesNotExist())
                .andExpect(jsonPath("$.data.jwt").doesNotExist());

        mockMvc.perform(get("/api/v1/admin/users/{id}", 999999L).cookie(accessCookie(admin)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value(IdentityErrorCodes.USER_NOT_FOUND));
    }

    @Test
    void promoteDemoteBlockAndUnblockApplyDocumentedTransitions() throws Exception {
        User actor = save("admin-actor", "admin-actor@example.com", "Admin Actor", UserRole.ADMIN, UserStatus.ACTIVE, 1);
        Cookie actorCookie = accessCookie(actor);

        User activeUser = save("active-user", "active-user@example.com", "Active User", UserRole.USER, UserStatus.ACTIVE, 2);
        postAdmin(actorCookie, "/api/v1/admin/users/%d/promote".formatted(activeUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        User blockedUser = save("blocked-user", "blocked-user@example.com", "Blocked User", UserRole.USER, UserStatus.BLOCKED, 3);
        postAdmin(actorCookie, "/api/v1/admin/users/%d/promote".formatted(blockedUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.status").value("BLOCKED"));

        postAdmin(actorCookie, "/api/v1/admin/users/%d/promote".formatted(activeUser.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value(IdentityErrorCodes.USER_INVALID_ROLE_TRANSITION));

        postAdmin(actorCookie, "/api/v1/admin/users/999999/promote")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value(IdentityErrorCodes.USER_NOT_FOUND));

        postAdmin(actorCookie, "/api/v1/admin/users/%d/demote".formatted(activeUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        postAdmin(actorCookie, "/api/v1/admin/users/%d/demote".formatted(blockedUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.status").value("BLOCKED"));

        postAdmin(actorCookie, "/api/v1/admin/users/%d/demote".formatted(activeUser.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value(IdentityErrorCodes.USER_INVALID_ROLE_TRANSITION));

        postAdmin(actorCookie, "/api/v1/admin/users/%d/block".formatted(activeUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.status").value("BLOCKED"));

        postAdmin(actorCookie, "/api/v1/admin/users/%d/block".formatted(activeUser.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value(IdentityErrorCodes.USER_INVALID_ROLE_TRANSITION));

        postAdmin(actorCookie, "/api/v1/admin/users/%d/unblock".formatted(activeUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        postAdmin(actorCookie, "/api/v1/admin/users/%d/unblock".formatted(activeUser.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value(IdentityErrorCodes.USER_INVALID_ROLE_TRANSITION));
    }

    @Test
    void lastActiveAdminProtectionAndSelfActionRulesWork() throws Exception {
        User soloAdmin = save("solo-admin", "solo-admin@example.com", "Solo Admin", UserRole.ADMIN, UserStatus.ACTIVE, 1);
        Cookie soloCookie = accessCookie(soloAdmin);

        postAdmin(soloCookie, "/api/v1/admin/users/%d/demote".formatted(soloAdmin.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value(IdentityErrorCodes.LAST_ADMIN_PROTECTED));

        postAdmin(soloCookie, "/api/v1/admin/users/%d/block".formatted(soloAdmin.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value(IdentityErrorCodes.LAST_ADMIN_PROTECTED));

        User secondAdmin = save("second-admin", "second-admin@example.com", "Second Admin", UserRole.ADMIN, UserStatus.ACTIVE, 2);
        postAdmin(soloCookie, "/api/v1/admin/users/%d/demote".formatted(soloAdmin.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("USER"));

        Cookie secondCookie = accessCookie(secondAdmin);
        User thirdAdmin = save("third-admin", "third-admin@example.com", "Third Admin", UserRole.ADMIN, UserStatus.ACTIVE, 3);
        postAdmin(secondCookie, "/api/v1/admin/users/%d/block".formatted(thirdAdmin.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.status").value("BLOCKED"));
    }

    @Test
    void staleJwtUsesCurrentDatabaseAuthorityAfterAdminMutations() throws Exception {
        User adminA = save("admin-a", "admin-a@example.com", "Admin A", UserRole.ADMIN, UserStatus.ACTIVE, 1);
        User adminB = save("admin-b", "admin-b@example.com", "Admin B", UserRole.ADMIN, UserStatus.ACTIVE, 2);
        Cookie staleAdminCookie = accessCookie(adminA);
        Cookie adminBCookie = accessCookie(adminB);

        postAdmin(adminBCookie, "/api/v1/admin/users/%d/demote".formatted(adminA.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/users").cookie(staleAdminCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value(SecurityErrorCodes.ACCESS_DENIED));

        mockMvc.perform(get("/api/v1/me").cookie(staleAdminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("USER"));

        User user = save("blocked-runtime-user", "blocked-runtime@example.com", "Blocked Runtime", UserRole.USER, UserStatus.ACTIVE, 3);
        Cookie staleUserCookie = accessCookie(user);
        postAdmin(adminBCookie, "/api/v1/admin/users/%d/block".formatted(user.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/me").cookie(staleUserCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value(SecurityErrorCodes.USER_BLOCKED));
    }

    private org.springframework.test.web.servlet.ResultActions postAdmin(Cookie accessCookie, String path) throws Exception {
        BrowserCsrf csrf = csrf(accessCookie);
        return mockMvc.perform(post(path)
                .cookie(accessCookie, csrf.cookie())
                .header("X-XSRF-TOKEN", csrf.token()));
    }

    private BrowserCsrf csrf(Cookie accessCookie) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf").cookie(accessCookie))
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
        return new BrowserCsrf(token, new Cookie("XSRF-TOKEN", cookieValue));
    }

    private Cookie accessCookie(User user) {
        return new Cookie(JwtAuthenticationFilter.ACCESS_COOKIE_NAME, jwtTokenService.createAccessToken(user));
    }

    private User save(String googleId, String email, String fullName, UserRole role, UserStatus status, int seconds) {
        Instant timestamp = BASE_TIME.plusSeconds(seconds);
        return userRepository.saveAndFlush(new User(
                googleId,
                email,
                fullName,
                "https://example.com/%s.png".formatted(googleId),
                role,
                status,
                timestamp,
                timestamp
        ));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(MvcResult result) throws Exception {
        Map<String, Object> body = objectMapper.readValue(result.getResponse().getContentAsByteArray(), Map.class);
        return (Map<String, Object>) body.get("data");
    }

    private record BrowserCsrf(String token, Cookie cookie) {
    }
}
