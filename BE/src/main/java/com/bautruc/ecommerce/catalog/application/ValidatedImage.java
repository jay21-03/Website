package com.bautruc.ecommerce.catalog.application;

public record ValidatedImage(byte[] content, String contentType, String extension) {
}
