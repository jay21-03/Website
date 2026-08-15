package com.bautruc.ecommerce.identity.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;

import com.bautruc.ecommerce.common.config.ApplicationProperties;
import com.bautruc.ecommerce.identity.domain.UserRole;
import com.bautruc.ecommerce.identity.domain.UserStatus;
import com.bautruc.ecommerce.identity.infrastructure.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AdminBootstrapValidatorTest {
    private final UserJpaRepository userRepository = Mockito.mock(UserJpaRepository.class);
    private final AdminEmailsParser adminEmailsParser = new AdminEmailsParser();

    @Test
    void prodPassesWhenActiveAdminExists() {
        when(userRepository.countByRoleAndStatus(UserRole.ADMIN, UserStatus.ACTIVE)).thenReturn(1L);
        AdminBootstrapValidator validator = new AdminBootstrapValidator(
                userRepository,
                adminEmailsParser,
                properties(List.of())
        );

        assertThatCode(() -> validator.run(null)).doesNotThrowAnyException();
    }

    @Test
    void prodPassesWhenAdminEmailsValidAndNonEmpty() {
        when(userRepository.countByRoleAndStatus(UserRole.ADMIN, UserStatus.ACTIVE)).thenReturn(0L);
        AdminBootstrapValidator validator = new AdminBootstrapValidator(
                userRepository,
                adminEmailsParser,
                properties(List.of("admin@example.com"))
        );

        assertThatCode(() -> validator.run(null)).doesNotThrowAnyException();
    }

    @Test
    void prodFailsWhenNoActiveAdminAndAdminEmailsEmpty() {
        when(userRepository.countByRoleAndStatus(UserRole.ADMIN, UserStatus.ACTIVE)).thenReturn(0L);
        AdminBootstrapValidator validator = new AdminBootstrapValidator(
                userRepository,
                adminEmailsParser,
                properties(List.of())
        );

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_EMAILS");
    }

    @Test
    void prodFailsWhenAdminEmailsInvalid() {
        when(userRepository.countByRoleAndStatus(UserRole.ADMIN, UserStatus.ACTIVE)).thenReturn(0L);
        AdminBootstrapValidator validator = new AdminBootstrapValidator(
                userRepository,
                adminEmailsParser,
                properties(List.of("bad-email"))
        );

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_EMAILS contains an invalid email.")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    private ApplicationProperties properties(List<String> adminEmails) {
        return new ApplicationProperties(
                "http://localhost:5173",
                List.of("http://localhost:5173"),
                "test-google-client-id",
                adminEmails,
                new ApplicationProperties.Jwt("", "bautruc-ecommerce", 7200, "HS256"),
                new ApplicationProperties.Payos("", "", "", ""),
                new ApplicationProperties.Aws("", new ApplicationProperties.S3("", "")),
                new ApplicationProperties.Image(5_242_880),
                new ApplicationProperties.Auth("")
        );
    }
}
