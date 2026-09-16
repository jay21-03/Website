package com.bautruc.ecommerce.common.storage;


final class UnavailableObjectStorageAdapter implements ObjectStoragePort {
    private static final String MESSAGE = "S3 object storage is not configured";
    @Override public void put(String objectKey, byte[] content, String contentType) { throw new ObjectStorageException(MESSAGE); }
    @Override public void delete(String objectKey) { throw new ObjectStorageException(MESSAGE); }
    @Override public String publicUrl(String objectKey) { return null; }
}
