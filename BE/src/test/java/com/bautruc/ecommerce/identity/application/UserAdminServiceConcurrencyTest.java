package com.bautruc.ecommerce.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.identity.domain.User;
import com.bautruc.ecommerce.identity.domain.UserRole;
import com.bautruc.ecommerce.identity.domain.UserStatus;
import com.bautruc.ecommerce.identity.infrastructure.UserJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class UserAdminServiceConcurrencyTest {
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
    private UserAdminService userAdminService;

    @Autowired
    private UserJpaRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void singleActiveAdminDemoteIsProtected() {
        User admin = save("single-demote-admin", UserRole.ADMIN, UserStatus.ACTIVE, 1);

        assertThatThrownBy(() -> userAdminService.demote(admin.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(IdentityErrorCodes.LAST_ADMIN_PROTECTED);
        assertThat(activeAdminCount()).isEqualTo(1);
    }

    @Test
    void singleActiveAdminBlockIsProtected() {
        User admin = save("single-block-admin", UserRole.ADMIN, UserStatus.ACTIVE, 1);

        assertThatThrownBy(() -> userAdminService.block(admin.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(IdentityErrorCodes.LAST_ADMIN_PROTECTED);
        assertThat(activeAdminCount()).isEqualTo(1);
    }

    @Test
    void twoActiveAdminsAllowOneNormalDemoteAndOneNormalBlock() {
        User adminA = save("normal-admin-a", UserRole.ADMIN, UserStatus.ACTIVE, 1);
        User adminB = save("normal-admin-b", UserRole.ADMIN, UserStatus.ACTIVE, 2);
        User adminC = save("normal-admin-c", UserRole.ADMIN, UserStatus.ACTIVE, 3);

        userAdminService.demote(adminA.getId());
        userAdminService.block(adminB.getId());

        assertThat(userRepository.findById(adminA.getId()).orElseThrow().getRole()).isEqualTo(UserRole.USER);
        assertThat(userRepository.findById(adminB.getId()).orElseThrow().getStatus()).isEqualTo(UserStatus.BLOCKED);
        assertThat(userRepository.findById(adminC.getId()).orElseThrow().isActiveAdmin()).isTrue();
        assertThat(activeAdminCount()).isEqualTo(1);
    }

    @Test
    void concurrentDemoteDemoteCannotRemoveAllActiveAdmins() throws Exception {
        RaceResult result = runRace(
                adminId -> () -> userAdminService.demote(adminId),
                adminId -> () -> userAdminService.demote(adminId)
        );

        assertRaceProtected(result);
    }

    @Test
    void concurrentDemoteBlockCannotRemoveAllActiveAdmins() throws Exception {
        RaceResult result = runRace(
                adminId -> () -> userAdminService.demote(adminId),
                adminId -> () -> userAdminService.block(adminId)
        );

        assertRaceProtected(result);
    }

    @Test
    void concurrentBlockBlockCannotRemoveAllActiveAdmins() throws Exception {
        RaceResult result = runRace(
                adminId -> () -> userAdminService.block(adminId),
                adminId -> () -> userAdminService.block(adminId)
        );

        assertRaceProtected(result);
    }

    private RaceResult runRace(OperationFactory first, OperationFactory second) throws Exception {
        User adminA = save("race-admin-a-" + System.nanoTime(), UserRole.ADMIN, UserStatus.ACTIVE, 1);
        User adminB = save("race-admin-b-" + System.nanoTime(), UserRole.ADMIN, UserStatus.ACTIVE, 2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<OperationResult> firstResult = executor.submit(racingOperation(barrier, first.operation(adminA.getId())));
            Future<OperationResult> secondResult = executor.submit(racingOperation(barrier, second.operation(adminB.getId())));
            OperationResult a = firstResult.get(10, TimeUnit.SECONDS);
            OperationResult b = secondResult.get(10, TimeUnit.SECONDS);
            return new RaceResult(List.of(a, b), activeAdminCount());
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private Callable<OperationResult> racingOperation(CyclicBarrier barrier, Runnable operation) {
        return () -> {
            barrier.await(10, TimeUnit.SECONDS);
            try {
                operation.run();
                return OperationResult.success();
            } catch (BusinessException exception) {
                return OperationResult.businessFailure(exception.code());
            }
        };
    }

    private void assertRaceProtected(RaceResult result) {
        assertThat(result.finalActiveAdminCount()).isGreaterThanOrEqualTo(1);
        assertThat(result.results())
                .anySatisfy(operation -> assertThat(operation.succeeded()).isTrue());
        assertThat(result.results())
                .anySatisfy(operation -> assertThat(operation.errorCode()).isEqualTo(IdentityErrorCodes.LAST_ADMIN_PROTECTED));
    }

    private long activeAdminCount() {
        return userRepository.countByRoleAndStatus(UserRole.ADMIN, UserStatus.ACTIVE);
    }

    private User save(String googleId, UserRole role, UserStatus status, int seconds) {
        Instant timestamp = NOW.plusSeconds(seconds);
        return userRepository.saveAndFlush(new User(
                googleId,
                googleId + "@example.com",
                "User " + googleId,
                null,
                role,
                status,
                timestamp,
                timestamp
        ));
    }

    private interface OperationFactory {
        Runnable operation(Long adminId);
    }

    private record RaceResult(List<OperationResult> results, long finalActiveAdminCount) {
    }

    private record OperationResult(boolean succeeded, String errorCode) {
        static OperationResult success() {
            return new OperationResult(true, null);
        }

        static OperationResult businessFailure(String errorCode) {
            return new OperationResult(false, errorCode);
        }
    }
}
