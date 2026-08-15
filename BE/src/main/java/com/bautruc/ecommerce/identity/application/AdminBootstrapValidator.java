package com.bautruc.ecommerce.identity.application;

import java.util.Set;

import com.bautruc.ecommerce.common.config.ApplicationProperties;
import com.bautruc.ecommerce.identity.domain.UserRole;
import com.bautruc.ecommerce.identity.domain.UserStatus;
import com.bautruc.ecommerce.identity.infrastructure.UserJpaRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class AdminBootstrapValidator implements ApplicationRunner {
    private final UserJpaRepository userRepository;
    private final AdminEmailsParser adminEmailsParser;
    private final ApplicationProperties properties;

    public AdminBootstrapValidator(
            UserJpaRepository userRepository,
            AdminEmailsParser adminEmailsParser,
            ApplicationProperties properties
    ) {
        this.userRepository = userRepository;
        this.adminEmailsParser = adminEmailsParser;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        long activeAdminCount = userRepository.countByRoleAndStatus(UserRole.ADMIN, UserStatus.ACTIVE);
        if (activeAdminCount > 0) {
            return;
        }

        Set<String> adminEmails = parseAdminEmails();
        if (adminEmails.isEmpty()) {
            throw new IllegalStateException("ADMIN_EMAILS must contain at least one valid email in prod.");
        }
    }

    private Set<String> parseAdminEmails() {
        try {
            return adminEmailsParser.parse(properties.adminEmails());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("ADMIN_EMAILS contains an invalid email.", exception);
        }
    }
}
