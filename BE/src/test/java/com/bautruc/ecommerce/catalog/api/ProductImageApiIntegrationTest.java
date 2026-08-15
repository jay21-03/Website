package com.bautruc.ecommerce.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
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
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.bautruc.ecommerce.catalog.application.ObjectStoragePort;
import com.bautruc.ecommerce.catalog.application.ProductImageService;
import com.bautruc.ecommerce.common.security.JwtAuthenticationFilter;
import com.bautruc.ecommerce.common.security.JwtTokenService;
import com.bautruc.ecommerce.identity.domain.User;
import com.bautruc.ecommerce.identity.domain.UserRole;
import com.bautruc.ecommerce.identity.domain.UserStatus;
import com.bautruc.ecommerce.identity.infrastructure.UserJpaRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class ProductImageApiIntegrationTest {
    @Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserJpaRepository users;
    @Autowired JwtTokenService tokens;
    @Autowired ProductImageService imageService;
    @MockitoBean ObjectStoragePort storage;

    @BeforeEach void clean() {
        jdbc.execute("TRUNCATE TABLE collections, users CASCADE");
        reset(storage);
        when(storage.publicUrl(anyString())).thenAnswer(invocation -> "https://cdn.example/" + invocation.getArgument(0));
    }

    @Test void adminUploadsReordersSelectsThumbnailAndDeletesImages() throws Exception {
        long productId = insertProduct();
        Cookie admin = access(saveAdmin("image-admin"));

        long first = upload(productId, admin, "first.png");
        long second = upload(productId, admin, "second.png");

        mvc.perform(put("/api/v1/admin/products/{productId}/images/{imageId}/thumbnail", productId, second)
                        .cookie(admin).with(csrf().asHeader()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.thumbnail").value(true));

        mvc.perform(put("/api/v1/admin/products/{productId}/images/order", productId)
                        .cookie(admin).with(csrf().asHeader()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imageIds\":[%d,%d]}".formatted(second, first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(second))
                .andExpect(jsonPath("$.data[1].id").value(first));

        mvc.perform(get("/api/v1/products/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.thumbnailUrl").value(org.hamcrest.Matchers.containsString("/products/")))
                .andExpect(jsonPath("$.data.images[0].id").value(second));

        mvc.perform(delete("/api/v1/admin/products/{productId}/images/{imageId}", productId, second)
                        .cookie(admin).with(csrf().asHeader()))
                .andExpect(status().isOk());

        assertThat(jdbc.queryForObject("select count(*) from product_images where product_id=?", Long.class, productId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select is_thumbnail from product_images where id=?", Boolean.class, first)).isTrue();
        verify(storage, atLeastOnce()).delete(anyString());
    }

    @Test void concurrentUploadsCannotExceedTenImages() throws Exception {
        long productId = insertProduct();
        for (int index = 0; index < 9; index++) {
            jdbc.update("insert into product_images(product_id,object_key,content_type,file_size_bytes,sort_order,is_thumbnail,created_at) values(?,?,?,?,?,?,now())",
                    productId, "products/" + productId + "/existing-" + index + ".png", "image/png", 8, index, index == 0);
        }
        CyclicBarrier bothUploaded = new CyclicBarrier(2);
        doAnswer(invocation -> { bothUploaded.await(); return null; }).when(storage)
                .put(anyString(), org.mockito.ArgumentMatchers.any(byte[].class), anyString());
        MockMultipartFile file = png("concurrent.png");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> uploadDirect(productId, file));
            Future<Boolean> second = executor.submit(() -> uploadDirect(productId, file));
            assertThat(java.util.List.of(first.get(), second.get()).stream().filter(Boolean::booleanValue).count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
        assertThat(jdbc.queryForObject("select count(*) from product_images where product_id=?", Long.class, productId)).isEqualTo(10);
        verify(storage, atLeastOnce()).delete(anyString());
    }

    private boolean uploadDirect(long productId, MockMultipartFile file) {
        try { imageService.upload(productId, file); return true; }
        catch (RuntimeException expected) { return false; }
    }

    private long upload(long productId, Cookie admin, String name) throws Exception {
        MvcResult result = mvc.perform(multipart("/api/v1/admin/products/{productId}/images", productId)
                        .file(png(name)).cookie(admin).with(csrf().asHeader()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.startsWith("https://cdn.example/products/")))
                .andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        return body.path("data").path("id").asLong();
    }

    private MockMultipartFile png(String name) {
        return new MockMultipartFile("file", name, "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
    }

    private long insertProduct() {
        long collection = jdbc.queryForObject("insert into collections(name_vi,name_en,status,created_at,updated_at) values('Gom','Pottery','ACTIVE',now(),now()) returning id", Long.class);
        return jdbc.queryForObject("insert into products(name_vi,name_en,base_price,status,collection_id,created_at,updated_at) values('Binh','Vase',100000,'ACTIVE',?,now(),now()) returning id", Long.class, collection);
    }

    private User saveAdmin(String subject) {
        Instant now = Instant.now();
        return users.save(new User(subject, subject + "@example.com", "Admin", null, UserRole.ADMIN, UserStatus.ACTIVE, now, now));
    }

    private Cookie access(User user) {
        return new Cookie(JwtAuthenticationFilter.ACCESS_COOKIE_NAME, tokens.createAccessToken(user));
    }
}
