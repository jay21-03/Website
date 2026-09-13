package com.bautruc.ecommerce.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@Testcontainers
class ProductionSwaggerDisabledTest {

    private static final String TEST_JWT_SECRET =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerProductionTestProperties(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", postgres::getJdbcUrl);
        registry.add("DB_USERNAME", postgres::getUsername);
        registry.add("DB_PASSWORD", postgres::getPassword);

        registry.add("FRONTEND_BASE_URL", () -> "https://example.test");
        registry.add("ALLOWED_ORIGINS", () -> "https://example.test");

        registry.add("GOOGLE_CLIENT_ID", () -> "test-google-client-id");
        registry.add("ADMIN_EMAILS", () -> "admin@example.test");

        registry.add("JWT_SECRET_BASE64", () -> TEST_JWT_SECRET);

        // No real AWS connection is needed for this test.
        registry.add("AWS_REGION", () -> "ap-southeast-1");
        registry.add("S3_BUCKET_NAME", () -> "");
        registry.add("S3_PUBLIC_BASE_URL", () -> "");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    @Test
    @DisplayName("production profile disables Swagger UI and OpenAPI docs")
    void productionProfileDisablesSwaggerUiAndApiDocs() throws Exception {
        assertThat(environment.matchesProfiles("prod")).isTrue();

        assertThat(environment.getProperty("springdoc.api-docs.enabled"))
                .isEqualTo("false");

        assertThat(environment.getProperty("springdoc.swagger-ui.enabled"))
                .isEqualTo("false");

        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isNotFound());
    }
}