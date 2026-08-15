package com.bautruc.ecommerce.reporting.api.response;

public record LowStockProductResponse(Long productId, String productNameVi, String productNameEn,
                                      long availableQuantity, long lowStockThreshold) {}
