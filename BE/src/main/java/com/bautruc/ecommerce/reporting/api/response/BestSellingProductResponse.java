package com.bautruc.ecommerce.reporting.api.response;

public record BestSellingProductResponse(Long productId, String productNameVi, String productNameEn,
                                         long soldQuantity) {}
