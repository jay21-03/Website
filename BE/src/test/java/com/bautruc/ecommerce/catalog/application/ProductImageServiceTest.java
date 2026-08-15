package com.bautruc.ecommerce.catalog.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bautruc.ecommerce.catalog.infrastructure.ProductImageJpaRepository;
import com.bautruc.ecommerce.common.config.ApplicationProperties;
import com.bautruc.ecommerce.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ProductImageServiceTest {
    private final ProductImageMetadataService metadata = mock(ProductImageMetadataService.class);
    private final ProductImageJpaRepository images = mock(ProductImageJpaRepository.class);
    private final ObjectStoragePort storage = mock(ObjectStoragePort.class);
    private final ProductImageService service = new ProductImageService(metadata, images, storage,
            new ImageSignatureValidator(), properties(1024));

    @Test void compensatesS3ObjectWhenDatabaseFinalizationFails() {
        doThrow(new BusinessException("TEST", "database failure"))
                .when(metadata).finalizeUpload(anyLong(), anyString(), anyString(), anyLong());
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});

        assertThatThrownBy(() -> service.upload(1L, file)).isInstanceOf(BusinessException.class);

        verify(storage).put(anyString(), org.mockito.ArgumentMatchers.any(byte[].class), org.mockito.ArgumentMatchers.eq("image/png"));
        verify(storage).delete(anyString());
    }

    @Test void doesNotWriteMetadataWhenS3UploadFails() {
        doThrow(new ObjectStorageException("down")).when(storage)
                .put(anyString(), org.mockito.ArgumentMatchers.any(byte[].class), anyString());
        MockMultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg",
                new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff});

        assertThatThrownBy(() -> service.upload(1L, file))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> org.assertj.core.api.Assertions.assertThat(error.code()).isEqualTo(CatalogErrorCodes.S3_UPLOAD_FAILED));
        verify(metadata, never()).finalizeUpload(anyLong(), anyString(), anyString(), anyLong());
    }

    @Test void retriesDeleteThreeTimesWithoutRestoringDeletedMetadata() {
        org.mockito.Mockito.when(metadata.delete(1L, 2L)).thenReturn("products/1/image.png");
        doThrow(new ObjectStorageException("down")).when(storage).delete("products/1/image.png");

        service.delete(1L, 2L);

        verify(storage, org.mockito.Mockito.times(3)).delete("products/1/image.png");
    }

    private ApplicationProperties properties(long maxBytes) {
        return new ApplicationProperties(null, null, null, null, null, null, null,
                new ApplicationProperties.Image(maxBytes), null);
    }
}
