package com.bautruc.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class BautrucEcommerceApplicationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoadsWithPostgreSqlAndFlyway() {
        assertThat(dataSource).isNotNull();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("8");
        List<String> appliedVersions = jdbcTemplate.queryForList(
                """
                select version
                from flyway_schema_history
                where success = true
                order by installed_rank
                """,
                String.class
        );
        assertThat(appliedVersions).containsExactly("1", "2", "3", "4", "5", "6", "7", "8");
        Long sequenceCount = jdbcTemplate.queryForObject(
                "select count(*) from pg_class where relkind = 'S' and relname = 'app_global_id_seq'",
                Long.class
        );
        assertThat(sequenceCount).isEqualTo(1);
        Long legacySequenceCount = jdbcTemplate.queryForObject(
                "select count(*) from pg_class where relkind = 'S' and relname = 'bt_global_sequence'",
                Long.class
        );
        assertThat(legacySequenceCount).isZero();
        Long usersTableCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'public' and table_name = 'users'",
                Long.class
        );
        assertThat(usersTableCount).isEqualTo(1);
        List<String> commerceTables = jdbcTemplate.queryForList(
                "select table_name from information_schema.tables where table_schema='public' and table_name in ('products','carts','cart_items','inventories','inventory_transactions') order by table_name",
                String.class
        );
        assertThat(commerceTables).containsExactly("cart_items", "carts", "inventories", "inventory_transactions", "products");
        List<String> checkoutTables = jdbcTemplate.queryForList(
                "select table_name from information_schema.tables where table_schema='public' and table_name in ('orders','order_items','payments','checkout_operations','checkout_operation_items') order by table_name",
                String.class
        );
        assertThat(checkoutTables).containsExactly("checkout_operation_items", "checkout_operations", "order_items", "orders", "payments");
        List<String> notificationTables = jdbcTemplate.queryForList(
                "select table_name from information_schema.tables where table_schema='public' and table_name in ('notifications','notification_recipients') order by table_name",
                String.class
        );
        assertThat(notificationTables).containsExactly("notification_recipients", "notifications");
        String idDefault = jdbcTemplate.queryForObject(
                """
                select column_default
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'users'
                  and column_name = 'id'
                """,
                String.class
        );
        assertThat(idDefault).contains("app_global_id_seq");
    }
}
