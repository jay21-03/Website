package com.bautruc.ecommerce.sitecontent.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;

import com.bautruc.ecommerce.common.security.JwtAuthenticationFilter;
import com.bautruc.ecommerce.common.security.JwtTokenService;
import com.bautruc.ecommerce.common.storage.ObjectStoragePort;
import com.bautruc.ecommerce.identity.domain.*;
import com.bautruc.ecommerce.identity.infrastructure.UserJpaRepository;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AboutContentApiIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserJpaRepository users;
    @Autowired JwtTokenService tokens;

    @MockitoBean ObjectStoragePort storage;

    @BeforeEach
    void clean() {
        jdbc.execute(
                "TRUNCATE TABLE about_page_content, site_presentation, users CASCADE"
        );
        jdbc.update("""
                UPDATE site_media
                SET object_key = NULL,
                    content_type = NULL,
                    file_size_bytes = NULL,
                    updated_at = now()
WHERE slot LIKE 'ABOUT_%' OR slot IN ('CATALOG_HERO', 'WORKSHOP_HERO', 'SUPPORT_HERO', 'CHECKOUT_HERO', 'SITE_LOGO',
    'CATALOG_HERO_DARK', 'WORKSHOP_HERO_DARK', 'SUPPORT_HERO_DARK', 'CHECKOUT_HERO_DARK', 'SITE_LOGO_DARK')
                """);

        when(storage.publicUrl(anyString()))
                .thenAnswer(invocation ->
                        "https://cdn.example/" + invocation.getArgument(0)
                );
    }

    @Test
    void publicAboutHasFallbackStateWithoutConfiguration()
            throws Exception {
        mvc.perform(get("/api/v1/about"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").doesNotExist())
                .andExpect(jsonPath("$.data.certificateUrl").doesNotExist());
    }

    @Test
    void onlyAdminCanWriteContentAndCsrfIsRequired()
            throws Exception {
        mvc.perform(put("/api/v1/admin/about")
                        .with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validContent()))
                .andExpect(status().isUnauthorized());

        mvc.perform(put("/api/v1/admin/about")
                        .cookie(access(user("customer", UserRole.USER)))
                        .with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validContent()))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/v1/admin/about")
                        .cookie(access(user("admin", UserRole.ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validContent()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanSaveAndPublicCanReadBilingualContent()
            throws Exception {
        Cookie admin = access(user("editor", UserRole.ADMIN));

        mvc.perform(put("/api/v1/admin/about")
                        .cookie(admin)
                        .with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validContent()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.heroTitle.vi")
                        .value("Tiêu đề Việt"))
                .andExpect(jsonPath("$.data.content.heroTitle.en")
                        .value("English title"));

        mvc.perform(get("/api/v1/about"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.timeline.length()")
                        .value(1))
                .andExpect(jsonPath("$.data.content.values.length()")
                        .value(1))
                .andExpect(jsonPath("$.data.content.certificateBody.en")
                        .value("Certificate description"));

        mvc.perform(put("/api/v1/admin/about")
                        .cookie(admin)
                        .with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validContent().replace(
                                "\"English title\"",
                                "\"Updated title\""
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.heroTitle.en")
                        .value("Updated title"));

        mvc.perform(get("/api/v1/about"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.heroTitle.en")
                        .value("Updated title"));
    }

    @Test
    void blankTranslationsAreRejected() throws Exception {
        Cookie admin = access(user("invalid", UserRole.ADMIN));

        mvc.perform(put("/api/v1/admin/about")
                        .cookie(admin)
                        .with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validContent().replace(
                                "\"English title\"",
                                "\"   \""
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("VALIDATION_FAILED"));

        mvc.perform(get("/api/v1/about"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").doesNotExist());
    }

    @Test
    void adminCanUploadAndClearCertificate() throws Exception {
        Cookie admin = access(user("media", UserRole.ADMIN));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "certificate.png",
                "image/png",
                new byte[] {
                        (byte) 0x89, 0x50, 0x4e, 0x47,
                        0x0d, 0x0a, 0x1a, 0x0a
                }
        );

        mvc.perform(multipart(
                            "/api/v1/admin/about/media/ABOUT_CERTIFICATE"
                        )
                        .file(file)
                        .cookie(admin)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .with(csrf().asHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slot")
                        .value("ABOUT_CERTIFICATE"))
                .andExpect(jsonPath("$.data.url")
                        .value(org.hamcrest.Matchers.startsWith(
                                "https://cdn.example/site/about/certificate/"
                        )));

        mvc.perform(get("/api/v1/about"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.certificateUrl")
                        .exists());

        mvc.perform(delete(
                            "/api/v1/admin/about/media/ABOUT_CERTIFICATE"
                        )
                        .cookie(admin)
                        .with(csrf().asHeader()))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/about"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.certificateUrl")
                        .doesNotExist());
    }

    @Test
    void aboutMediaEndpointRejectsHomepageSlots()
            throws Exception {
        Cookie admin = access(user("slot", UserRole.ADMIN));

        mvc.perform(delete(
                            "/api/v1/admin/about/media/HOME_HERO"
                        )
                        .cookie(admin)
                        .with(csrf().asHeader()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void presentationRequiresAdminAndPreservesCheckoutFields() throws Exception {
        String valid = """
                {"pages":{"checkout":{"title":{"vi":"Thanh toán","en":"Checkout"}}},
                 "checkout":{"fieldOrder":["receiverName","phone","email","address","note"],
                             "sectionOrder":["summary","form"],"showNote":false,"showEmail":true},
                 "colorPreset":"sage","fontPreset":"modern"}
                """;
        mvc.perform(get("/api/v1/site/presentation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").doesNotExist());

        mvc.perform(put("/api/v1/admin/site/presentation")
                        .contentType(MediaType.APPLICATION_JSON).content(valid).with(csrf().asHeader()))
                .andExpect(status().isUnauthorized());

        mvc.perform(put("/api/v1/admin/site/presentation")
                        .cookie(access(user("viewer", UserRole.USER)))
                        .contentType(MediaType.APPLICATION_JSON).content(valid).with(csrf().asHeader()))
                .andExpect(status().isForbidden());

        Cookie admin = access(user("presentation", UserRole.ADMIN));
        mvc.perform(put("/api/v1/admin/site/presentation")
                        .cookie(admin).contentType(MediaType.APPLICATION_JSON).content(valid))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/v1/admin/site/presentation")
                        .cookie(admin).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON).content(valid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.checkout.sectionOrder[0]").value("summary"));

        mvc.perform(get("/api/v1/site/presentation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.pages.checkout.title.vi").value("Thanh toán"));
        mvc.perform(put("/api/v1/admin/site/presentation")
                        .cookie(admin).with(csrf().asHeader()).contentType(MediaType.APPLICATION_JSON)
                        .content(valid.replace("\"Checkout\"", "\"Checkout\",\"font\":\"vietnamese\",\"color\":\"#563820\"")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/site/presentation"))
                .andExpect(jsonPath("$.data.content.pages.checkout.title.font").value("vietnamese"))
                .andExpect(jsonPath("$.data.content.pages.checkout.title.color").value("#563820"));
        String themed = """
                {"pages":{"brand":{"name":{"vi":"Tên sáng","en":"Light name"}}},
                 "checkout":{"fieldOrder":["receiverName","phone","email","address","note"],
                             "sectionOrder":["form","summary"],"showNote":true,"showEmail":true},
                 "colorPreset":"clay","fontPreset":"heritage",
                 "colors":{"background":"#FBF6EC","card":"#F1E4D2","primary":"#9D4B2F",
                           "text":"#2C1A0E","secondary":"#704126","accent":"#C69A56"},
                 "dark":{"pages":{"brand":{"name":{"vi":"Tên tối","en":"Dark name"}}},
                         "checkout":{"fieldOrder":["receiverName","phone","email","address","note"],
                                     "sectionOrder":["summary","form"],"showNote":false,"showEmail":true},
                         "colorPreset":"night","fontPreset":"editorial",
                         "colors":{"background":"#1D1713","card":"#30251E","primary":"#D18B5A",
                                   "text":"#F5E9DB","secondary":"#AE8A72","accent":"#E2B976"}}}
                """;
        mvc.perform(put("/api/v1/admin/site/presentation")
                        .cookie(admin).with(csrf().asHeader()).contentType(MediaType.APPLICATION_JSON).content(themed))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/site/presentation"))
                .andExpect(jsonPath("$.data.content.pages.brand.name.vi").value("Tên sáng"))
                .andExpect(jsonPath("$.data.content.dark.pages.brand.name.vi").value("Tên tối"))
                .andExpect(jsonPath("$.data.content.dark.colors.background").value("#1D1713"));
        mvc.perform(put("/api/v1/admin/site/presentation")
                        .cookie(admin).with(csrf().asHeader()).contentType(MediaType.APPLICATION_JSON)
                        .content(themed.replace("#F5E9DB", "invalid")))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/v1/admin/site/presentation")
                        .cookie(admin).with(csrf().asHeader()).contentType(MediaType.APPLICATION_JSON)
                        .content(valid.replace("\"receiverName\",", "")))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/v1/admin/site/presentation")
                        .cookie(admin).with(csrf().asHeader()).contentType(MediaType.APPLICATION_JSON)
                        .content(valid.replace("\"Thanh toán\"", "\"   \"")))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/admin/site/presentation")
                        .cookie(admin).with(csrf().asHeader()).contentType(MediaType.APPLICATION_JSON)
                        .content(valid.replace("\"Checkout\"", "\"Checkout\",\"font\":\"unknown\"")))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/admin/site/presentation")
                        .cookie(admin).with(csrf().asHeader()).contentType(MediaType.APPLICATION_JSON)
                        .content(valid.replace("\"Checkout\"", "\"Checkout\",\"color\":\"red\"")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void siteMediaUsesSeparateSlotsAndRejectsHomepageMedia() throws Exception {
        Cookie admin = access(user("site-media", UserRole.ADMIN));
        mvc.perform(delete("/api/v1/admin/site/media/HOME_HERO")
                        .cookie(admin).with(csrf().asHeader()))
                .andExpect(status().isBadRequest());

        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png",
                new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
        mvc.perform(multipart("/api/v1/admin/site/media/SITE_LOGO")
                        .file(file).cookie(admin).with(csrf().asHeader())
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.startsWith(
                        "https://cdn.example/site/brand/logo/")));

        mvc.perform(get("/api/v1/site/presentation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.media.SITE_LOGO").isNotEmpty());
        mvc.perform(multipart("/api/v1/admin/site/media/CATALOG_HERO")
                        .file(file).cookie(admin).with(csrf().asHeader())
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/site/presentation"))
                .andExpect(jsonPath("$.data.media.CATALOG_HERO").isNotEmpty());
        mvc.perform(multipart("/api/v1/admin/site/media/SITE_LOGO_DARK")
                        .file(file).cookie(admin).with(csrf().asHeader())
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/site/presentation"))
                .andExpect(jsonPath("$.data.media.SITE_LOGO_DARK").isNotEmpty());
    }

    private User user(String key, UserRole role) {
        Instant now = Instant.now();
        return users.save(new User(
                "about-" + key,
                "about-" + key + "@example.com",
                key,
                null,
                role,
                UserStatus.ACTIVE,
                now,
                now
        ));
    }

    private Cookie access(User user) {
        return new Cookie(
                JwtAuthenticationFilter.ACCESS_COOKIE_NAME,
                tokens.createAccessToken(user)
        );
    }

    private String validContent() {
        return """
                {
                  "heroTitle": {"vi":"Tiêu đề Việt","en":"English title"},
                  "heroSubtitle": {"vi":"Phụ đề Việt","en":"English subtitle"},
                  "storyEyebrow": {"vi":"Hành trình","en":"Journey"},
                  "storyTitle": {"vi":"Câu chuyện","en":"Our story"},
                  "timeline": [
                    {"year":"2022","text":{"vi":"Năm 2022","en":"Year 2022"}}
                  ],
                  "valuesEyebrow": {"vi":"Giá trị","en":"Values"},
                  "valuesTitle": {"vi":"Giá trị cốt lõi","en":"Core values"},
                  "values": [
                    {
                      "title":{"vi":"Bền vững","en":"Sustainability"},
                      "description":{"vi":"Mô tả","en":"Description"}
                    }
                  ],
                  "heritageTitle": {"vi":"Di sản","en":"Heritage"},
                  "heritageBody": {"vi":"Nội dung di sản","en":"Heritage description"},
                  "certificateEyebrow": {"vi":"Chứng nhận","en":"Certification"},
                  "certificateTitle": {"vi":"Chứng nhận sản phẩm","en":"Product certificate"},
                  "certificateBody": {"vi":"Nội dung chứng nhận","en":"Certificate description"},
                  "certificateMockTitle": {"vi":"Gốm Chăm","en":"Cham pottery"},
                  "certificateMockPiece": {"vi":"Tác phẩm","en":"Piece"},
                  "certificateMockMaker": {"vi":"Nghệ nhân","en":"Artisan"},
                  "certificateMockSlogan": {"vi":"Di sản","en":"Heritage"}
                }
                """;
    }
}
