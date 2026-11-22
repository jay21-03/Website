package com.bautruc.ecommerce.common.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.Arrays;

import org.springframework.web.util.UriUtils;

public final class LocalObjectStorageAdapter implements ObjectStoragePort {
    private final Path root;
    private final String publicBaseUrl;

    public LocalObjectStorageAdapter(Path root, String publicBaseUrl) {
        this.root = Objects.requireNonNull(root).toAbsolutePath().normalize();
        this.publicBaseUrl = stripTrailingSlash(Objects.requireNonNull(publicBaseUrl));
        if (this.publicBaseUrl.isBlank()) {
            throw new IllegalArgumentException("Local object storage public base URL is required");
        }
        try {
            Files.createDirectories(this.root);
        } catch (IOException exception) {
            throw new ObjectStorageException("Could not create local object storage directory", exception);
        }
    }

    @Override
    public void put(String objectKey, byte[] content, String contentType) {
        Path destination = resolve(objectKey);
        try {
            Files.createDirectories(destination.getParent());
            Files.write(destination, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException exception) {
            throw new ObjectStorageException("Could not write local object", exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            Files.deleteIfExists(resolve(objectKey));
        } catch (IOException exception) {
            throw new ObjectStorageException("Could not delete local object", exception);
        }
    }

    @Override
    public String publicUrl(String objectKey) {
        String encodedKey = Arrays.stream(objectKey.split("/", -1))
                .map(segment -> UriUtils.encodePathSegment(segment, StandardCharsets.UTF_8))
                .collect(Collectors.joining("/"));
        return publicBaseUrl + "/" + encodedKey;
    }

    private Path resolve(String objectKey) {
        if (objectKey == null || objectKey.isBlank() || objectKey.startsWith("/")
                || objectKey.contains("\\") || objectKey.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Invalid local object key");
        }
        Path key = Path.of(objectKey);
        if (key.isAbsolute() || Arrays.stream(objectKey.split("/", -1))
                .anyMatch(segment -> segment.isBlank() || segment.equals(".") || segment.equals(".."))) {
            throw new IllegalArgumentException("Invalid local object key");
        }
        Path destination = root.resolve(key).normalize();
        if (!destination.startsWith(root)) {
            throw new IllegalArgumentException("Invalid local object key");
        }
        return destination;
    }

    private static String stripTrailingSlash(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }
}
