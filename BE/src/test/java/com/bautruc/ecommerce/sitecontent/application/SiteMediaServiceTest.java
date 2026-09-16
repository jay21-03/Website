package com.bautruc.ecommerce.sitecontent.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import com.bautruc.ecommerce.common.config.ApplicationProperties;
import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.common.storage.ObjectStorageException;
import com.bautruc.ecommerce.common.storage.ObjectStoragePort;
import com.bautruc.ecommerce.sitecontent.domain.SiteMediaSlot;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.mock.web.MockMultipartFile;

class SiteMediaServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-16T07:00:00Z");

    private final SiteMediaMetadataService metadata = mock(SiteMediaMetadataService.class);
    private final ObjectStoragePort storage = mock(ObjectStoragePort.class);
    private final SiteMediaService service = new SiteMediaService(
            metadata,
            storage,
            new SiteMediaImageValidator(),
            properties(1024)
    );

    @Test
    void uploadsNewObjectCommitsMetadataThenDeletesPreviousObject() {
        when(metadata.replace(
                org.mockito.ArgumentMatchers.eq(SiteMediaSlot.HOME_HERO),
                anyString(),
                org.mockito.ArgumentMatchers.eq("image/png"),
                org.mockito.ArgumentMatchers.eq(8L)
        )).thenAnswer(invocation -> new SiteMediaMetadataChange(
                SiteMediaSlot.HOME_HERO,
                "site/home/hero/old.png",
                invocation.getArgument(1),
                NOW
        ));
        when(storage.publicUrl(anyString())).thenAnswer(invocation -> "https://cdn.example/" + invocation.getArgument(0));

        SiteMediaView result = service.upload(SiteMediaSlot.HOME_HERO, png());

        assertThat(result.slot()).isEqualTo(SiteMediaSlot.HOME_HERO);
        assertThat(result.url()).startsWith("https://cdn.example/site/home/hero/");
        assertThat(result.updatedAt()).isEqualTo(NOW);

        InOrder order = inOrder(storage, metadata);
        order.verify(storage).put(anyString(), org.mockito.ArgumentMatchers.any(byte[].class),
                org.mockito.ArgumentMatchers.eq("image/png"));
        order.verify(metadata).replace(
                org.mockito.ArgumentMatchers.eq(SiteMediaSlot.HOME_HERO),
                anyString(),
                org.mockito.ArgumentMatchers.eq("image/png"),
                org.mockito.ArgumentMatchers.eq(8L)
        );
        order.verify(storage).delete("site/home/hero/old.png");
    }

    @Test
    void compensatesUploadedObjectWhenMetadataCommitFails() {
        doThrow(new BusinessException("TEST", "database failure"))
                .when(metadata)
                .replace(
                        org.mockito.ArgumentMatchers.eq(SiteMediaSlot.HOME_HERO),
                        anyString(),
                        org.mockito.ArgumentMatchers.eq("image/png"),
                        org.mockito.ArgumentMatchers.eq(8L)
                );

        assertThatThrownBy(() -> service.upload(SiteMediaSlot.HOME_HERO, png()))
                .isInstanceOf(BusinessException.class);

        verify(storage).put(anyString(), org.mockito.ArgumentMatchers.any(byte[].class),
                org.mockito.ArgumentMatchers.eq("image/png"));
        verify(storage).delete(anyString());
    }

    @Test
    void doesNotChangeMetadataWhenStorageUploadFails() {
        doThrow(new ObjectStorageException("down"))
                .when(storage)
                .put(anyString(), org.mockito.ArgumentMatchers.any(byte[].class),
                        org.mockito.ArgumentMatchers.eq("image/png"));

        assertThatThrownBy(() -> service.upload(SiteMediaSlot.HOME_HERO, png()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.code()).isEqualTo(
                                SiteContentErrorCodes.HOMEPAGE_MEDIA_STORAGE_UPLOAD_FAILED
                        ));

        verify(metadata, never()).replace(
                org.mockito.ArgumentMatchers.any(),
                anyString(),
                anyString(),
                org.mockito.ArgumentMatchers.anyLong()
        );
    }

    @Test
    void clearCommitsMetadataThenDeletesPreviousObject() {
        when(metadata.clear(SiteMediaSlot.HOME_STORY)).thenReturn(new SiteMediaMetadataChange(
                SiteMediaSlot.HOME_STORY,
                "site/home/story/old.jpg",
                null,
                NOW
        ));

        SiteMediaView result = service.clear(SiteMediaSlot.HOME_STORY);

        assertThat(result.url()).isNull();
        InOrder order = inOrder(metadata, storage);
        order.verify(metadata).clear(SiteMediaSlot.HOME_STORY);
        order.verify(storage).delete("site/home/story/old.jpg");
    }

    @Test
    void rejectsImageLargerThanConfiguredLimitBeforeStorageWrite() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.png",
                "image/png",
                new byte[1025]
        );

        assertThatThrownBy(() -> service.upload(SiteMediaSlot.HOME_HERO, file))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.code()).isEqualTo(SiteContentErrorCodes.HOMEPAGE_MEDIA_TOO_LARGE));

        verify(storage, never()).put(anyString(), org.mockito.ArgumentMatchers.any(byte[].class), anyString());
    }

    private MockMultipartFile png() {
        return new MockMultipartFile(
                "file",
                "hero.png",
                "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a}
        );
    }

    private ApplicationProperties properties(long maxBytes) {
        return new ApplicationProperties(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new ApplicationProperties.Image(maxBytes),
                null
        );
    }
}
