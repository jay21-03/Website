package com.bautruc.ecommerce.sitecontent.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import com.bautruc.ecommerce.catalog.application.HomepageProductQuery;
import com.bautruc.ecommerce.catalog.application.HomepageProductView;
import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.common.storage.ObjectStoragePort;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.sitecontent.infrastructure.HomepageFeaturedProductJpaRepository;
import com.bautruc.ecommerce.sitecontent.infrastructure.SiteMediaJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HomepageContentServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-16T04:00:00Z");

    @Mock
    SiteMediaJpaRepository media;

    @Mock
    HomepageFeaturedProductJpaRepository featured;

    @Mock
    HomepageProductQuery products;

    @Mock
    ObjectStoragePort storage;

    HomepageContentService service;

    @BeforeEach
    void setUp() {
        BusinessClock clock = new BusinessClock() {
            @Override
            public Instant now() {
                return NOW;
            }

            @Override
            public ZoneId businessZone() {
                return ZoneId.of("Asia/Ho_Chi_Minh");
            }
        };
        service = new HomepageContentService(media, featured, products, clock, storage);
    }

    @Test
    void rejectsDuplicateFeaturedProductIds() {
        assertThatThrownBy(() -> service.updateFeaturedProducts(List.of(10L, 10L, 20L)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.code()).isEqualTo(SiteContentErrorCodes.HOMEPAGE_FEATURED_PRODUCTS_INVALID));
    }

    @Test
    void rejectsProductsThatAreNotAllPublicAndActive() {
        when(products.publicProductsByIds(anyCollection())).thenReturn(List.of(
                product(10L),
                product(20L)
        ));

        assertThatThrownBy(() -> service.updateFeaturedProducts(List.of(10L, 20L, 30L)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.code()).isEqualTo(SiteContentErrorCodes.HOMEPAGE_FEATURED_PRODUCTS_INVALID));
    }

    @Test
    void replacesExistingFeaturedConfigurationForThreeValidProducts() {
        when(products.publicProductsByIds(anyCollection())).thenReturn(List.of(
                product(30L),
                product(10L),
                product(20L)
        ));
        when(featured.findAllByOrderBySlotAsc()).thenReturn(List.of());

        service.updateFeaturedProducts(List.of(10L, 20L, 30L));

        verify(featured).deleteAllInBatch();
        verify(featured).saveAll(org.mockito.ArgumentMatchers.anyList());
        verify(featured).flush();
    }

    private HomepageProductView product(Long id) {
        return new HomepageProductView(id, "VI " + id, "EN " + id, null, null, 1000, 1000, null);
    }
}
