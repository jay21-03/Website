package com.bautruc.ecommerce.sitecontent.api;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.sitecontent.application.SiteMediaService;
import com.bautruc.ecommerce.sitecontent.application.SiteMediaView;
import com.bautruc.ecommerce.sitecontent.application.SitePresentationService;
import com.bautruc.ecommerce.sitecontent.domain.SiteMediaSlot;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SitePresentationControllerTest {
    @Test
    void acceptsUploadedIconsButNotHomepageMedia() {
        SiteMediaService media = mock(SiteMediaService.class);
        BusinessClock clock = mock(BusinessClock.class);
        when(clock.businessNow()).thenReturn(ZonedDateTime.ofInstant(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")));
        SitePresentationController controller = new SitePresentationController(mock(SitePresentationService.class), media, clock);
        MockMultipartFile file = new MockMultipartFile("file", "icon.png", "image/png", new byte[] {1});

        for (SiteMediaSlot slot : new SiteMediaSlot[] {
                SiteMediaSlot.SITE_CHAT_ICON, SiteMediaSlot.SITE_ZALO_ICON,
                SiteMediaSlot.SITE_FACEBOOK_ICON, SiteMediaSlot.SITE_TRUST_1_ICON,
                SiteMediaSlot.SITE_TRUST_2_ICON, SiteMediaSlot.SITE_TRUST_3_ICON
        }) {
            when(media.upload(slot, file)).thenReturn(new SiteMediaView(slot, "/icon.png", Instant.parse("2026-01-01T00:00:00Z")));
            assertThat(controller.upload(slot, file).data().slot()).isEqualTo(slot);
            verify(media).upload(slot, file);
        }

        assertThatThrownBy(() -> controller.upload(SiteMediaSlot.HOME_HERO, file))
                .isInstanceOf(BusinessException.class);
    }
}
