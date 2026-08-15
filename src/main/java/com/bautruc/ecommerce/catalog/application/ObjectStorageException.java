package com.bautruc.ecommerce.catalog.application;

public class ObjectStorageException extends RuntimeException {
    public ObjectStorageException(String message) { super(message); }
    public ObjectStorageException(String message, Throwable cause) { super(message, cause); }
}
