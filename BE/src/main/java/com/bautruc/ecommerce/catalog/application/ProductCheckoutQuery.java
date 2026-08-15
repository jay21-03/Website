package com.bautruc.ecommerce.catalog.application;
import java.time.Instant;
public interface ProductCheckoutQuery { ProductCheckoutView requirePurchasable(Long productId); ProductCheckoutView requirePurchasable(Long productId, Instant pricingAt); }
