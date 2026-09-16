package com.bautruc.ecommerce.sitecontent.domain;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "homepage_featured_products")
public class HomepageFeaturedProduct {
    @Id
    private Short slot;

    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected HomepageFeaturedProduct() {
    }

    public HomepageFeaturedProduct(short slot, Long productId, Instant now) {
        if (slot < 1 || slot > 3) throw new IllegalArgumentException("slot must be between 1 and 3");
        if (productId == null || now == null) throw new IllegalArgumentException("productId and now are required");
        this.slot = slot;
        this.productId = productId;
        this.updatedAt = now;
    }

    public Short getSlot() { return slot; }
    public Long getProductId() { return productId; }
    public Instant getUpdatedAt() { return updatedAt; }
}
