package com.bautruc.ecommerce.sitecontent.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bautruc.ecommerce.catalog.application.HomepageProductQuery;
import com.bautruc.ecommerce.catalog.application.HomepageProductView;
import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.common.storage.ObjectStoragePort;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.sitecontent.domain.HomepageContent;
import com.bautruc.ecommerce.sitecontent.domain.HomepageFeaturedProduct;
import com.bautruc.ecommerce.sitecontent.domain.SiteMedia;
import com.bautruc.ecommerce.sitecontent.domain.SiteMediaSlot;
import com.bautruc.ecommerce.sitecontent.infrastructure.HomepageContentJpaRepository;
import com.bautruc.ecommerce.sitecontent.infrastructure.HomepageFeaturedProductJpaRepository;
import com.bautruc.ecommerce.sitecontent.infrastructure.SiteMediaJpaRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomepageContentService {
    private static final int FEATURED_COUNT = 3;
    private static final int COPY_VALUE_MAX_LENGTH = 1200;
    private static final String DEFAULT_SLOGAN_VI = "Tinh hoa gốm Chăm – Gìn giữ hồn di sản";
    private static final String DEFAULT_SLOGAN_EN = "The essence of Cham pottery - preserving heritage soul";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<Map<String, String>> COPY_TYPE = new TypeReference<>() {};
    private static final Map<String, String> DEFAULT_COPY = defaultCopy();

    private final HomepageContentJpaRepository content;
    private final SiteMediaJpaRepository media;
    private final HomepageFeaturedProductJpaRepository featured;
    private final HomepageProductQuery productQuery;
    private final BusinessClock clock;
    private final ObjectStoragePort storage;

    public HomepageContentService(
            HomepageContentJpaRepository content,
            SiteMediaJpaRepository media,
            HomepageFeaturedProductJpaRepository featured,
            HomepageProductQuery productQuery,
            BusinessClock clock,
            ObjectStoragePort storage
    ) {
        this.content = content;
        this.media = media;
        this.featured = featured;
        this.productQuery = productQuery;
        this.clock = clock;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public HomepageContentSnapshot current() {
        HomepageContent copy = content.findById(HomepageContent.SINGLETON_ID).orElse(null);
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

        List<HomepageFeaturedProduct> configured = featured.findAllByOrderBySlotAsc();
        if (configured.isEmpty()) {
            return snapshot(copy, List.copyOf(mediaViews), List.of());
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

        return snapshot(copy, List.copyOf(mediaViews), List.copyOf(featuredItems));
    }

    @Transactional
    public HomepageContentSnapshot updateSlogan(String sloganVi, String sloganEn) {
        HomepageContent copy = content.findById(HomepageContent.SINGLETON_ID)
                .orElseThrow(() -> new BusinessException(
                        SiteContentErrorCodes.HOMEPAGE_CONTENT_NOT_FOUND,
                        "Homepage content not found."
                ));
        copy.updateSlogan(sloganVi, sloganEn, clock.now());
        content.flush();
        return current();
    }

    @Transactional
    public HomepageContentSnapshot updateCopy(Map<String, String> copy) {
        HomepageContent contentRow = content.findById(HomepageContent.SINGLETON_ID)
                .orElseThrow(() -> new BusinessException(
                        SiteContentErrorCodes.HOMEPAGE_CONTENT_NOT_FOUND,
                        "Homepage content not found."
                ));
        contentRow.updateCopyJson(writeCopy(sanitizeCopy(copy)), clock.now());
        content.flush();
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

    private HomepageContentSnapshot snapshot(
            HomepageContent copy,
            List<SiteMediaView> mediaViews,
            List<FeaturedProductItem> featuredItems
    ) {
        String sloganVi = copy == null ? DEFAULT_SLOGAN_VI : copy.getSloganVi();
        String sloganEn = copy == null ? DEFAULT_SLOGAN_EN : copy.getSloganEn();
        Map<String, String> contentCopy = copy == null ? DEFAULT_COPY : readCopy(copy.getCopyJson());
        return new HomepageContentSnapshot(sloganVi, sloganEn, contentCopy, mediaViews, featuredItems);
    }

    private static Map<String, String> readCopy(String json) {
        try {
            return sanitizeCopy(JSON.readValue(json == null || json.isBlank() ? "{}" : json, COPY_TYPE));
        } catch (Exception ignored) {
            return DEFAULT_COPY;
        }
    }

    private static String writeCopy(Map<String, String> copy) {
        try {
            return JSON.writeValueAsString(copy);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid homepage copy.", e);
        }
    }

    private static Map<String, String> sanitizeCopy(Map<String, String> input) {
        Map<String, String> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : DEFAULT_COPY.entrySet()) {
            String value = input == null ? null : input.get(entry.getKey());
            if (value == null || value.isBlank()) {
                value = entry.getValue();
            }
            value = value.trim();
            if (value.length() > COPY_VALUE_MAX_LENGTH) {
                value = value.substring(0, COPY_VALUE_MAX_LENGTH);
            }
            sanitized.put(entry.getKey(), value);
        }
        return Map.copyOf(sanitized);
    }

    private static Map<String, String> defaultCopy() {
        Map<String, String> copy = new LinkedHashMap<>();
        copy.put("heroEyebrowVi", "Làng gốm Bàu Trúc · Khánh Hòa");
        copy.put("heroEyebrowEn", "Bàu Trúc pottery village · Khánh Hòa");
        copy.put("heroSubtitleVi", "Gốm thủ công Chăm Bàu Trúc | Di sản UNESCO 2022");
        copy.put("heroSubtitleEn", "Handmade Cham pottery from Bàu Trúc | UNESCO Heritage 2022");
        copy.put("heroDescriptionVi", "Mỗi sản phẩm là một phiên bản duy nhất — được tạo ra hoàn toàn bằng đôi bàn tay của nghệ nhân Chăm, từ đất sét tự nhiên làng Bàu Trúc, Khánh Hòa.");
        copy.put("heroDescriptionEn", "Every piece is one of a kind — shaped entirely by the hands of Cham artisans from natural clay of Bàu Trúc village, Khánh Hòa.");
        copy.put("heroPrimaryLabelVi", "Khám Phá Sản Phẩm");
        copy.put("heroPrimaryLabelEn", "Explore Products");
        copy.put("heroSecondaryLabelVi", "Đặt Lịch Trải Nghiệm");
        copy.put("heroSecondaryLabelEn", "Book an Experience");
        copy.put("trust1TitleVi", "100% Thủ Công");
        copy.put("trust1TitleEn", "100% Handmade");
        copy.put("trust1DescriptionVi", "Không khuôn, không bàn xoay máy — tạo hình hoàn toàn bằng tay.");
        copy.put("trust1DescriptionEn", "No molds, no wheel — every piece is shaped entirely by hand.");
        copy.put("trust2TitleVi", "Nguyên Liệu Tự Nhiên");
        copy.put("trust2TitleEn", "Natural Materials");
        copy.put("trust2DescriptionVi", "Đất sét lấy từ cánh đồng Nu Lanh, pha cát mịn sông Quao.");
        copy.put("trust2DescriptionEn", "Clay from the Nu Lanh fields blended with fine Quao river sand.");
        copy.put("trust3TitleVi", "Di Sản UNESCO");
        copy.put("trust3TitleEn", "UNESCO Heritage");
        copy.put("trust3DescriptionVi", "Nghệ thuật gốm Chăm Bàu Trúc được ghi danh năm 2022.");
        copy.put("trust3DescriptionEn", "Cham pottery of Bàu Trúc was inscribed by UNESCO in 2022.");
        copy.put("storyEyebrowVi", "Câu chuyện");
        copy.put("storyEyebrowEn", "Our story");
        copy.put("storyTitleVi", "Hơn 15 năm giữ lửa nghề Chăm");
        copy.put("storyTitleEn", "Over 15 years keeping the Cham craft alive");
        copy.put("storyBodyVi", "Từ năm 2009, Nghệ nhân Đàng Xem đã gìn giữ và phát triển nghề gốm truyền thống của người Chăm tại làng Bàu Trúc. Mỗi sản phẩm mang trong mình hàng trăm năm lịch sử và dấu ấn riêng của đôi bàn tay tạo ra nó.");
        copy.put("storyBodyEn", "Since 2009, artisan Đàng Xem has preserved and developed the traditional Cham pottery craft in Bàu Trúc village. Each piece carries centuries of history and the mark of the hands that made it.");
        copy.put("storyLinkLabelVi", "Đọc thêm câu chuyện của chúng tôi →");
        copy.put("storyLinkLabelEn", "Read more of our story →");
        copy.put("featuredEyebrowVi", "Bộ sưu tập");
        copy.put("featuredEyebrowEn", "Collection");
        copy.put("featuredTitleVi", "Sản Phẩm Nổi Bật");
        copy.put("featuredTitleEn", "Featured Products");
        copy.put("featuredEmptyVi", "Sản phẩm nổi bật đang được cập nhật.");
        copy.put("featuredEmptyEn", "Featured products are being updated.");
        copy.put("experienceTitleVi", "Trải Nghiệm Làm Gốm Cùng Nghệ Nhân");
        copy.put("experienceTitleEn", "Make Pottery with the Artisan");
        copy.put("experienceBodyVi", "Đặt tay vào đất sét — cảm nhận hàng trăm năm văn hóa Chăm qua đôi bàn tay của chính bạn. Workshop phù hợp cho cá nhân, gia đình và đoàn tour.");
        copy.put("experienceBodyEn", "Put your hands in the clay and feel centuries of Cham culture. Workshops for individuals, families and tour groups.");
        copy.put("experienceButtonVi", "Đặt Lịch Ngay");
        copy.put("experienceButtonEn", "Book Now");
        copy.put("partnersTitleVi", "Đối Tác Tin Cậy");
        copy.put("partnersTitleEn", "Trusted Partners");
        copy.put("partnersItem1Vi", "Amanoi Resort");
        copy.put("partnersItem1En", "Amanoi Resort");
        copy.put("partnersItem2Vi", "Tour lữ hành địa phương");
        copy.put("partnersItem2En", "Local tour operators");
        copy.put("partnersItem3Vi", "UNESCO");
        copy.put("partnersItem3En", "UNESCO");
        copy.put("socialTitleVi", "Theo Dõi Hành Trình Gốm");
        copy.put("socialTitleEn", "Follow the Pottery Journey");
        copy.put("socialHandle", "@dangxem.baoutruc");
        return Map.copyOf(copy);
    }
}
