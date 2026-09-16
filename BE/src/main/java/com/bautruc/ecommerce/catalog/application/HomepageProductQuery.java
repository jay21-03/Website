package com.bautruc.ecommerce.catalog.application;

import java.util.Collection;
import java.util.List;

public interface HomepageProductQuery {
    List<HomepageProductView> publicProductsByIds(Collection<Long> productIds);
}
