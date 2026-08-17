package com.bautruc.ecommerce.order.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import com.bautruc.ecommerce.common.security.JwtAuthenticationFilter;
import com.bautruc.ecommerce.common.security.JwtTokenService;
import com.bautruc.ecommerce.identity.domain.User;
import com.bautruc.ecommerce.identity.domain.UserRole;
import com.bautruc.ecommerce.identity.domain.UserStatus;
import com.bautruc.ecommerce.identity.infrastructure.UserJpaRepository;
import com.bautruc.ecommerce.payment.application.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class OrderPaymentApiIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserJpaRepository users;
    @Autowired JwtTokenService tokens;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean PayOSClient payos;

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE TABLE collections, users CASCADE");
        when(payos.createPaymentRequest(any())).thenAnswer(invocation -> {
            PayOSCreatePaymentCommand command = invocation.getArgument(0);
            return new PayOSPaymentResult(command.orderId(), command.amount(), "link-" + command.orderId(), "PENDING", "https://pay.test/" + command.orderId(), "qr-" + command.orderId());
        });
    }

    @Test
    void checkoutIsLocallyPreparedOnceAndOwnerCanReadSnapshots() throws Exception {
        User user = save("buyer", UserRole.USER);
        Cookie access = access(user);
        long productId = stockedProduct(100_000, 5);
        addToCart(access, productId, 2);
        String key = UUID.randomUUID().toString();
        String request = checkoutRequest("Hanoi");

        mvc.perform(post("/api/v1/checkout")
                        .cookie(access).with(csrf().asHeader())
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentStatus").value("PENDING"));

        mvc.perform(post("/api/v1/checkout")
                        .cookie(access).with(csrf().asHeader())
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkoutUrl").exists());

        Long orderId = jdbc.queryForObject("select id from orders", Long.class);
        Long paymentId = jdbc.queryForObject("select id from payments", Long.class);
        mvc.perform(get("/api/v1/me/orders/{id}", orderId).cookie(access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("NEW"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.items[0].productNameVi").value("Bình gốm"))
                .andExpect(jsonPath("$.data.items[0].totalPrice").value(200_000))
                .andExpect(jsonPath("$.data.subtotal").value(200_000));
        mvc.perform(get("/api/v1/payments/{id}", paymentId).cookie(access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.amount").value(200_000));

        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("select count(*) from orders", Long.class)).isOne();
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("select count(*) from payments", Long.class)).isOne();
    }

    @Test
    void rejectsMissingKeyCsrfAndCrossUserReads() throws Exception {
        User owner = save("owner", UserRole.USER);
        User other = save("other", UserRole.USER);
        Cookie ownerAccess = access(owner);
        long productId = stockedProduct(10_000, 2);
        addToCart(ownerAccess, productId, 1);

        mvc.perform(post("/api/v1/checkout").cookie(ownerAccess)
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(checkoutRequest("Hue")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/checkout").cookie(ownerAccess).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON).content(checkoutRequest("Hue")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_KEY_REQUIRED"));

        prepare(ownerAccess, checkoutRequest("Hue"));
        Long orderId = jdbc.queryForObject("select id from orders", Long.class);
        Long paymentId = jdbc.queryForObject("select id from payments", Long.class);
        mvc.perform(get("/api/v1/me/orders/{id}", orderId).cookie(access(other)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/payments/{id}", paymentId).cookie(access(other)))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminTransitionRequiresPaidPaymentAndValidTarget() throws Exception {
        User admin = save("admin", UserRole.ADMIN);
        User owner = save("transition-owner", UserRole.USER);
        Cookie ownerAccess = access(owner);
        Cookie adminAccess = access(admin);
        long productId = stockedProduct(50_000, 3);
        addToCart(ownerAccess, productId, 1);
        prepare(ownerAccess, checkoutRequest("Saigon"));
        Long orderId = jdbc.queryForObject("select id from orders", Long.class);

        mvc.perform(patch("/api/v1/admin/orders/{id}/status", orderId)
                        .cookie(ownerAccess).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/admin/orders/{id}/status", orderId)
                        .cookie(adminAccess).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"NEW\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/admin/orders/{id}/status", orderId)
                        .cookie(adminAccess).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ORDER_NOT_PAID"));

        jdbc.update("update payments set status='PAID', paid_at=now(), updated_at=now() where order_id=?", orderId);
        mvc.perform(patch("/api/v1/admin/orders/{id}/status", orderId)
                        .cookie(adminAccess).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("CONFIRMED"));
        mvc.perform(patch("/api/v1/admin/orders/{id}/status", orderId)
                        .cookie(adminAccess).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("COMPLETED"));
    }

    @Test
    void adminOrderListFiltersPaymentStatusWithDatabasePagination() throws Exception {
        User admin = save("admin-order-list", UserRole.ADMIN);
        User owner = save("order-list-owner", UserRole.USER);
        long paidOrder = directOrder(owner.getId(), "BT-PAID-001", "PAID", 1000);
        directOrder(owner.getId(), "BT-PENDING-001", "PENDING", 2000);
        long paidOrderSecond = directOrder(owner.getId(), "BT-PAID-002", "PAID", 3000);

        mvc.perform(get("/api/v1/admin/orders")
                        .param("paymentStatus", "PAID")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "totalAmount,asc")
                        .cookie(access(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].id").value(paidOrder))
                .andExpect(jsonPath("$.data.content[0].paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.data.last").value(false));

        mvc.perform(get("/api/v1/admin/orders")
                        .param("paymentStatus", "PAID")
                        .param("page", "1")
                        .param("size", "1")
                        .param("sort", "totalAmount,asc")
                        .cookie(access(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].id").value(paidOrderSecond))
                .andExpect(jsonPath("$.data.content[0].paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.data.last").value(true));
    }

    private void addToCart(Cookie access, long productId, int quantity) throws Exception {
        mvc.perform(post("/api/v1/cart/items").cookie(access).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":%d,\"quantity\":%d}".formatted(productId, quantity)))
                .andExpect(status().isOk());
    }

    private void prepare(Cookie access, String request) throws Exception {
        mvc.perform(post("/api/v1/checkout").cookie(access).with(csrf().asHeader())
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk());
    }

    private long stockedProduct(long price, long quantity) {
        Long collectionId = jdbc.queryForObject("insert into collections(name_vi,name_en,status,created_at,updated_at) values('Gốm','Pottery','ACTIVE',now(),now()) returning id", Long.class);
        Long productId = jdbc.queryForObject("insert into products(name_vi,name_en,base_price,status,collection_id,created_at,updated_at) values('Bình gốm','Ceramic vase',?,'ACTIVE',?,now(),now()) returning id", Long.class, price, collectionId);
        jdbc.update("insert into inventories(product_id,quantity,reserved_quantity,low_stock_threshold,updated_at) values(?,?,0,5,now())", productId, quantity);
        return productId;
    }

    private long directOrder(long userId, String code, String paymentStatus, long amount) {
        Long orderId = jdbc.queryForObject("select nextval('app_global_id_seq')", Long.class);
        jdbc.update("""
                insert into orders(id,order_code,user_id,receiver_name,phone,email,address,total_amount,status,created_at,updated_at)
                values(?,?,?,?,?,?,?,?,?,now(),now())
                """, orderId, code, userId, "Nguyen Van A", "0909000000", code.toLowerCase() + "@example.com", "Bau Truc", amount, "NEW");
        jdbc.update("""
                insert into payments(order_id,provider,status,amount,created_at,expires_at,updated_at)
                values(?,'PAYOS',?,?,now(),now() + interval '15 minutes',now())
                """, orderId, paymentStatus, amount);
        return orderId;
    }

    private String checkoutRequest(String address) throws Exception {
        JsonNode node = json.readTree("{\"receiverName\":\"Nguyen Van A\",\"phone\":\"0901234567\",\"address\":\"x\"}");
        return ((com.fasterxml.jackson.databind.node.ObjectNode) node).put("address", address).toString();
    }

    private User save(String key, UserRole role) {
        Instant now = Instant.now();
        return users.save(new User(key, key + "@example.com", key, null, role, UserStatus.ACTIVE, now, now));
    }

    private Cookie access(User user) {
        return new Cookie(JwtAuthenticationFilter.ACCESS_COOKIE_NAME, tokens.createAccessToken(user));
    }
}
