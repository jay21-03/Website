package com.bautruc.ecommerce.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import com.bautruc.ecommerce.catalog.application.ObjectStoragePort;
import com.bautruc.ecommerce.common.security.JwtAuthenticationFilter;
import com.bautruc.ecommerce.common.security.JwtTokenService;
import com.bautruc.ecommerce.identity.domain.User;
import com.bautruc.ecommerce.identity.domain.UserRole;
import com.bautruc.ecommerce.identity.domain.UserStatus;
import com.bautruc.ecommerce.identity.infrastructure.UserJpaRepository;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class CatalogProductQueryIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    UserJpaRepository users;

    @Autowired
    JwtTokenService tokens;

    @MockitoBean
    ObjectStoragePort storage;

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE TABLE collections, users CASCADE");
        when(storage.publicUrl(anyString())).thenAnswer(invocation -> "https://cdn.example/" + invocation.getArgument(0));
    }

    @Test
    void publicProductsSupportKeywordCollectionPriceRangeAndSellingPriceSort() throws Exception {
        long pottery = collection("Gom", "Pottery");
        long other = collection("Khac", "Other");
        long bowl = product("Chen gom", "Pottery bowl", 1000, "ACTIVE", pottery);
        long vase = product("Binh gom", "Pottery vase", 2000, "ACTIVE", pottery);
        product("Binh an", "Hidden vase", 900, "INACTIVE", pottery);
        product("Binh ngoai", "Other collection vase", 900, "ACTIVE", other);
        discount(bowl, "PERCENTAGE", "10.00");
        discount(vase, "FIXED_PRICE", "1500");

        mvc.perform(get("/api/v1/products")
                        .param("keyword", "gom")
                        .param("collectionId", Long.toString(pottery))
                        .param("minPrice", "800")
                        .param("maxPrice", "1200")
                        .param("sort", "sellingPrice,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(bowl))
                .andExpect(jsonPath("$.data.content[0].sellingPrice").value(900));
    }

    @Test
    void databaseSellingPriceSortMatchesPricingServiceHalfUpSemantics() throws Exception {
        long collection = collection("Gia", "Pricing");
        long roundsDown = product("Gia A", "Price A", 100, "ACTIVE", collection);
        long roundsHalfUp = product("Gia B", "Price B", 100, "ACTIVE", collection);
        long roundsUp = product("Gia C", "Price C", 100, "ACTIVE", collection);
        discount(roundsDown, "PERCENTAGE", "33.51");
        discount(roundsHalfUp, "PERCENTAGE", "33.50");
        discount(roundsUp, "PERCENTAGE", "33.49");

        mvc.perform(get("/api/v1/products")
                        .param("keyword", "Gia")
                        .param("sort", "sellingPrice,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(roundsDown))
                .andExpect(jsonPath("$.data.content[0].sellingPrice").value(66))
                .andExpect(jsonPath("$.data.content[1].sellingPrice").value(67))
                .andExpect(jsonPath("$.data.content[2].sellingPrice").value(67));
    }

    @Test
    void invalidSortReturnsValidationFailed() throws Exception {
        mvc.perform(get("/api/v1/products").param("sort", "basePrice,desc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void productBasePriceUpdateRejectsExistingInvalidFixedPriceDiscountAtomically() throws Exception {
        long collection = collection("Cap nhat", "Update");
        long product = product("Binh", "Vase", 1000, "ACTIVE", collection);
        discount(product, "FIXED_PRICE", "900");
        Cookie admin = access(saveAdmin());

        mvc.perform(put("/api/v1/admin/products/{id}", product)
                        .cookie(admin)
                        .with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nameVi": "Binh",
                                  "nameEn": "Vase",
                                  "descriptionVi": "Mo ta",
                                  "descriptionEn": "Description",
                                  "basePrice": 800,
                                  "collectionId": %d,
                                  "status": "ACTIVE"
                                }
                                """.formatted(collection)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("DISCOUNT_INVALID"));

        assertThat(jdbc.queryForObject("SELECT base_price FROM products WHERE id = ?", Long.class, product))
                .isEqualTo(1000);
    }

    private long collection(String nameVi, String nameEn) {
        return jdbc.queryForObject("""
                INSERT INTO collections(name_vi,name_en,status,created_at,updated_at)
                VALUES(?,?,'ACTIVE',now(),now())
                RETURNING id
                """, Long.class, nameVi, nameEn);
    }

    private long product(String nameVi, String nameEn, long basePrice, String status, long collectionId) {
        return jdbc.queryForObject("""
                INSERT INTO products(name_vi,name_en,base_price,status,collection_id,created_at,updated_at)
                VALUES(?,?,?,?,?,now(),now())
                RETURNING id
                """, Long.class, nameVi, nameEn, basePrice, status, collectionId);
    }

    private void discount(long productId, String type, String value) {
        jdbc.update("""
                INSERT INTO discounts(product_id,discount_type,discount_value,start_at,end_at,is_active,created_at,updated_at)
                VALUES(?,?,?,now() - interval '1 hour',now() + interval '1 hour',true,now(),now())
                """, productId, type, new java.math.BigDecimal(value));
    }

    private User saveAdmin() {
        Instant now = Instant.now();
        return users.save(new User("catalog-query-admin", "catalog-query-admin@example.com", "Admin", null,
                UserRole.ADMIN, UserStatus.ACTIVE, now, now));
    }

    private Cookie access(User user) {
        return new Cookie(JwtAuthenticationFilter.ACCESS_COOKIE_NAME, tokens.createAccessToken(user));
    }
}
