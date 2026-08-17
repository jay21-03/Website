package com.bautruc.ecommerce.catalog.application;

import com.bautruc.ecommerce.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public enum ProductSort {
    CREATED_AT_ASC("createdAt", true),
    CREATED_AT_DESC("createdAt", false),
    SELLING_PRICE_ASC("sellingPrice", true),
    SELLING_PRICE_DESC("sellingPrice", false),
    NAME_VI_ASC("nameVi", true),
    NAME_VI_DESC("nameVi", false),
    NAME_EN_ASC("nameEn", true),
    NAME_EN_DESC("nameEn", false);

    private static final String VALIDATION_FAILED = "VALIDATION_FAILED";

    private final String field;
    private final boolean ascending;

    ProductSort(String field, boolean ascending) {
        this.field = field;
        this.ascending = ascending;
    }

    public String field() {
        return field;
    }

    public boolean ascending() {
        return ascending;
    }

    public static ProductSort parse(String value) {
        if (value == null || value.isBlank()) {
            return CREATED_AT_DESC;
        }

        String[] parts = value.trim().split(",", -1);
        if (parts.length != 2) {
            throw invalidSort();
        }

        String field = parts[0].trim();
        String direction = parts[1].trim().toLowerCase(java.util.Locale.ROOT);
        boolean ascending;
        if ("asc".equals(direction)) {
            ascending = true;
        } else if ("desc".equals(direction)) {
            ascending = false;
        } else {
            throw invalidSort();
        }

        for (ProductSort sort : values()) {
            if (sort.field.equals(field) && sort.ascending == ascending) {
                return sort;
            }
        }
        throw invalidSort();
    }

    private static BusinessException invalidSort() {
        return new BusinessException(
                VALIDATION_FAILED,
                "Invalid product sort. Allowed values: createdAt,asc|desc; sellingPrice,asc|desc; nameVi,asc|desc; nameEn,asc|desc.",
                HttpStatus.BAD_REQUEST
        );
    }
}
