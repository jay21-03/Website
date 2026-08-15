package com.bautruc.ecommerce.commerce.application;
public record ProviderCancellation(String providerId,String reason) { public static ProviderCancellation none(){return new ProviderCancellation(null,null);} public boolean required(){return providerId!=null;} }

