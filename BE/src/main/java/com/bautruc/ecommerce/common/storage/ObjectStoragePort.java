package com.bautruc.ecommerce.common.storage;

public interface ObjectStoragePort extends AutoCloseable {
    void put(String objectKey, byte[] content, String contentType);
    void delete(String objectKey);
    String publicUrl(String objectKey);
    @Override default void close() { }
}
