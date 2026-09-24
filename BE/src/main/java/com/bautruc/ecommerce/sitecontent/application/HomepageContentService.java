package com.bautruc.ecommerce.sitecontent.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bautruc.ecommerce.catalog.application.HomepageProductQuery;
import com.bautruc.ecommerce.catalog.application.HomepageProductView;
import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.common.storage.ObjectStoragePort;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.sitecontent.domain.HomepageFeaturedProduct;
import com.bautruc.ecommerce.sitecontent.domain.HomepageSettings;
import com.bautruc.ecommerce.sitecontent.domain.SiteMedia;
import com.bautruc.ecommerce.sitecontent.domain.SiteMediaSlot;
import com.bautruc.ecommerce.sitecontent.infrastructure.HomepageFeaturedProductJpaRepository;
import com.bautruc.ecommerce.sitecontent.infrastructure.HomepageSettingsJpaRepository;
import com.bautruc.ecommerce.sitecontent.infrastructure.SiteMediaJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomepageContentService {
    private static final int FEATURED_COUNT = 3;
    public static final String DEFAULT_SLOGAN_VI = "Tinh hoa gốm Chăm – Gìn giữ hồn di sản";
    public static final String DEFAULT_SLOGAN_EN = "The essence of Cham pottery – preserving the soul of heritage";

    private final SiteMediaJpaRepository media;
    private final HomepageFeaturedProductJpaRepository featured;
    private final HomepageSettingsJpaRepository settings;
    private final HomepageProductQuery productQuery;
    private final BusinessClock clock;
    private final ObjectStoragePort storage;

    public HomepageContentService(
            SiteMediaJpaRepository media,
            HomepageFeaturedProductJpaRepository featured,
            HomepageSettingsJpaRepository settings,
            HomepageProductQuery productQuery,
            BusinessClock clock,
            ObjectStoragePort storage
    ) {
        this.media = media;
        this.featured = featured;
        this.settings = settings;
        this.productQuery = productQuery;
        this.clock = clock;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public HomepageContentSnapshot current() {
        Map<SiteMediaSlot, SiteMedia> mediaBySlot = new HashMap<>();
        for (SiteMedia item : media.findAll()) {
            mediaBySlot.put(item.getSlot(), item);
        }

        List<SiteMediaView> mediaViews = new ArrayList<>();
        for (SiteMediaSlot slot : SiteMediaSlot.values()) {
            SiteMedia item = mediaBySlot.get(slot);
            String url = item == null || item.getObjectKey() == null ? null : storage.publicUrl(item.getObjectKey());
            mediaViews.add(new SiteMediaView(slot, url, item == null ? null : item.getUpdatedAt()));
        }

        HomepageSloganView slogan = settings.findById(HomepageSettings.SINGLETON_ID)
                .map(value -> new HomepageSloganView(value.getSloganVi(), value.getSloganEn(), value.getUpdatedAt()))
                .orElse(new HomepageSloganView(DEFAULT_SLOGAN_VI, DEFAULT_SLOGAN_EN, null));

        List<HomepageFeaturedProduct> configured = featured.findAllByOrderBySlotAsc();
        if (configured.isEmpty()) {
            return new HomepageContentSnapshot(List.copyOf(mediaViews), List.of(), slogan);
        }

        List<Long> productIds = configured.stream().map(HomepageFeaturedProduct::getProductId).toList();
        Map<Long, HomepageProductView> publicProducts = new HashMap<>();
        for (HomepageProductView product : productQuery.publicProductsByIds(productIds)) {
            publicProducts.put(product.id(), product);
        }

        List<FeaturedProductItem> featuredItems = new ArrayList<>();
        for (HomepageFeaturedProduct item : configured) {
            HomepageProductView product = publicProducts.get(item.getProductId());
            if (product != null) {
                featuredItems.add(new FeaturedProductItem(item.getSlot(), product));
            }
        }

        return new HomepageContentSnapshot(List.copyOf(mediaViews), List.copyOf(featuredItems), slogan);
    }

    @Transactional
    public HomepageContentSnapshot updateSlogan(String sloganVi, String sloganEn) {
        String normalizedVi = sloganVi.trim();
        String normalizedEn = sloganEn.trim();
        HomepageSettings value = settings.findById(HomepageSettings.SINGLETON_ID)
                .orElseGet(() -> new HomepageSettings(normalizedVi, normalizedEn, clock.now()));
        value.update(normalizedVi, normalizedEn, clock.now());
        settings.saveAndFlush(value);
        return current();
    }

    @Transactional
    public HomepageContentSnapshot updateFeaturedProducts(List<Long> productIds) {
        validateRequest(productIds);

        List<HomepageProductView> publicProducts = productQuery.publicProductsByIds(productIds);
        Set<Long> validIds = new HashSet<>();
        for (HomepageProductView product : publicProducts) {
            validIds.add(product.id());
        }
        if (validIds.size() != FEATURED_COUNT || !validIds.containsAll(productIds)) {
            throw invalidFeaturedProducts();
        }

        Instant now = clock.now();
        List<HomepageFeaturedProduct> rows = new ArrayList<>();
        for (int index = 0; index < productIds.size(); index++) {
            rows.add(new HomepageFeaturedProduct((short) (index + 1), productIds.get(index), now));
        }

        featured.deleteAllInBatch();
        featured.saveAll(rows);
        featured.flush();

        return current();
    }

    private void validateRequest(List<Long> productIds) {
        if (productIds == null || productIds.size() != FEATURED_COUNT) {
            throw invalidFeaturedProducts();
        }
        Set<Long> unique = new HashSet<>(productIds);
        if (unique.size() != FEATURED_COUNT || unique.contains(null)) {
            throw invalidFeaturedProducts();
        }
    }

    private BusinessException invalidFeaturedProducts() {
        return new BusinessException(
                SiteContentErrorCodes.HOMEPAGE_FEATURED_PRODUCTS_INVALID,
                "Homepage featured products must contain exactly three distinct active products."
        );
    }
}
