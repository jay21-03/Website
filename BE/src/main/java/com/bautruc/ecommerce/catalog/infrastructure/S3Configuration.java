package com.bautruc.ecommerce.catalog.infrastructure;

import com.bautruc.ecommerce.catalog.application.ObjectStoragePort;
import com.bautruc.ecommerce.common.config.ApplicationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Configuration {
    @Bean(destroyMethod = "close")
    ObjectStoragePort objectStoragePort(ApplicationProperties properties) {
        ApplicationProperties.Aws aws = properties.aws();
        ApplicationProperties.S3 s3 = aws == null ? null : aws.s3();
        if (aws == null || blank(aws.region()) || s3 == null || blank(s3.bucketName()) || blank(s3.publicBaseUrl())) {
            return new UnavailableObjectStorageAdapter();
        }
        S3Client client = S3Client.builder().region(Region.of(aws.region().trim())).build();
        return new CloseableS3ObjectStorageAdapter(client, s3.bucketName(), s3.publicBaseUrl());
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }

    private static final class CloseableS3ObjectStorageAdapter extends S3ObjectStorageAdapter implements AutoCloseable {
        private final S3Client client;
        private CloseableS3ObjectStorageAdapter(S3Client client, String bucketName, String publicBaseUrl) {
            super(client, bucketName, publicBaseUrl);
            this.client = client;
        }
        @Override public void close() { client.close(); }
    }
}
