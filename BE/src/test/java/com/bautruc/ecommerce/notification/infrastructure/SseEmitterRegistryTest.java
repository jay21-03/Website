package com.bautruc.ecommerce.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class SseEmitterRegistryTest {

    private static final Instant NOW = Instant.parse("2026-09-13T10:00:00Z");

    @Test
    @DisplayName("connect registers emitter and sends initial connected event")
    void connectRegistersEmitterAndSendsConnectedEvent() throws Exception {
        SseEmitterRegistry registry = new SseEmitterRegistry(60000L);
        SseEmitter mockEmitter = mock(SseEmitter.class);

        SseEmitter result = registry.connect(1L, mockEmitter, NOW);

        assertThat(result).isSameAs(mockEmitter);
        assertThat(registry.connectionCount(1L)).isEqualTo(1);

        ArgumentCaptor<SseEmitter.SseEventBuilder> captor = ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        verify(mockEmitter).send(captor.capture());

        Set<ResponseBodyEmitter.DataWithMediaType> data = captor.getValue().build();
        String formatted = data.stream().map(d -> String.valueOf(d.getData())).reduce("", (a, b) -> a + b);
        assertThat(formatted).contains("connected");
        assertThat(formatted).contains("2026-09-13T10:00:00Z");
    }

    @Test
    @DisplayName("default connect creates a real SseEmitter and registers it")
    void defaultConnectCreatesRealEmitter() {
        SseEmitterRegistry registry = new SseEmitterRegistry(60000L);

        SseEmitter emitter = registry.connect(1L, NOW);

        assertThat(emitter).isNotNull();
        assertThat(registry.connectionCount(1L)).isEqualTo(1);
    }

    @Test
    @DisplayName("sendHeartbeats sends comment-based heartbeat to all active emitters")
    void sendHeartbeatsSendsHeartbeatCommentToActiveEmitters() throws Exception {
        SseEmitterRegistry registry = new SseEmitterRegistry(60000L);
        SseEmitter emitter1 = mock(SseEmitter.class);
        SseEmitter emitter2 = mock(SseEmitter.class);

        registry.connect(1L, emitter1, NOW);
        registry.connect(2L, emitter2, NOW);
        clearInvocations(emitter1, emitter2);

        registry.sendHeartbeats();

        ArgumentCaptor<SseEmitter.SseEventBuilder> captor1 = ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        ArgumentCaptor<SseEmitter.SseEventBuilder> captor2 = ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        verify(emitter1).send(captor1.capture());
        verify(emitter2).send(captor2.capture());

        String formatted1 = captor1.getValue().build().stream().map(d -> String.valueOf(d.getData())).reduce("", (a, b) -> a + b);
        String formatted2 = captor2.getValue().build().stream().map(d -> String.valueOf(d.getData())).reduce("", (a, b) -> a + b);
        assertThat(formatted1).contains("heartbeat");
        assertThat(formatted2).contains("heartbeat");
        assertThat(registry.connectionCount(1L)).isEqualTo(1);
        assertThat(registry.connectionCount(2L)).isEqualTo(1);
    }

    @Test
    @DisplayName("heartbeat failure cleans up failed emitter and stops sending to it")
    void heartbeatFailureRemovesEmitterAndStopsSendingHeartbeats() throws Exception {
        SseEmitterRegistry registry = new SseEmitterRegistry(60000L);
        SseEmitter failingEmitter = mock(SseEmitter.class);
        SseEmitter healthyEmitter = mock(SseEmitter.class);

        registry.connect(1L, failingEmitter, NOW);
        registry.connect(2L, healthyEmitter, NOW);
        clearInvocations(failingEmitter, healthyEmitter);

        doThrow(new IOException("Broken pipe")).when(failingEmitter).send(any(SseEmitter.SseEventBuilder.class));

        registry.sendHeartbeats();

        verify(failingEmitter).completeWithError(any(IOException.class));
        assertThat(registry.connectionCount(1L)).isEqualTo(0);
        assertThat(registry.connectionCount(2L)).isEqualTo(1);

        clearInvocations(failingEmitter, healthyEmitter);
        registry.sendHeartbeats();

        verifyNoInteractions(failingEmitter);
        verify(healthyEmitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("send delivers normal business notifications to active emitter")
    void sendDeliversNormalNotificationToConnectedAdmin() throws Exception {
        SseEmitterRegistry registry = new SseEmitterRegistry(60000L);
        SseEmitter emitter = mock(SseEmitter.class);

        registry.connect(1L, emitter, NOW);
        clearInvocations(emitter);

        Map<String, Object> payload = Map.of("notificationId", 123L, "type", "ORDER_CREATED");
        registry.send(1L, "notification", payload);

        ArgumentCaptor<SseEmitter.SseEventBuilder> captor = ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        verify(emitter).send(captor.capture());
        String formatted = captor.getValue().build().stream().map(d -> String.valueOf(d.getData())).reduce("", (a, b) -> a + b);
        assertThat(formatted).contains("notification");
        assertThat(formatted).contains("ORDER_CREATED");
    }

    @Test
    @DisplayName("send failure cleans up broken emitter")
    void sendFailureCleansUpBrokenEmitter() throws Exception {
        SseEmitterRegistry registry = new SseEmitterRegistry(60000L);
        SseEmitter failingEmitter = mock(SseEmitter.class);

        registry.connect(1L, failingEmitter, NOW);
        clearInvocations(failingEmitter);

        doThrow(new IOException("Connection reset")).when(failingEmitter).send(any(SseEmitter.SseEventBuilder.class));

        registry.send(1L, "notification", Map.of("id", 1L));

        verify(failingEmitter).completeWithError(any(IOException.class));
        assertThat(registry.connectionCount(1L)).isEqualTo(0);
    }

    @Test
    @DisplayName("sendHeartbeats safely handles empty registry")
    void sendHeartbeatsSafelyHandlesEmptyRegistry() {
        SseEmitterRegistry registry = new SseEmitterRegistry(60000L);
        registry.sendHeartbeats();
        assertThat(registry.connectionCount(1L)).isEqualTo(0);
    }
}
