package com.bautruc.ecommerce.catalog.domain;

import java.util.Map;

public final class CollectionSlug {
    private CollectionSlug() {}

    public static String fromName(String nameVi, long id) {
        String normalized = ProductSlug.fromName(nameVi, id).replaceFirst("-" + id + "$", "");
        return Map.of(
                "binh-gom-trang-tri", "binh-gom-trang-tri",
                "tuong-gom-nghe-thuat", "tuong-gom-nghe-thuat",
                "phu-dieu-mat-na-gom", "phu-dieu-mat-na-gom",
                "mo-hinh-thap-cham", "mo-hinh-thap-cham",
                "tieu-canh-gom-san-vuon", "tieu-canh-gom-san-vuon",
                "do-gom-gia-dung", "do-gom-gia-dung"
        ).getOrDefault(normalized, normalized + "-" + id);
    }
}
