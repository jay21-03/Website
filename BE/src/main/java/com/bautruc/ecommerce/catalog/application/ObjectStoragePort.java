package com.bautruc.ecommerce.catalog.application;

public interface ObjectStoragePort extends AutoCloseable {
    void put(String objectKey, byte[] content, String contentType);
    void delete(String objectKey);
    String publicUrl(String objectKey);
    @Override default void close() { }
}
