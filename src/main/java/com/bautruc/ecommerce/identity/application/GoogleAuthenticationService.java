package com.bautruc.ecommerce.identity.application;

import java.time.Instant;
import java.util.Set;

import com.bautruc.ecommerce.common.config.ApplicationProperties;
import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.common.exception.ConflictException;
import com.bautruc.ecommerce.common.security.JwtTokenService;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.identity.domain.User;
import com.bautruc.ecommerce.identity.domain.UserRole;
import com.bautruc.ecommerce.identity.domain.UserStatus;
import com.bautruc.ecommerce.identity.infrastructure.UserJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class GoogleAuthenticationService {
    private final GoogleIdentityVerifier googleIdentityVerifier;
    private final UserJpaRepository userRepository;
    private final AdminEmailsParser adminEmailsParser;
    private final ApplicationProperties properties;
    private final BusinessClock businessClock;
    private final JwtTokenService jwtTokenService;
    private final TransactionTemplate transactionTemplate;

    public GoogleAuthenticationService(
            GoogleIdentityVerifier googleIdentityVerifier,
            UserJpaRepository userRepository,
            AdminEmailsParser adminEmailsParser,
            ApplicationProperties properties,
            BusinessClock businessClock,
            JwtTokenService jwtTokenService,
            PlatformTransactionManager transactionManager
    ) {
        this.googleIdentityVerifier = googleIdentityVerifier;
        this.userRepository = userRepository;
        this.adminEmailsParser = adminEmailsParser;
        this.properties = properties;
        this.businessClock = businessClock;
        this.jwtTokenService = jwtTokenService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public GoogleAuthenticationResult authenticate(String credential) {
        VerifiedGoogleIdentity identity = googleIdentityVerifier.verify(credential);
        try {
            return transactionTemplate.execute(status -> authenticateVerifiedIdentity(identity));
        } catch (DataIntegrityViolationException exception) {
            return resolveAfterConcurrentCreate(identity);
        }
    }

    private GoogleAuthenticationResult authenticateVerifiedIdentity(VerifiedGoogleIdentity identity) {
        return userRepository.findByGoogleId(identity.sub())
                .map(user -> result(requireActive(user), false))
                .orElseGet(() -> createUser(identity));
    }

    private GoogleAuthenticationResult createUser(VerifiedGoogleIdentity identity) {
        if (userRepository.findByEmail(identity.email()).isPresent()) {
            throw new ConflictException("Google email is already associated with another user.");
        }

        Set<String> adminEmails = adminEmailsParser.parse(properties.adminEmails());
        UserRole role = adminEmails.contains(identity.email()) ? UserRole.ADMIN : UserRole.USER;
        Instant now = businessClock.now();
        User user = new User(
                identity.sub(),
                identity.email(),
                identity.name(),
                identity.picture(),
                role,
                UserStatus.ACTIVE,
                now,
                now
        );
        return result(userRepository.saveAndFlush(user), true);
    }

    private GoogleAuthenticationResult resolveAfterConcurrentCreate(VerifiedGoogleIdentity identity) {
        return transactionTemplate.execute(status -> userRepository.findByGoogleId(identity.sub())
                .map(user -> result(requireActive(user), false))
                .orElseThrow(() -> new ConflictException("Google identity conflicts with an existing user.")));
    }

    private GoogleAuthenticationResult result(User user, boolean created) {
        String accessToken = jwtTokenService.createAccessToken(user);
        return new GoogleAuthenticationResult(
                user,
                created,
                accessToken,
                jwtTokenService.parseAndValidate(accessToken).expiresAt()
        );
    }

    private User requireActive(User user) {
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new BusinessException(
                    IdentityErrorCodes.USER_BLOCKED,
                    "User is blocked.",
                    HttpStatus.UNAUTHORIZED
            );
        }
        return user;
    }
}
