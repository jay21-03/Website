package com.bautruc.ecommerce.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.bautruc.ecommerce.support.api.request.SupportSettingsRequest;
import com.bautruc.ecommerce.support.application.SupportSettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class SupportSettingsServiceTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private SupportSettingsService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void readsAndUpdatesSupportSettings() {
        assertThat(service.current().getEmail()).isEqualTo("Cosogombautrucdangxem@gmail.com");

        var updated = service.update(new SupportSettingsRequest(
                "support@example.com",
                "0909000000",
                "0909000001",
                "https://facebook.example.com",
                "Bau Truc",
                "https://maps.example.com",
                "8:00 - 17:00"
        ));

        assertThat(updated.getEmail()).isEqualTo("support@example.com");
        assertThat(updated.getZaloPhone()).isEqualTo("0909000000");
        assertThat(updated.getSecondaryPhone()).isEqualTo("0909000001");
        Long rowCount = jdbcTemplate.queryForObject("select count(*) from support_settings where id = 1", Long.class);
        assertThat(rowCount).isEqualTo(1);
    }
}
