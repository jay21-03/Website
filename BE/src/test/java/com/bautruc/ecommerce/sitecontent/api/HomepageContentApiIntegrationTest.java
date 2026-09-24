package com.bautruc.ecommerce.sitecontent.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import com.bautruc.ecommerce.common.storage.ObjectStoragePort;
import com.bautruc.ecommerce.sitecontent.application.HomepageContentService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class HomepageContentApiIntegrationTest {
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
        jdbc.execute("TRUNCATE TABLE homepage_featured_products, product_images, products, collections, users CASCADE");
        jdbc.update("UPDATE site_media SET object_key = NULL, content_type = NULL, file_size_bytes = NULL, updated_at = now()");
        jdbc.update("UPDATE homepage_settings SET slogan_vi = ?, slogan_en = ?, updated_at = now() WHERE id = 1",
                HomepageContentService.DEFAULT_SLOGAN_VI, HomepageContentService.DEFAULT_SLOGAN_EN);
        when(storage.publicUrl(anyString())).thenAnswer(invocation -> "https://cdn.example/" + invocation.getArgument(0));
    }

    @Test
    void publicHomeIsAnonymousAndReturnsSeededMediaSlots() throws Exception {
        mvc.perform(get("/api/v1/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.socialImages.length()").value(6))
                .andExpect(jsonPath("$.data.sloganVi").value(HomepageContentService.DEFAULT_SLOGAN_VI))
                .andExpect(jsonPath("$.data.sloganEn").value(HomepageContentService.DEFAULT_SLOGAN_EN))
                .andExpect(jsonPath("$.data.featuredProducts.length()").value(0));
    }

    @Test
    void adminHomeRequiresAdminRole() throws Exception {
        mvc.perform(get("/api/v1/admin/home"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/v1/admin/home").cookie(access(saveUser())))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/admin/home").cookie(access(saveAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.media.length()").value(8));
    }

    @Test
    void featuredWriteRequiresCsrf() throws Exception {
        Cookie admin = access(saveAdmin());
        long collection = collection();
        long first = product("One", "ACTIVE", collection);
        long second = product("Two", "ACTIVE", collection);
        long third = product("Three", "ACTIVE", collection);

        mvc.perform(put("/api/v1/admin/home/featured-products")
                        .cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(first, second, third)))
                .andExpect(status().isForbidden());
    }

    @Test
    void sloganWriteRequiresAdminAndCsrf() throws Exception {
        String body = "{\"sloganVi\":\"Slogan mới\",\"sloganEn\":\"New slogan\"}";

        mvc.perform(put("/api/v1/admin/home/slogan")
                        .with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        mvc.perform(put("/api/v1/admin/home/slogan")
                        .cookie(access(saveUser()))
                        .with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/v1/admin/home/slogan")
                        .cookie(access(saveAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanUpdateSloganAndPublicHomeReturnsPersistedValues() throws Exception {
        Cookie admin = access(saveAdmin());

        mvc.perform(put("/api/v1/admin/home/slogan")
                        .cookie(admin)
                        .with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sloganVi\":\"  Đất kể chuyện  \",\"sloganEn\":\"  Clay tells stories  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sloganVi").value("Đất kể chuyện"))
                .andExpect(jsonPath("$.data.sloganEn").value("Clay tells stories"));

        mvc.perform(get("/api/v1/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sloganVi").value("Đất kể chuyện"))
                .andExpect(jsonPath("$.data.sloganEn").value("Clay tells stories"));
    }

    @Test
    void sloganValidationRejectsBlankOrOversizedValues() throws Exception {
        Cookie admin = access(saveAdmin());
        String oversized = "x".repeat(201);

        mvc.perform(put("/api/v1/admin/home/slogan")
                        .cookie(admin)
                        .with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sloganVi\":\"   \",\"sloganEn\":\"English\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/v1/admin/home/slogan")
                        .cookie(admin)
                        .with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sloganVi\":\"Việt\",\"sloganEn\":\"" + oversized + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCanConfigureThreeActiveProductsAndPublicHomePreservesSlotOrder() throws Exception {
        Cookie admin = access(saveAdmin());
        long collection = collection();
        long first = product("One", "ACTIVE", collection);
        long second = product("Two", "ACTIVE", collection);
        long third = product("Three", "ACTIVE", collection);

        mvc.perform(put("/api/v1/admin/home/featured-products")
                        .cookie(admin)
                        .with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(third, first, second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.featuredProducts[0].slot").value(1))
                .andExpect(jsonPath("$.data.featuredProducts[0].id").value(third))
                .andExpect(jsonPath("$.data.featuredProducts[1].id").value(first))
                .andExpect(jsonPath("$.data.featuredProducts[2].id").value(second));

        mvc.perform(get("/api/v1/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.featuredProducts[0].id").value(third))
                .andExpect(jsonPath("$.data.featuredProducts[1].id").value(first))
                .andExpect(jsonPath("$.data.featuredProducts[2].id").value(second));
    }

    @Test
    void duplicateInactiveAndDeletedProductsAreRejected() throws Exception {
        Cookie admin = access(saveAdmin());
        long collection = collection();
        long activeOne = product("One", "ACTIVE", collection);
        long activeTwo = product("Two", "ACTIVE", collection);
        long inactive = product("Inactive", "INACTIVE", collection);
        long deleted = product("Deleted", "ACTIVE", collection);
        jdbc.update("UPDATE products SET deleted_at = now(), status = 'INACTIVE' WHERE id = ?", deleted);

        mvc.perform(put("/api/v1/admin/home/featured-products")
                        .cookie(admin)
                        .with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(activeOne, activeOne, activeTwo)))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/v1/admin/home/featured-products")
                        .cookie(admin)
                        .with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(activeOne, activeTwo, inactive)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("HOMEPAGE_FEATURED_PRODUCTS_INVALID"));

        mvc.perform(put("/api/v1/admin/home/featured-products")
                        .cookie(admin)
                        .with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(activeOne, activeTwo, deleted)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("HOMEPAGE_FEATURED_PRODUCTS_INVALID"));
    }


    @Test
    void mediaWriteRequiresAdminAndCsrf() throws Exception {
        MockMultipartFile file = png("file", "hero.png");

        mvc.perform(multipart("/api/v1/admin/home/media/HOME_HERO")
                        .file(file)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .with(csrf().asHeader()))
                .andExpect(status().isUnauthorized());

        mvc.perform(multipart("/api/v1/admin/home/media/HOME_HERO")
                        .file(png("file", "hero.png"))
                        .cookie(access(saveUser()))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .with(csrf().asHeader()))
                .andExpect(status().isForbidden());

        mvc.perform(multipart("/api/v1/admin/home/media/HOME_HERO")
                        .file(png("file", "hero.png"))
                        .cookie(access(saveAdmin()))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanUploadHomepageMediaAndPublicHomeUsesCurrentCdnUrl() throws Exception {
        Cookie admin = access(saveAdmin());

        mvc.perform(multipart("/api/v1/admin/home/media/HOME_HERO")
                        .file(png("file", "hero.png"))
                        .cookie(admin)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .with(csrf().asHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slot").value("HOME_HERO"))
                .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.startsWith(
                        "https://cdn.example/site/home/hero/"
                )));

        verify(storage).put(
                startsWith("site/home/hero/"),
                any(byte[].class),
                eq("image/png")
        );

        mvc.perform(get("/api/v1/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.heroImageUrl").value(org.hamcrest.Matchers.startsWith(
                        "https://cdn.example/site/home/hero/"
                )));
    }

    @Test
    void adminCanClearHomepageMediaAndPublicHomeFallsBackToNull() throws Exception {
        Cookie admin = access(saveAdmin());
        String oldKey = "site/home/story/existing.jpg";
        jdbc.update("""
                UPDATE site_media
                SET object_key = ?, content_type = 'image/jpeg', file_size_bytes = 100, updated_at = now()
                WHERE slot = 'HOME_STORY'
                """, oldKey);

        mvc.perform(delete("/api/v1/admin/home/media/HOME_STORY")
                        .cookie(admin)
                        .with(csrf().asHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slot").value("HOME_STORY"))
                .andExpect(jsonPath("$.data.url").doesNotExist());

        verify(storage).delete(oldKey);

        mvc.perform(get("/api/v1/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storyImageUrl").doesNotExist());
    }

    @Test
    void homepageMediaRejectsDeclaredTypeThatDoesNotMatchContent() throws Exception {
        Cookie admin = access(saveAdmin());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hero.jpg",
                "image/jpeg",
                new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a}
        );

        mvc.perform(multipart("/api/v1/admin/home/media/HOME_HERO")
                        .file(file)
                        .cookie(admin)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .with(csrf().asHeader()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("HOMEPAGE_MEDIA_TYPE_UNSUPPORTED"));
    }

    @Test
    void homepageUsesCurrentProductThumbnailInsteadOfStoredHomepageImage() throws Exception {
        Cookie admin = access(saveAdmin());
        long collection = collection();
        long first = product("One", "ACTIVE", collection);
        long second = product("Two", "ACTIVE", collection);
        long third = product("Three", "ACTIVE", collection);

        long oldImage = image(first, "products/" + first + "/old.jpg", 0, true);
        long newImage = image(first, "products/" + first + "/new.jpg", 1, false);

        mvc.perform(put("/api/v1/admin/home/featured-products")
                        .cookie(admin)
                        .with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(first, second, third)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.featuredProducts[0].thumbnailUrl")
                        .value("https://cdn.example/products/" + first + "/old.jpg"));

        jdbc.update("UPDATE product_images SET is_thumbnail = false WHERE id = ?", oldImage);
        jdbc.update("UPDATE product_images SET is_thumbnail = true WHERE id = ?", newImage);

        mvc.perform(get("/api/v1/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.featuredProducts[0].thumbnailUrl")
                        .value("https://cdn.example/products/" + first + "/new.jpg"));
    }


    private MockMultipartFile png(String partName, String filename) {
        return new MockMultipartFile(
                partName,
                filename,
                "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a}
        );
    }

    private long collection() {
        return jdbc.queryForObject("""
                INSERT INTO collections(name_vi,name_en,status,created_at,updated_at)
                VALUES('Trang chu','Homepage','ACTIVE',now(),now())
                RETURNING id
                """, Long.class);
    }

    private long product(String name, String status, long collectionId) {
        return jdbc.queryForObject("""
                INSERT INTO products(name_vi,name_en,description_vi,description_en,base_price,status,collection_id,created_at,updated_at)
                VALUES(?,?,?,?,100000,?,?,now(),now())
                RETURNING id
                """, Long.class, name, name, "Mo ta " + name, "Description " + name, status, collectionId);
    }

    private long image(long productId, String key, int sortOrder, boolean thumbnail) {
        return jdbc.queryForObject("""
                INSERT INTO product_images(product_id,object_key,content_type,file_size_bytes,sort_order,is_thumbnail,created_at)
                VALUES(?,?,'image/jpeg',100,?,?,now())
                RETURNING id
                """, Long.class, productId, key, sortOrder, thumbnail);
    }

    private String body(long first, long second, long third) {
        return "{\"productIds\":[" + first + "," + second + "," + third + "]}";
    }

    private User saveAdmin() {
        Instant now = Instant.now();
        return users.save(new User("homepage-admin", "homepage-admin@example.com", "Admin", null,
                UserRole.ADMIN, UserStatus.ACTIVE, now, now));
    }

    private User saveUser() {
        Instant now = Instant.now();
        return users.save(new User("homepage-user", "homepage-user@example.com", "User", null,
                UserRole.USER, UserStatus.ACTIVE, now, now));
    }

    private Cookie access(User user) {
        return new Cookie(JwtAuthenticationFilter.ACCESS_COOKIE_NAME, tokens.createAccessToken(user));
    }
}
