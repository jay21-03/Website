package com.bautruc.ecommerce.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.common.exception.ConflictException;
import com.bautruc.ecommerce.identity.domain.User;
import com.bautruc.ecommerce.identity.domain.UserRole;
import com.bautruc.ecommerce.identity.domain.UserStatus;
import com.bautruc.ecommerce.identity.infrastructure.UserJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "bautruc.admin-emails=admin@example.com,demoted@example.com"
})
@Testcontainers
class GoogleAuthenticationServiceIntegrationTest {
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
    private GoogleAuthenticationService authenticationService;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private FakeGoogleIdentityVerifier googleIdentityVerifier;

    @BeforeEach
    void setUp() {
        googleIdentityVerifier.clear();
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        googleIdentityVerifier.clear();
    }

    @Test
    void normalFirstGoogleLoginCreatesActiveUser() {
        googleIdentityVerifier.register("credential", identity(
                "google-user-1",
                " User@Example.com ",
                "Normal User",
                "https://example.com/user.png"
        ));

        GoogleAuthenticationResult result = authenticationService.authenticate("credential");

        assertThat(result.created()).isTrue();
        assertThat(result.user().getGoogleId()).isEqualTo("google-user-1");
        assertThat(result.user().getEmail()).isEqualTo("user@example.com");
        assertThat(result.user().getFullName()).isEqualTo("Normal User");
        assertThat(result.user().getAvatarUrl()).isEqualTo("https://example.com/user.png");
        assertThat(result.user().getRole()).isEqualTo(UserRole.USER);
        assertThat(result.user().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void adminEmailsFirstLoginCreatesActiveAdmin() {
        googleIdentityVerifier.register("credential", identity(
                "google-admin-1",
                " Admin@Example.com ",
                "Admin User",
                null
        ));

        GoogleAuthenticationResult result = authenticationService.authenticate("credential");

        assertThat(result.created()).isTrue();
        assertThat(result.user().getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(result.user().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void existingBootstrapAdminDemotedToUserIsNotRePromoted() {
        User existing = userRepository.saveAndFlush(new User(
                "google-demoted-1",
                "demoted@example.com",
                "Demoted Admin",
                null,
                UserRole.USER,
                UserStatus.ACTIVE,
                NOW,
                NOW
        ));
        googleIdentityVerifier.register("credential", identity(
                existing.getGoogleId(),
                existing.getEmail(),
                "Google Name",
                "https://example.com/new.png"
        ));

        GoogleAuthenticationResult result = authenticationService.authenticate("credential");

        assertThat(result.created()).isFalse();
        assertThat(result.user().getId()).isEqualTo(existing.getId());
        assertThat(result.user().getRole()).isEqualTo(UserRole.USER);
        assertThat(result.user().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void existingActiveAdminRemainsAdmin() {
        User existing = userRepository.saveAndFlush(new User(
                "google-admin-2",
                "admin2@example.com",
                "Existing Admin",
                null,
                UserRole.ADMIN,
                UserStatus.ACTIVE,
                NOW,
                NOW
        ));
        googleIdentityVerifier.register("credential", identity(existing.getGoogleId(), existing.getEmail()));

        GoogleAuthenticationResult result = authenticationService.authenticate("credential");

        assertThat(result.created()).isFalse();
        assertThat(result.user().getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void existingBlockedUserIsRejectedAndStatusIsNotReset() {
        User existing = userRepository.saveAndFlush(new User(
                "google-blocked-user",
                "blocked-user@example.com",
                "Blocked User",
                null,
                UserRole.USER,
                UserStatus.BLOCKED,
                NOW,
                NOW
        ));
        googleIdentityVerifier.register("credential", identity(existing.getGoogleId(), existing.getEmail()));

        assertThatThrownBy(() -> authenticationService.authenticate("credential"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.code()).isEqualTo(IdentityErrorCodes.USER_BLOCKED);
                    assertThat(exception.status().value()).isEqualTo(401);
                });
        assertThat(userRepository.findById(existing.getId())).get()
                .extracting(User::getStatus)
                .isEqualTo(UserStatus.BLOCKED);
    }

    @Test
    void existingBlockedAdminIsRejected() {
        User existing = userRepository.saveAndFlush(new User(
                "google-blocked-admin",
                "blocked-admin@example.com",
                "Blocked Admin",
                null,
                UserRole.ADMIN,
                UserStatus.BLOCKED,
                NOW,
                NOW
        ));
        googleIdentityVerifier.register("credential", identity(existing.getGoogleId(), existing.getEmail()));

        assertThatThrownBy(() -> authenticationService.authenticate("credential"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.code()).isEqualTo(IdentityErrorCodes.USER_BLOCKED));
    }

    @Test
    void existingUserDoesNotReceiveProfileOrRoleOverwriteFromGoogle() {
        User existing = userRepository.saveAndFlush(new User(
                "google-existing",
                "admin@example.com",
                "Stored Name",
                "https://example.com/stored.png",
                UserRole.USER,
                UserStatus.ACTIVE,
                NOW,
                NOW
        ));
        googleIdentityVerifier.register("credential", identity(
                existing.getGoogleId(),
                existing.getEmail(),
                "Google Changed Name",
                "https://example.com/changed.png"
        ));

        GoogleAuthenticationResult result = authenticationService.authenticate("credential");

        assertThat(result.user().getRole()).isEqualTo(UserRole.USER);
        assertThat(result.user().getFullName()).isEqualTo("Stored Name");
        assertThat(result.user().getAvatarUrl()).isEqualTo("https://example.com/stored.png");
    }

    @Test
    void newGoogleIdWithExistingEmailIsConflict() {
        userRepository.saveAndFlush(new User(
                "existing-google-sub",
                "same@example.com",
                "Existing User",
                null,
                UserRole.USER,
                UserStatus.ACTIVE,
                NOW,
                NOW
        ));
        googleIdentityVerifier.register("credential", identity("new-google-sub", "same@example.com"));

        assertThatThrownBy(() -> authenticationService.authenticate("credential"))
                .isInstanceOf(ConflictException.class);
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void concurrentFirstLoginDoesNotCreateDuplicateUsers() throws Exception {
        int workers = 2;
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        googleIdentityVerifier.register("credential", identity("concurrent-google-sub", "concurrent@example.com"));

        ExecutorService executor = Executors.newFixedThreadPool(workers);
        try {
            List<Callable<GoogleAuthenticationResult>> tasks = new ArrayList<>();
            for (int i = 0; i < workers; i++) {
                tasks.add(() -> {
                    ready.countDown();
                    start.await();
                    return authenticationService.authenticate("credential");
                });
            }
            List<Future<GoogleAuthenticationResult>> futures = tasks.stream()
                    .map(executor::submit)
                    .toList();
            ready.await();
            start.countDown();

            List<GoogleAuthenticationResult> results = new ArrayList<>();
            for (Future<GoogleAuthenticationResult> future : futures) {
                results.add(future.get());
            }

            assertThat(results).hasSize(workers);
            assertThat(userRepository.findByGoogleId("concurrent-google-sub")).isPresent();
            assertThat(userRepository.count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private VerifiedGoogleIdentity identity(String sub, String email) {
        return identity(sub, email, "Google User", null);
    }

    private VerifiedGoogleIdentity identity(String sub, String email, String name, String picture) {
        return new VerifiedGoogleIdentity(sub, email, true, name, picture);
    }

    @TestConfiguration
    static class FakeGoogleIdentityVerifierConfiguration {
        @Bean
        @Primary
        FakeGoogleIdentityVerifier fakeGoogleIdentityVerifier() {
            return new FakeGoogleIdentityVerifier();
        }
    }

    static class FakeGoogleIdentityVerifier implements GoogleIdentityVerifier {
        private final Map<String, VerifiedGoogleIdentity> identities = new ConcurrentHashMap<>();

        @Override
        public VerifiedGoogleIdentity verify(String credential) {
            VerifiedGoogleIdentity identity = identities.get(credential);
            if (identity == null) {
                throw new BusinessException(
                        IdentityErrorCodes.AUTH_INVALID_GOOGLE_CREDENTIAL,
                        "Invalid Google credential.",
                        org.springframework.http.HttpStatus.UNAUTHORIZED
                );
            }
            return identity;
        }

        void register(String credential, VerifiedGoogleIdentity identity) {
            identities.put(credential, identity);
        }

        void clear() {
            identities.clear();
        }
    }
}
