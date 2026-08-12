package com.bautruc.ecommerce.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import com.bautruc.ecommerce.identity.domain.User;
import com.bautruc.ecommerce.identity.domain.UserRole;
import com.bautruc.ecommerce.identity.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@ActiveProfiles("test")
@Testcontainers
class UserJpaRepositoryTest {
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
    private UserJpaRepository userJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void savesAndFindsUserByGoogleIdAndEmail() {
        User user = userJpaRepository.saveAndFlush(new User(
                "google-sub-1",
                "Admin@Example.com",
                "Admin User",
                "https://example.com/avatar.png",
                UserRole.ADMIN,
                UserStatus.ACTIVE,
                NOW,
                NOW
        ));

        assertThat(user.getId()).isNotNull();
        assertThat(user.getEmail()).isEqualTo("admin@example.com");
        assertThat(userJpaRepository.findByGoogleId("google-sub-1")).contains(user);
        assertThat(userJpaRepository.findByEmail("admin@example.com")).contains(user);
        assertThat(userJpaRepository.countByRoleAndStatus(UserRole.ADMIN, UserStatus.ACTIVE)).isEqualTo(1);
    }

    @Test
    void usesAuthoritativeGlobalSequenceForGeneratedUserIds() {
        Long authoritativeSequenceCount = jdbcTemplate.queryForObject(
                "select count(*) from pg_class where relkind = 'S' and relname = 'app_global_id_seq'",
                Long.class
        );
        Long legacySequenceCount = jdbcTemplate.queryForObject(
                "select count(*) from pg_class where relkind = 'S' and relname = 'bt_global_sequence'",
                Long.class
        );

        User user = userJpaRepository.saveAndFlush(user("sequence-google-id", "sequence@example.com"));

        assertThat(authoritativeSequenceCount).isEqualTo(1);
        assertThat(legacySequenceCount).isZero();
        assertThat(user.getId()).isNotNull().isInstanceOf(Long.class);
    }

    @Test
    void persistsRoleAndStatusAsStrings() {
        User user = userJpaRepository.saveAndFlush(new User(
                "enum-google-id",
                "enum@example.com",
                "Enum User",
                null,
                UserRole.ADMIN,
                UserStatus.BLOCKED,
                NOW,
                NOW
        ));

        String role = jdbcTemplate.queryForObject("select role from users where id = ?", String.class, user.getId());
        String status = jdbcTemplate.queryForObject("select status from users where id = ?", String.class, user.getId());

        assertThat(role).isEqualTo("ADMIN");
        assertThat(status).isEqualTo("BLOCKED");
    }

    @Test
    void enforcesUniqueGoogleId() {
        userJpaRepository.saveAndFlush(user("same-google-id", "one@example.com"));

        assertThatThrownBy(() -> userJpaRepository.saveAndFlush(user("same-google-id", "two@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void enforcesUniqueEmail() {
        userJpaRepository.saveAndFlush(user("google-sub-1", "same@example.com"));

        assertThatThrownBy(() -> userJpaRepository.saveAndFlush(user("google-sub-2", "same@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void locksActiveAdminRowsForLastAdminProtection() {
        userJpaRepository.saveAndFlush(new User(
                "admin-google-id",
                "admin@example.com",
                "Admin",
                null,
                UserRole.ADMIN,
                UserStatus.ACTIVE,
                NOW,
                NOW
        ));
        userJpaRepository.saveAndFlush(new User(
                "active-user-google-id",
                "active-user@example.com",
                "Active User",
                null,
                UserRole.USER,
                UserStatus.ACTIVE,
                NOW,
                NOW
        ));
        userJpaRepository.saveAndFlush(new User(
                "blocked-admin-google-id",
                "blocked-admin@example.com",
                "Blocked Admin",
                null,
                UserRole.ADMIN,
                UserStatus.BLOCKED,
                NOW,
                NOW
        ));
        userJpaRepository.saveAndFlush(new User(
                "blocked-user-google-id",
                "blocked-user@example.com",
                "Blocked User",
                null,
                UserRole.USER,
                UserStatus.BLOCKED,
                NOW,
                NOW
        ));

        assertThat(userJpaRepository.findActiveAdminsForUpdate())
                .hasSize(1)
                .allMatch(User::isActiveAdmin);
    }

    private User user(String googleId, String email) {
        return new User(
                googleId,
                email,
                "Test User",
                null,
                UserRole.USER,
                UserStatus.ACTIVE,
                NOW,
                NOW
        );
    }
}
