package com.bautruc.ecommerce.catalog.infrastructure;

import java.util.Objects;

import com.bautruc.ecommerce.catalog.application.ObjectStorageException;
import com.bautruc.ecommerce.catalog.application.ObjectStoragePort;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3ObjectStorageAdapter implements ObjectStoragePort {
    private static final String CACHE_CONTROL = "public, max-age=31536000, immutable";
    private final S3Client client;
    private final String bucketName;
    private final String publicBaseUrl;

    public S3ObjectStorageAdapter(S3Client client, String bucketName, String publicBaseUrl) {
        this.client = Objects.requireNonNull(client);
        this.bucketName = requireConfiguration(bucketName, "S3 bucket name");
        this.publicBaseUrl = stripTrailingSlash(requireConfiguration(publicBaseUrl, "S3 public base URL"));
    }

    @Override
    public void put(String objectKey, byte[] content, String contentType) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(contentType)
                    .contentLength((long) content.length)
                    .cacheControl(CACHE_CONTROL)
                    .build();
            client.putObject(request, RequestBody.fromBytes(content));
        } catch (RuntimeException exception) {
            throw new ObjectStorageException("Could not upload object to S3", exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(objectKey).build());
        } catch (RuntimeException exception) {
            throw new ObjectStorageException("Could not delete object from S3", exception);
        }
    }

    @Override
    public String publicUrl(String objectKey) {
        return publicBaseUrl + "/" + objectKey;
    }

    private static String requireConfiguration(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value.trim();
    }

    private static String stripTrailingSlash(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') end--;
        return value.substring(0, end);
    }
}
