package com.bautruc.ecommerce.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class ProductSlugTest {
    @Test void normalizesVietnameseAndKeepsDuplicateNamesUnique() {
        assertThat(ProductSlug.fromName("Bình gốm Đàng Xem!", 4927)).isEqualTo("binh-gom-dang-xem-4927");
        assertThat(ProductSlug.fromName("Bình gốm Đàng Xem!", 4928)).isEqualTo("binh-gom-dang-xem-4928");
        assertThat(ProductSlug.fromName("###", 4)).isEqualTo("san-pham-4");
    }
}
