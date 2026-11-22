package com.bautruc.ecommerce.catalog.domain;

import java.text.Normalizer;
import java.util.Locale;

public final class ProductSlug {
    private ProductSlug() {}

    public static String fromName(String nameVi, long id) {
        String base = Normalizer.normalize(nameVi.toLowerCase(Locale.ROOT).replace('đ', 'd'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        return (base.isEmpty() ? "san-pham" : base) + "-" + id;
    }
}
