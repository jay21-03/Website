package com.bautruc.ecommerce.sitecontent.api.response;

import com.bautruc.ecommerce.catalog.application.HomepageProductView;
import com.bautruc.ecommerce.sitecontent.application.FeaturedProductItem;

public record HomepageFeaturedProductResponse(
        short slot,
        Long id,
        String nameVi,
        String nameEn,
        String descriptionVi,
        String descriptionEn,
        long basePrice,
        long sellingPrice,
        String thumbnailUrl
) {
    public static HomepageFeaturedProductResponse from(FeaturedProductItem item) {
        HomepageProductView product = item.product();
        return new HomepageFeaturedProductResponse(
                item.slot(),
                product.id(),
                product.nameVi(),
                product.nameEn(),
                product.descriptionVi(),
                product.descriptionEn(),
                product.basePrice(),
                product.sellingPrice(),
                product.thumbnailUrl()
        );
    }
}
