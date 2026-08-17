package com.bautruc.ecommerce.catalog.infrastructure;

import com.bautruc.ecommerce.catalog.application.ProductSearchCriteria;
import com.bautruc.ecommerce.catalog.domain.Product;
import org.springframework.data.domain.Page;

public interface ProductSearchRepository {
    Page<Product> searchPublic(ProductSearchCriteria criteria);
}
