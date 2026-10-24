package com.bautruc.ecommerce.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import com.bautruc.ecommerce.cart.application.CartApplicationService;
import com.bautruc.ecommerce.catalog.api.request.CollectionRequest;
import com.bautruc.ecommerce.catalog.api.request.DiscountRequest;
import com.bautruc.ecommerce.catalog.api.request.ProductRequest;
import com.bautruc.ecommerce.catalog.application.CatalogService;
import com.bautruc.ecommerce.catalog.domain.*;
import com.bautruc.ecommerce.commerce.api.request.CheckoutRequest;
import com.bautruc.ecommerce.commerce.api.response.CheckoutResponse;
import com.bautruc.ecommerce.commerce.application.CheckoutApplicationService;
import com.bautruc.ecommerce.commerce.application.PaymentResultApplicationService;
import com.bautruc.ecommerce.common.security.*;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.identity.domain.*;
import com.bautruc.ecommerce.identity.infrastructure.UserJpaRepository;
import com.bautruc.ecommerce.inventory.application.InventoryCommandService;
import com.bautruc.ecommerce.inventory.domain.InventoryTransactionType;
import com.bautruc.ecommerce.notification.application.*;
import com.bautruc.ecommerce.notification.domain.NotificationType;
import com.bautruc.ecommerce.notification.infrastructure.SseEmitterRegistry;
import com.bautruc.ecommerce.order.application.OrderCommandService;
import com.bautruc.ecommerce.order.domain.OrderStatus;
import com.bautruc.ecommerce.payment.application.*;
import com.bautruc.ecommerce.payment.domain.Payment;
import com.bautruc.ecommerce.payment.infrastructure.PaymentJpaRepository;
import com.bautruc.ecommerce.reporting.application.ReportingQueryService;
import com.bautruc.ecommerce.reporting.domain.ReportGroupBy;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;

import org.springframework.test.context.ActiveProfiles;

import org.springframework.test.context.DynamicPropertyRegistry;

