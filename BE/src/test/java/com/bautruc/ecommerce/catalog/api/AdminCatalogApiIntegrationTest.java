package com.bautruc.ecommerce.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class AdminCatalogApiIntegrationTest {
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
    void adminProductListAndDetailExcludeSoftDeletedProducts() throws Exception {
        Cookie admin = access(saveAdmin());
        long collection = collection("Admin", "Admin");
        long active = product("San pham", "Product", 1000, "ACTIVE", collection);
        long inactive = product("Ngung ban", "Inactive", 2000, "INACTIVE", collection);
        long deleted = product("Da xoa", "Deleted", 3000, "ACTIVE", collection);
        jdbc.update("UPDATE products SET deleted_at = now() WHERE id = ?", deleted);

        mvc.perform(get("/api/v1/admin/products").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));

        mvc.perform(get("/api/v1/admin/products/{id}", inactive).cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(inactive))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        mvc.perform(get("/api/v1/admin/products/{id}", deleted).cookie(admin))
                .andExpect(status().isNotFound());

        assertThat(active).isPositive();
    }

    @Test
    void adminCanPatchProductAndCollectionStatus() throws Exception {
        Cookie admin = access(saveAdmin());
        long collection = collection("Bo suu tap", "Collection");
        long product = product("San pham", "Product", 1000, "ACTIVE", collection);

        mvc.perform(patch("/api/v1/admin/products/{id}/status", product)
                        .cookie(admin)
                        .with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        mvc.perform(patch("/api/v1/admin/collections/{id}/status", collection)
                        .cookie(admin)
                        .with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    void adminCollectionsListAndDetailExcludeSoftDeletedCollections() throws Exception {
        Cookie admin = access(saveAdmin());
        long active = collection("Dang hien", "Active");
        long deleted = collection("Da xoa", "Deleted");
        jdbc.update("UPDATE collections SET deleted_at = now() WHERE id = ?", deleted);

        mvc.perform(get("/api/v1/admin/collections").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(active));

        mvc.perform(get("/api/v1/admin/collections/{id}", active).cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(active));

        mvc.perform(get("/api/v1/admin/collections/{id}", deleted).cookie(admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCanToggleDiscountActiveState() throws Exception {
        Cookie admin = access(saveAdmin());
        long collection = collection("Giam gia", "Discount");
        long product = product("Binh", "Vase", 1000, "ACTIVE", collection);
        discount(product, "PERCENTAGE", "10.00");

        mvc.perform(patch("/api/v1/admin/products/{id}/discount/active", product)
                        .cookie(admin)
                        .with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isActive\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(false));
    }

    @Test
    void adminProductQuerySupportsKeywordStatusCollectionPaginationSortingAndDiscountConfig() throws Exception {
        Cookie admin = access(saveAdmin());
        long firstCollection = collection("Binh", "Vase");
        long secondCollection = collection("Tuong", "Statue");
        long activeLow = product("Binh nho", "Small vase", 1000, "ACTIVE", firstCollection);
        long inactiveHigh = product("Binh lon", "Large vase", 3000, "INACTIVE", firstCollection);
        product("Tuong Cham", "Cham statue", 2000, "ACTIVE", secondCollection);
        discount(inactiveHigh, "FIXED_PRICE", "2500");

        mvc.perform(get("/api/v1/admin/products")
                        .param("keyword", "binh")
                        .param("collectionId", String.valueOf(firstCollection))
                        .param("sort", "basePrice,desc")
                        .param("page", "0")
                        .param("size", "1")
                        .cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].id").value(inactiveHigh))
                .andExpect(jsonPath("$.data.content[0].status").value("INACTIVE"))
                .andExpect(jsonPath("$.data.content[0].discount.discountType").value("FIXED_PRICE"))
                .andExpect(jsonPath("$.data.last").value(false));

        mvc.perform(get("/api/v1/admin/products")
                        .param("status", "ACTIVE")
                        .param("collectionId", String.valueOf(firstCollection))
                        .cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(activeLow));

        mvc.perform(get("/api/v1/products")
                        .param("keyword", "binh")
                        .cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(activeLow));
    }

    @Test
    void adminCollectionQuerySupportsKeywordStatusPaginationSortingAndDelete() throws Exception {
        Cookie admin = access(saveAdmin());
        long inactive = collection("Bo suu tap an", "Hidden collection");
        long active = collection("Bo suu tap mo", "Open collection");
        jdbc.update("UPDATE collections SET status = 'INACTIVE' WHERE id = ?", inactive);

        mvc.perform(get("/api/v1/admin/collections")
                        .param("keyword", "bo suu tap")
                        .param("status", "INACTIVE")
                        .param("sort", "nameVi,asc")
                        .param("page", "0")
                        .param("size", "1")
                        .cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(inactive))
                .andExpect(jsonPath("$.data.content[0].status").value("INACTIVE"));

        mvc.perform(get("/api/v1/collections").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(active));

        mvc.perform(delete("/api/v1/admin/collections/{id}", inactive)
                        .cookie(admin)
                        .with(csrf().asHeader()))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/admin/collections/{id}", inactive).cookie(admin))
                .andExpect(status().isNotFound());
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
        return users.save(new User("admin-catalog", "admin-catalog@example.com", "Admin", null,
                UserRole.ADMIN, UserStatus.ACTIVE, now, now));
    }

    private Cookie access(User user) {
        return new Cookie(JwtAuthenticationFilter.ACCESS_COOKIE_NAME, tokens.createAccessToken(user));
    }
}
