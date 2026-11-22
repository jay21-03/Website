package com.bautruc.ecommerce.seo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.bautruc.ecommerce.common.security.JwtAuthenticationFilter;
import com.bautruc.ecommerce.common.security.JwtTokenService;
import com.bautruc.ecommerce.common.storage.ObjectStoragePort;
import com.bautruc.ecommerce.identity.domain.User;
import com.bautruc.ecommerce.identity.domain.UserRole;
import com.bautruc.ecommerce.identity.domain.UserStatus;
import com.bautruc.ecommerce.identity.infrastructure.UserJpaRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
class SeoContentIntegrationTest {
    @Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired SeoPageController seoPages;
    @Autowired UserJpaRepository users;
    @Autowired JwtTokenService tokens;
    @MockitoBean ObjectStoragePort storage;

    @BeforeEach void clean() { jdbc.execute("TRUNCATE blog_posts, faq_items, users CASCADE"); }

    @Test void blogDraftIsPrivateAndPublishedPostAppearsInSitemap() throws Exception {
        Cookie admin = admin();
        String draft = """
                {"slug":"test-pottery-story","title":"Pottery story","bodyMarkdown":"# Pottery story\\nA real story.","status":"DRAFT"}
                """;
        mvc.perform(post("/api/v1/admin/blog").with(csrf().asHeader()).contentType(MediaType.APPLICATION_JSON).content(draft))
                .andExpect(status().isUnauthorized());
        var result = mvc.perform(post("/api/v1/admin/blog").cookie(admin).with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(draft)).andExpect(status().isOk()).andReturn();
        long id = ((Number) com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.data.id")).longValue();
        mvc.perform(get("/api/v1/blog/test-pottery-story")).andExpect(status().isNotFound());
        mvc.perform(get("/sitemap.xml")).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("test-pottery-story"))));
        mvc.perform(put("/api/v1/admin/blog/{id}", id).cookie(admin).with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(draft.replace("DRAFT", "PUBLISHED")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/blog/test-pottery-story")).andExpect(status().isOk());
        mvc.perform(get("/sitemap.xml")).andExpect(content().string(org.hamcrest.Matchers.containsString("/blog/test-pottery-story")));
        org.springframework.test.util.ReflectionTestUtils.setField(seoPages, "shell",
                "<!doctype html><html><head><title>old</title><meta name=\"robots\" content=\"noindex\" /></head><body><div id=\"root\"></div></body></html>");
        org.springframework.test.util.ReflectionTestUtils.setField(seoPages, "shellExpires", Instant.now().plusSeconds(60));
        mvc.perform(get("/blog/test-pottery-story"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<title>Pottery story</title>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/blog/test-pottery-story")));
    }

    @Test void futureArticleCannotPublishBeforeDecember() throws Exception {
        Cookie admin = admin();
        mvc.perform(post("/api/v1/admin/blog").cookie(admin).with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"slug":"mua-gom-bau-truc-lam-qua-tet","title":"Tet pottery","bodyMarkdown":"# Tet pottery","status":"PUBLISHED"}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test void scheduledBlogIsPrivateUntilItsPublicationTime() throws Exception {
        Cookie admin = admin();
        String future = Instant.now().plusSeconds(3600).toString();
        String input = "{\"slug\":\"scheduled-story\",\"title\":\"Scheduled story\",\"bodyMarkdown\":\"# Scheduled story\",\"status\":\"SCHEDULED\",\"publishAt\":\"" + future + "\"}";
        var result = mvc.perform(post("/api/v1/admin/blog").cookie(admin).with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(input)).andExpect(status().isOk()).andReturn();
        long id = ((Number) com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.data.id")).longValue();
        mvc.perform(get("/api/v1/blog/scheduled-story")).andExpect(status().isNotFound());
        mvc.perform(get("/sitemap.xml")).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("scheduled-story"))));
        jdbc.update("UPDATE blog_posts SET publish_at = now() - interval '1 second' WHERE id = ?", id);
        mvc.perform(get("/api/v1/blog/scheduled-story")).andExpect(status().isOk());
        mvc.perform(get("/sitemap.xml")).andExpect(content().string(org.hamcrest.Matchers.containsString("/blog/scheduled-story")));
    }

    @Test void adminMetadataSaveAppearsInPublicHtmlWithoutBuild() throws Exception {
        String payload = """
                {"path":"/faq","title":"Updated FAQ title","description":"Updated FAQ description"}
                """;
        mvc.perform(put("/api/v1/admin/seo/metadata").with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/admin/seo/metadata").cookie(admin()).with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/seo/metadata"))
                .andExpect(jsonPath("$.data['/faq'].title").value("Updated FAQ title"));
        org.springframework.test.util.ReflectionTestUtils.setField(seoPages, "shell",
                "<!doctype html><html><head><title>old</title><meta name=\"robots\" content=\"noindex\" /></head><body><div id=\"root\"></div></body></html>");
        org.springframework.test.util.ReflectionTestUtils.setField(seoPages, "shellExpires", Instant.now().plusSeconds(60));
        mvc.perform(get("/faq"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<title>Updated FAQ title</title>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("content=\"Updated FAQ description\"")));
    }

    @Test void productHtmlRedirectAndSitemapMatchPublicationStatus() throws Exception {
        long collection = jdbc.queryForObject("""
                INSERT INTO collections(name_vi,name_en,slug,status,created_at,updated_at)
                VALUES ('Test collection','Test collection','test-seo-collection','ACTIVE',now(),now()) RETURNING id
                """, Long.class);
        long active = jdbc.queryForObject("""
                INSERT INTO products(name_vi,name_en,slug,base_price,status,collection_id,created_at,updated_at)
                VALUES ('Test pottery','Test pottery','test-pottery-product',120000,'ACTIVE',?,now(),now()) RETURNING id
                """, Long.class, collection);
        jdbc.update("""
                INSERT INTO products(name_vi,name_en,slug,base_price,status,collection_id,created_at,updated_at)
                VALUES ('Hidden pottery','Hidden pottery','hidden-pottery-product',120000,'INACTIVE',?,now(),now())
                """, collection);
        org.springframework.test.util.ReflectionTestUtils.setField(seoPages, "shell",
                "<!doctype html><html><head><title>old</title><meta name=\"robots\" content=\"noindex\" /></head><body><div id=\"root\"></div></body></html>");
        org.springframework.test.util.ReflectionTestUtils.setField(seoPages, "shellExpires", Instant.now().plusSeconds(60));
        mvc.perform(get("/products/{id}", active)).andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "https://dangxembautruc.com/san-pham/test-pottery-product"));
        mvc.perform(get("/san-pham/test-pottery-product")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<title>Test pottery |")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("https://schema.org/OutOfStock")));
        mvc.perform(get("/san-pham/hidden-pottery-product")).andExpect(status().isNotFound());
        mvc.perform(get("/san-pham/no-such-product")).andExpect(status().isNotFound());
        mvc.perform(get("/sitemap.xml")).andExpect(content().string(org.hamcrest.Matchers.containsString("/san-pham/test-pottery-product")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("hidden-pottery-product"))));
    }

    @Test void faqSaveHideAndDeleteControlPublicContent() throws Exception {
        Cookie admin = admin();
        String item = """
                {"questionVi":"Câu hỏi?","answerVi":"Câu trả lời","questionEn":"Question?","answerEn":"Answer","position":1,"visible":true}
                """;
        mvc.perform(post("/api/v1/admin/faq").with(csrf().asHeader()).contentType(MediaType.APPLICATION_JSON).content(item))
                .andExpect(status().isUnauthorized());
        var result = mvc.perform(post("/api/v1/admin/faq").cookie(admin).with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(item)).andExpect(status().isOk()).andReturn();
        long id = ((Number) com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.data.id")).longValue();
        mvc.perform(get("/api/v1/faq")).andExpect(jsonPath("$.data[0].questionVi").value("Câu hỏi?"));
        mvc.perform(put("/api/v1/admin/faq/{id}", id).cookie(admin).with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(item.replace("true", "false"))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/faq")).andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(delete("/api/v1/admin/faq/{id}", id).cookie(admin).with(csrf().asHeader())).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM faq_items", Long.class)).isZero();
    }

    private Cookie admin() {
        Instant now = Instant.now();
        User user = users.save(new User("seo-admin", "seo-admin@example.com", "Admin", null,
                UserRole.ADMIN, UserStatus.ACTIVE, now, now));
        return new Cookie(JwtAuthenticationFilter.ACCESS_COOKIE_NAME, tokens.createAccessToken(user));
    }
}
