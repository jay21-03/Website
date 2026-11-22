package com.bautruc.ecommerce.common.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalObjectStorageAdapterTest {
    @TempDir
    Path root;

    @Test
    void storesAndDeletesObjectsUnderTheirKeys() throws Exception {
        LocalObjectStorageAdapter storage = new LocalObjectStorageAdapter(root.resolve("objects"), "http://localhost:8080/local-media/");

        storage.put("products/17/pottery image.jpg", new byte[] {1, 2, 3}, "image/jpeg");

        assertThat(Files.readAllBytes(root.resolve("objects/products/17/pottery image.jpg")))
                .containsExactly((byte) 1, (byte) 2, (byte) 3);
        assertThat(storage.publicUrl("products/17/pottery image.jpg"))
                .isEqualTo("http://localhost:8080/local-media/products/17/pottery%20image.jpg");

        storage.delete("products/17/pottery image.jpg");

        assertThat(Files.exists(root.resolve("objects/products/17/pottery image.jpg"))).isFalse();
    }

    @Test
    void rejectsObjectKeysThatEscapeTheStorageRoot() {
        LocalObjectStorageAdapter storage = new LocalObjectStorageAdapter(root.resolve("objects"), "http://localhost:8080/local-media");

        assertThatThrownBy(() -> storage.put("../outside.txt", new byte[] {1}, "text/plain"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.delete("products\\outside.txt"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
