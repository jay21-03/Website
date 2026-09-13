package com.bautruc.ecommerce.notification.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.notification.application.AdminNotificationService;
import com.bautruc.ecommerce.notification.infrastructure.SseEmitterRegistry;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class AdminNotificationControllerTest {

    @Test
    void streamDisablesProxyBufferingAndCaching() {
        AdminNotificationService notifications = mock(AdminNotificationService.class);
        SseEmitterRegistry emitters = mock(SseEmitterRegistry.class);
        BusinessClock clock = mock(BusinessClock.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        Instant now = Instant.parse("2026-09-13T10:00:00Z");
        SseEmitter emitter = mock(SseEmitter.class);

        when(notifications.currentAdminId()).thenReturn(7L);
        when(clock.now()).thenReturn(now);
        when(emitters.connect(7L, now)).thenReturn(emitter);

        AdminNotificationController controller =
                new AdminNotificationController(notifications, emitters, clock);

        SseEmitter result = controller.stream(response);

        verify(response).setHeader("X-Accel-Buffering", "no");
        verify(response).setHeader("Cache-Control", "no-cache");
        verify(emitters).connect(7L, now);

        assertThat(result).isSameAs(emitter);
    }
}