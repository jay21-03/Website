package com.bautruc.ecommerce.sitecontent.application;

import com.bautruc.ecommerce.catalog.application.HomepageProductView;

public record FeaturedProductItem(
        short slot,
        HomepageProductView product
) {
}
