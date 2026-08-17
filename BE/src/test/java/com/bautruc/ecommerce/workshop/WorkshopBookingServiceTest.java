package com.bautruc.ecommerce.workshop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.List;
import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.workshop.api.request.WorkshopBookingRequest;
import com.bautruc.ecommerce.workshop.api.request.WorkshopBookingStatusRequest;
import com.bautruc.ecommerce.workshop.api.request.WorkshopOfferingRequest;
import com.bautruc.ecommerce.workshop.application.WorkshopBookingService;
import com.bautruc.ecommerce.workshop.application.WorkshopOfferingService;
import com.bautruc.ecommerce.workshop.domain.WorkshopBookingStatus;
import com.bautruc.ecommerce.workshop.domain.WorkshopOfferingStatus;
import org.flywaydb.core.Flyway;
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
class WorkshopBookingServiceTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private WorkshopBookingService service;

    @Autowired
    private WorkshopOfferingService offeringService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @Test
    void flywayCreatesWorkshopBookingsTable() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("10");
        Long tableCount = jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.tables
                where table_schema = 'public'
                  and table_name = 'workshop_bookings'
                """,
                Long.class
        );
        assertThat(tableCount).isEqualTo(1);
        Long offeringTableCount = jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.tables
                where table_schema = 'public'
                  and table_name = 'workshop_offerings'
                """,
                Long.class
        );
        assertThat(offeringTableCount).isEqualTo(1);
    }

    @Test
    void createsAndUpdatesWorkshopBooking() {
        var offering = offeringService.create(new WorkshopOfferingRequest(
                "Lam gom co ban",
                "Trai nghiem tao hinh gom bang tay",
                150000,
                120,
                10,
                null,
                WorkshopOfferingStatus.ACTIVE
        ));
        var booking = service.create(new WorkshopBookingRequest(
                offering.getId(),
                "Nguyen Van A",
                "Customer@Example.com",
                "0909000000",
                OffsetDateTime.now().plusDays(2),
                4,
                "Muon trai nghiem lam gom"
        ));

        assertThat(booking.getId()).isNotNull();
        assertThat(booking.getWorkshopId()).isEqualTo(offering.getId());
        assertThat(booking.getEmail()).isEqualTo("customer@example.com");
        assertThat(booking.getStatus()).isEqualTo(WorkshopBookingStatus.NEW);

        var updated = service.updateStatus(
                booking.getId(),
                new WorkshopBookingStatusRequest(WorkshopBookingStatus.CONFIRMED)
        );
        assertThat(updated.getStatus()).isEqualTo(WorkshopBookingStatus.CONFIRMED);

        var confirmed = service.list(WorkshopBookingStatus.CONFIRMED, 0, 20);
        assertThat(confirmed.getContent()).extracting("id").contains(booking.getId());
    }

    @Test
    void rejectsPastWorkshopBookingTime() {
        assertThatThrownBy(() -> service.create(new WorkshopBookingRequest(
                null,
                "Nguyen Van B",
                "b@example.com",
                "0909000001",
                OffsetDateTime.now().minusDays(1),
                2,
                null
        ))).isInstanceOf(BusinessException.class)
                .hasMessageContaining("future");
    }
}