import org.springframework.test.context.DynamicPropertySource;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers
class Phase5BackendIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void db(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired NotificationService notificationService;
    @Autowired AdminNotificationService adminNotifications;
    @Autowired SseEmitterRegistry emitters;
    @Autowired ReportingQueryService reporting;
    @Autowired CatalogService catalog;
    @Autowired InventoryCommandService inventory;
    @Autowired CartApplicationService cart;
    @Autowired CheckoutApplicationService checkout;
    @Autowired PaymentResultApplicationService paymentResults;
    @Autowired OrderCommandService orderCommands;
    @Autowired PaymentJpaRepository payments;
    @Autowired UserJpaRepository users;
    @Autowired BusinessClock clock;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mockMvc;
    @MockitoBean PayOSClient payos;

    @BeforeEach void clean() {
        SecurityContextHolder.clearContext();
        jdbc.execute("TRUNCATE TABLE notifications, collections, users CASCADE");
        when(payos.createPaymentRequest(any())).thenAnswer(invocation -> {
            PayOSCreatePaymentCommand command = invocation.getArgument(0);
            return new PayOSPaymentResult(command.orderId(), command.amount(), "link-" + command.orderId(),
                    "PENDING", "https://pay.test/" + command.orderId(), "qr-" + command.orderId());
        });
    }

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void notificationReadStateIsPerAdminAndBlockedAdminIsNotRecipient() {
        User adminA = user("notify-a", UserRole.ADMIN, UserStatus.ACTIVE);
        User adminB = user("notify-b", UserRole.ADMIN, UserStatus.ACTIVE);
        user("notify-blocked", UserRole.ADMIN, UserStatus.BLOCKED);
        notificationService.create(NotificationType.NEW_ORDER, "New", "Order created", "ORDER", 10L,
                null, "NEW_ORDER:ORDER:10", clock.now());

        authenticate(adminA);
        var unreadA = adminNotifications.list(false, null, 0, 20, null);
        assertThat(unreadA.totalElements()).isOne();
        assertThat(adminNotifications.markRead(unreadA.content().getFirst().id()).isRead()).isTrue();
        assertThat(adminNotifications.list(false, null, 0, 20, null).totalElements()).isZero();

        authenticate(adminB);
        assertThat(adminNotifications.list(false, null, 0, 20, null).totalElements()).isOne();
        assertThat(jdbc.queryForObject("select count(*) from notification_recipients", Long.class)).isEqualTo(2);
    }

    @Test void businessAnalyticsUsesReadOnlyPeriodQueriesAndProtectsAdminRoute() throws Exception {
        User admin = user("business-admin", UserRole.ADMIN, UserStatus.ACTIVE);
        User buyer = user("business-buyer", UserRole.USER, UserStatus.ACTIVE);
        var report = reporting.businessAnalytics(30);
        assertThat(report).containsEntry("source", "POSTGRESQL");
        var metrics = (java.util.Map<?, ?>) report.get("metrics");
        assertThat(metrics.get("completed_paid_revenue")).isEqualTo("0");
        assertThat(metrics.get("average_order_value")).isNull();
        Instant now = clock.now();
        jdbc.update("""
                INSERT INTO workshop_bookings(full_name,email,phone,preferred_at,participants,status,created_at,updated_at)
                VALUES ('Test guest','test@example.invalid','0000000000',?,3,'NEW',?,?)
                """, java.sql.Timestamp.from(now.plusSeconds(3600)), java.sql.Timestamp.from(now), java.sql.Timestamp.from(now));
        var interest = (java.util.List<?>) reporting.businessAnalytics(30).get("workshopInterest");
        assertThat(interest).hasSize(1);
        assertThat(((java.util.Map<?, ?>) interest.getFirst()).get("bookings")).isEqualTo(1L);
        assertThat(((java.util.Map<?, ?>) interest.getFirst()).get("participants")).isEqualTo(3L);
        Instant old = now.minusSeconds(120L * 24 * 60 * 60);
        jdbc.update("""
                INSERT INTO workshop_bookings(full_name,email,phone,preferred_at,participants,status,created_at,updated_at)
                VALUES ('Earlier guest','earlier@example.invalid','0000000000',?,2,'NEW',?,?)
                """, java.sql.Timestamp.from(old.plusSeconds(3600)), java.sql.Timestamp.from(old), java.sql.Timestamp.from(old));
        assertThat(((java.util.Map<?, ?>) reporting.businessAnalytics(30).get("metrics")).get("workshop_bookings")).isEqualTo(1L);
        assertThat(((java.util.Map<?, ?>) reporting.businessAnalytics(0).get("metrics")).get("workshop_bookings")).isEqualTo(2L);
        assertThat(((java.util.Map<?, ?>) reporting.businessAnalytics(0).get("period")).get("fromDate")).isNull();
        long paidOrder = jdbc.queryForObject("SELECT nextval('app_global_id_seq')", Long.class);
        long cancelledOrder = jdbc.queryForObject("SELECT nextval('app_global_id_seq')", Long.class);
        for (long id : List.of(paidOrder, cancelledOrder)) {
            jdbc.update("""
                    INSERT INTO orders(id,order_code,user_id,receiver_name,phone,email,address,total_amount,status,completed_at,created_at,updated_at)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                    """, id, "BI-" + id, buyer.getId(), "Test", "0123456789", "test@example.test", "Test address",
                    120000L, id == paidOrder ? "COMPLETED" : "CANCELLED",
                    id == paidOrder ? java.sql.Timestamp.from(now) : null,
                    java.sql.Timestamp.from(now), java.sql.Timestamp.from(now));
        }
        jdbc.update("""
                INSERT INTO payments(order_id,provider,status,amount,created_at,expires_at,paid_at,updated_at)
                VALUES (?,'PAYOS','PAID',120000,?,?,?,?)
                """, paidOrder, java.sql.Timestamp.from(now), java.sql.Timestamp.from(now.plusSeconds(3600)),
                java.sql.Timestamp.from(now), java.sql.Timestamp.from(now));
        jdbc.update("""
                INSERT INTO payments(order_id,provider,status,amount,created_at,expires_at,updated_at)
                VALUES (?,'PAYOS','EXPIRED',120000,?,?,?)
                """, cancelledOrder, java.sql.Timestamp.from(now), java.sql.Timestamp.from(now.plusSeconds(3600)), java.sql.Timestamp.from(now));
        jdbc.update("""
                INSERT INTO order_items(order_id,product_id,product_name_vi,product_name_en,base_price,selling_price,quantity,total_price)
                VALUES (?,999,'Gốm thử','Test pottery',60000,60000,2,120000)
                """, paidOrder);
        metrics = (java.util.Map<?, ?>) reporting.businessAnalytics(30).get("metrics");
        assertThat(metrics.get("orders_created")).isEqualTo(2L);
        assertThat(metrics.get("completed_paid_revenue")).isEqualTo("120000");
        assertThat(metrics.get("average_order_value")).isEqualTo("120000");
        assertThat(metrics.get("sold_quantity")).isEqualTo(2L);
        mockMvc.perform(get("/api/v1/admin/analytics/business").with(authentication(auth(buyer))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/analytics/realtime").with(authentication(auth(buyer))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/analytics/historical?days=30").with(authentication(auth(buyer))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/analytics/business"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/analytics/business?days=30").with(authentication(auth(admin))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.metrics.completed_paid_revenue").value("120000"));
        mockMvc.perform(get("/api/v1/admin/analytics/business?days=0").with(authentication(auth(admin))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.metrics.workshop_bookings").value(2));
    }

    @Test void concurrentDedupCreatesOneNotificationAndOneRecipientSnapshot() throws Exception {
        user("dedup-admin", UserRole.ADMIN, UserStatus.ACTIVE);
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var calls = java.util.stream.IntStream.range(0, 2).mapToObj(i -> executor.submit(() -> {
                ready.countDown(); start.await();
                notificationService.create(NotificationType.PAYMENT_SUCCESS, "Paid", "Paid", "PAYMENT", 77L,
                        null, "PAYMENT_SUCCESS:PAYMENT:77", clock.now());
                return null;
            })).toList();
            ready.await(); start.countDown();
            for (var call : calls) call.get();
        }
        assertThat(jdbc.queryForObject("select count(*) from notifications", Long.class)).isOne();
        assertThat(jdbc.queryForObject("select count(*) from notification_recipients", Long.class)).isOne();
    }

    @Test void inventoryNotificationsOnlyFollowAvailabilityStateTransitions() {
        user("stock-admin", UserRole.ADMIN, UserStatus.ACTIVE);
        long product = product("stock", 10);
        inventory.adjust(product, InventoryTransactionType.ADJUSTMENT, -5, "enter low", null);
        inventory.adjust(product, InventoryTransactionType.ADJUSTMENT, -1, "remain low", null);
        inventory.adjust(product, InventoryTransactionType.ADJUSTMENT, -4, "enter out", null);
        assertThat(jdbc.queryForObject("select count(*) from notifications where type='LOW_STOCK'", Long.class)).isOne();
        assertThat(jdbc.queryForObject("select count(*) from notifications where type='OUT_OF_STOCK'", Long.class)).isOne();
    }

    @Test void completedSseConnectionCannotRollbackPersistentNotification() {
        User admin = user("sse-admin", UserRole.ADMIN, UserStatus.ACTIVE);
        emitters.connect(admin.getId(), clock.now()).complete();
        notificationService.create(NotificationType.NEW_ORDER, "New", "Persisted", "ORDER", 99L,
                null, "NEW_ORDER:ORDER:99", clock.now());
        assertThat(jdbc.queryForObject("select count(*) from notifications where dedup_key='NEW_ORDER:ORDER:99'", Long.class)).isOne();
    }

    @Test void adminNotificationAndReportingHttpContractsAreProtectedAndUsable() throws Exception {
        User admin = user("api-admin", UserRole.ADMIN, UserStatus.ACTIVE);
        User buyer = user("api-user", UserRole.USER, UserStatus.ACTIVE);
        notificationService.create(NotificationType.NEW_ORDER, "New", "API notification", "ORDER", 123L,
                null, "NEW_ORDER:ORDER:123", clock.now());
        Long notificationId = jdbc.queryForObject("select id from notifications where dedup_key='NEW_ORDER:ORDER:123'", Long.class);

        mockMvc.perform(get("/api/v1/admin/notifications").with(authentication(auth(buyer))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/notifications?isRead=false").with(authentication(auth(admin))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].isRead").value(false));
        mockMvc.perform(patch("/api/v1/admin/notifications/{id}/read", notificationId)
                        .with(authentication(auth(admin))).with(csrf().asHeader()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.isRead").value(true));
        mockMvc.perform(get("/api/v1/admin/dashboard").with(authentication(auth(admin))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalRevenue").value(0));
        mockMvc.perform(get("/api/v1/admin/reports/revenue")
                        .param("fromDate", "2026-08-01").param("toDate", "2026-08-31").param("groupBy", "DAY")
                        .with(authentication(auth(admin))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalRevenue").value(0));
    }

    @Test void customerNotificationsArePrivateAndReadPerCustomer() throws Exception {
        User buyer = user("customer-notify", UserRole.USER, UserStatus.ACTIVE);
        User other = user("customer-other", UserRole.USER, UserStatus.ACTIVE);
        User admin = user("customer-notify-admin", UserRole.ADMIN, UserStatus.ACTIVE);
        notificationService.create(NotificationType.NEW_ORDER, "New order", "Order ORD-20261022-00000042 was created.",
                "ORDER", 42L, null, "NEW_ORDER:ORDER:42", clock.now(), buyer.getId());
        Long notificationId = jdbc.queryForObject("select id from notifications where dedup_key='NEW_ORDER:ORDER:42'", Long.class);

        mockMvc.perform(get("/api/v1/me/notifications").with(authentication(auth(admin))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/me/notifications").with(authentication(auth(other))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
        mockMvc.perform(patch("/api/v1/me/notifications/{id}/read", notificationId)
                        .with(authentication(auth(other))).with(csrf().asHeader()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/me/notifications?isRead=false").with(authentication(auth(buyer))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        mockMvc.perform(patch("/api/v1/me/notifications/{id}/read", notificationId)
                        .with(authentication(auth(buyer))).with(csrf().asHeader()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.isRead").value(true));
        mockMvc.perform(get("/api/v1/me/notifications?isRead=false").with(authentication(auth(buyer))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test void dashboardRevenueAndBestSellingUseOnlyCompletedPaidOrders() {
        user("report-admin", UserRole.ADMIN, UserStatus.ACTIVE);
        long completedProduct = product("completed", 20);
        long newProduct = product("new-paid", 20);
        long pendingProduct = product("pending", 20);
        product("dashboard-low", 4);

        CheckoutResponse completed = checkout(user("buyer-completed", UserRole.USER, UserStatus.ACTIVE), completedProduct, 2);
        pay(completed); orderCommands.transition(completed.orderId(), OrderStatus.CONFIRMED);
        orderCommands.transition(completed.orderId(), OrderStatus.COMPLETED);

        CheckoutResponse refunded = checkout(user("buyer-refunded", UserRole.USER, UserStatus.ACTIVE), completedProduct, 1);
        pay(refunded); orderCommands.transition(refunded.orderId(), OrderStatus.CONFIRMED);
        orderCommands.transition(refunded.orderId(), OrderStatus.COMPLETED);
        jdbc.update("update payments set status='REFUNDED',refunded_at=now() where id=?", refunded.paymentId());

        CheckoutResponse newPaid = checkout(user("buyer-new", UserRole.USER, UserStatus.ACTIVE), newProduct, 3);
        pay(newPaid);
        checkout(user("buyer-pending", UserRole.USER, UserStatus.ACTIVE), pendingProduct, 4);

        LocalDate today = clock.businessNow().toLocalDate();
        var revenue = reporting.revenue(today, today, ReportGroupBy.DAY);
        assertThat(revenue.totalRevenue()).isEqualTo(1600);
        assertThat(revenue.points()).singleElement().satisfies(point -> assertThat(point.revenue()).isEqualTo(1600));
        assertThat(reporting.bestSelling(today, today, 10)).singleElement().satisfies(product -> {
            assertThat(product.productId()).isEqualTo(completedProduct);
            assertThat(product.soldQuantity()).isEqualTo(2);
        });

        var dashboard = reporting.dashboard();
        assertThat(dashboard.totalOrders()).isEqualTo(4);
        assertThat(dashboard.totalRevenue()).isEqualTo(1600);
        assertThat(dashboard.newOrders()).isOne();
        assertThat(dashboard.recentOrders()).hasSize(4);
        assertThat(dashboard.lowStockProducts()).extracting("productId").contains(productId("dashboard-low"));

        jdbc.update("update orders set completed_at=? where id=?", java.sql.Timestamp.from(
                Instant.parse("2026-07-31T17:00:00Z")), completed.orderId());
        assertThat(reporting.revenue(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 1), ReportGroupBy.DAY)
                .totalRevenue()).isEqualTo(1600);
        assertThat(reporting.revenue(LocalDate.of(2026, 7, 31), LocalDate.of(2026, 7, 31), ReportGroupBy.DAY)
                .totalRevenue()).isZero();
    }

    private CheckoutResponse checkout(User buyer, long productId, int quantity) {
        authenticate(buyer); cart.add(productId, quantity);
        return checkout.checkout(UUID.randomUUID().toString(), new CheckoutRequest("Receiver", "0901234567",
                buyer.getEmail(), "Address", null));
    }

    private void pay(CheckoutResponse response) {
        Payment payment = payments.findById(response.paymentId()).orElseThrow();
        paymentResults.processSuccess(new VerifiedPayOSEvent(response.orderId(), response.totalAmount(), "VND",
                "link-" + response.orderId(), "REF-" + response.paymentId(), payment.getCreatedAt(), true),
                payment.getCreatedAt());
    }

    private long product(String key, long stock) {
        var collection = catalog.createCollection(new CollectionRequest("C " + key, "C " + key, null, null,
                CollectionStatus.ACTIVE));
        long id = catalog.createProduct(new ProductRequest("P " + key, "P " + key, null, null, 1000L,
                collection.getId(), ProductStatus.ACTIVE)).getId();
        catalog.upsertDiscount(id, new DiscountRequest(DiscountType.FIXED_PRICE, new java.math.BigDecimal("800"),
                java.time.OffsetDateTime.parse("2020-01-01T00:00:00+07:00"),
                java.time.OffsetDateTime.parse("2099-01-01T00:00:00+07:00"), true));
        inventory.adjust(id, InventoryTransactionType.IMPORT, stock, "seed", null);
        return id;
    }

    private Long productId(String key) {
        return jdbc.queryForObject("select id from products where name_en=?", Long.class, "P " + key);
    }

    private User user(String key, UserRole role, UserStatus status) {
        Instant now = clock.now();
        return users.save(new User(key, key + "@example.com", key, null, role, status, now, now));
    }

    private void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(auth(user));
    }

    private UsernamePasswordAuthenticationToken auth(User user) {
        return new UsernamePasswordAuthenticationToken(new AuthenticatedUser(user.getId(), user.getEmail(),
                user.getRole().name()), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
    }
}
