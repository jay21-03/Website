package com.bautruc.ecommerce.catalog.infrastructure;

import java.time.Instant;
import java.util.Locale;

import com.bautruc.ecommerce.catalog.application.ProductSearchCriteria;
import com.bautruc.ecommerce.catalog.application.ProductSort;
import com.bautruc.ecommerce.catalog.domain.Product;
import com.bautruc.ecommerce.common.response.PageResponse;
import com.bautruc.ecommerce.common.time.BusinessClock;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class ProductSearchRepositoryImpl implements ProductSearchRepository {
    private static final String EFFECTIVE_PRICE_SQL = """
            CASE
                WHEN d.id IS NOT NULL
                 AND d.is_active = TRUE
                 AND :pricingAt >= d.start_at
                 AND :pricingAt <= d.end_at
                    THEN CASE
                        WHEN d.discount_type = 'FIXED_PRICE'
                            THEN CAST(d.discount_value AS BIGINT)
                        WHEN d.discount_type = 'PERCENTAGE'
                            THEN CAST(ROUND((CAST(p.base_price AS NUMERIC) - (CAST(p.base_price AS NUMERIC) * d.discount_value / 100)), 0) AS BIGINT)
                        ELSE p.base_price
                    END
                ELSE p.base_price
            END
            """;

    private final EntityManager entityManager;
    private final BusinessClock clock;

    public ProductSearchRepositoryImpl(EntityManager entityManager, BusinessClock clock) {
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Override
    public Page<Product> searchPublic(ProductSearchCriteria criteria) {
        int page = Math.max(PageResponse.DEFAULT_PAGE, criteria.page());
        int size = Math.min(Math.max(1, criteria.size()), PageResponse.MAX_SIZE);
        Instant pricingAt = clock.now();

        String fromWhere = fromWhere(criteria);
        String orderBy = orderBy(criteria.sort());

        Query dataQuery = entityManager.createNativeQuery("""
                SELECT p.*
                """ + fromWhere + "\n" + orderBy, Product.class);
        bind(dataQuery, criteria, pricingAt, requiresPricingAt(criteria) || isSellingPriceSort(criteria.sort()));
        dataQuery.setFirstResult(page * size);
        dataQuery.setMaxResults(size);

        Query countQuery = entityManager.createNativeQuery("SELECT COUNT(*) " + fromWhere);
        bind(countQuery, criteria, pricingAt, requiresPricingAt(criteria));

        @SuppressWarnings("unchecked")
        java.util.List<Product> content = dataQuery.getResultList();
        long total = ((Number) countQuery.getSingleResult()).longValue();

        return new PageImpl<>(content, PageRequest.of(page, size), total);
    }

    private String fromWhere(ProductSearchCriteria criteria) {
        StringBuilder sql = new StringBuilder("""
                 FROM products p
                 LEFT JOIN discounts d ON d.product_id = p.id
                 WHERE p.status = 'ACTIVE'
                   AND p.deleted_at IS NULL
                """);

        if (criteria.keyword() != null && !criteria.keyword().isBlank()) {
            sql.append(" AND (LOWER(p.name_vi) LIKE :keyword OR LOWER(p.name_en) LIKE :keyword)\n");
        }
        if (criteria.collectionId() != null) {
            sql.append(" AND p.collection_id = :collectionId\n");
        }
        if (criteria.minPrice() != null) {
            sql.append(" AND ").append(EFFECTIVE_PRICE_SQL).append(" >= :minPrice\n");
        }
        if (criteria.maxPrice() != null) {
            sql.append(" AND ").append(EFFECTIVE_PRICE_SQL).append(" <= :maxPrice\n");
        }
        return sql.toString();
    }

    private String orderBy(ProductSort sort) {
        ProductSort effectiveSort = sort == null ? ProductSort.CREATED_AT_DESC : sort;
        String direction = effectiveSort.ascending() ? "ASC" : "DESC";
        return switch (effectiveSort) {
            case CREATED_AT_ASC, CREATED_AT_DESC -> "ORDER BY p.created_at " + direction + ", p.id " + direction;
            case SELLING_PRICE_ASC, SELLING_PRICE_DESC -> "ORDER BY " + EFFECTIVE_PRICE_SQL + " " + direction + ", p.id " + direction;
            case NAME_VI_ASC, NAME_VI_DESC -> "ORDER BY LOWER(p.name_vi) " + direction + ", p.id " + direction;
            case NAME_EN_ASC, NAME_EN_DESC -> "ORDER BY LOWER(p.name_en) " + direction + ", p.id " + direction;
        };
    }

    private void bind(Query query, ProductSearchCriteria criteria, Instant pricingAt, boolean bindPricingAt) {
        if (bindPricingAt) {
            query.setParameter("pricingAt", pricingAt);
        }
        if (criteria.keyword() != null && !criteria.keyword().isBlank()) {
            query.setParameter("keyword", "%" + criteria.keyword().trim().toLowerCase(Locale.ROOT) + "%");
        }
        if (criteria.collectionId() != null) {
            query.setParameter("collectionId", criteria.collectionId());
        }
        if (criteria.minPrice() != null) {
            query.setParameter("minPrice", criteria.minPrice());
        }
        if (criteria.maxPrice() != null) {
            query.setParameter("maxPrice", criteria.maxPrice());
        }
    }

    private boolean requiresPricingAt(ProductSearchCriteria criteria) {
        return criteria.minPrice() != null || criteria.maxPrice() != null;
    }

    private boolean isSellingPriceSort(ProductSort sort) {
        return sort == ProductSort.SELLING_PRICE_ASC || sort == ProductSort.SELLING_PRICE_DESC;
    }
}
