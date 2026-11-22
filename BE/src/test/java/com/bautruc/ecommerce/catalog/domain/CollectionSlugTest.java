package com.bautruc.ecommerce.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class CollectionSlugTest {
    @Test
    void mapsApprovedProductionCollectionsAndKeepsOtherSlugsUnique() {
        assertThat(CollectionSlug.fromName("Tiểu cảnh & gốm sân vườn", 4401)).isEqualTo("tieu-canh-gom-san-vuon");
        assertThat(CollectionSlug.fromName("Phù điêu & mặt nạ gốm", 4351)).isEqualTo("phu-dieu-mat-na-gom");
        assertThat(CollectionSlug.fromName("Bộ sưu tập khác", 77)).isEqualTo("bo-suu-tap-khac-77");
    }
}
