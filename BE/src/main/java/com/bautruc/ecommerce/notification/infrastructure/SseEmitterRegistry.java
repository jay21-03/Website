package com.bautruc.ecommerce.notification.infrastructure;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SseEmitterRegistry {
    private static final Logger log = LoggerFactory.getLogger(SseEmitterRegistry.class);
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final long timeoutMs;

    public SseEmitterRegistry(@Value("${bautruc.notification.sse-timeout-ms:1800000}") long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public SseEmitter connect(Long adminId, Instant now) {
        return connect(adminId, new SseEmitter(timeoutMs), now);
    }

    SseEmitter connect(Long adminId, SseEmitter emitter, Instant now) {
        emitters.computeIfAbsent(adminId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(adminId, emitter));
        emitter.onTimeout(() -> remove(adminId, emitter));
        emitter.onError(error -> remove(adminId, emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data(Map.of("connectedAt", now.toString())));
        } catch (IOException | IllegalStateException exception) {
            remove(adminId, emitter);
            try {
                emitter.completeWithError(exception);
            } catch (RuntimeException ignored) {
            }
        }
        return emitter;
    }

    public void send(Long adminId, String eventName, Object data) {
        CopyOnWriteArrayList<SseEmitter> connections = emitters.get(adminId);
        if (connections == null) return;
        for (SseEmitter emitter : connections) {
            try { emitter.send(SseEmitter.event().name(eventName).data(data)); }
            catch (IOException | IllegalStateException exception) {
                log.debug("Removing failed notification SSE emitter for adminId={}", adminId, exception);
                remove(adminId, emitter);
                try { emitter.completeWithError(exception); } catch (RuntimeException ignored) { }
            }
        }
    }

    @Scheduled(
            fixedDelayString = "${bautruc.notification.sse-heartbeat-ms:15000}",
            scheduler = "sseHeartbeatScheduler"
    )
    public void sendHeartbeats() {
        if (emitters.isEmpty()) {
            return;
        }
        for (Map.Entry<Long, CopyOnWriteArrayList<SseEmitter>> entry : emitters.entrySet()) {
            Long adminId = entry.getKey();
            CopyOnWriteArrayList<SseEmitter> connections = entry.getValue();
            if (connections == null) {
                continue;
            }
            for (SseEmitter emitter : connections) {
                try {
                    emitter.send(SseEmitter.event().comment("heartbeat"));
                } catch (IOException | IllegalStateException exception) {
                    log.debug("Removing failed notification SSE emitter during heartbeat for adminId={}", adminId, exception);
                    remove(adminId, emitter);
                    try {
                        emitter.completeWithError(exception);
                    } catch (RuntimeException ignored) {
                    }
                }
            }
        }
    }

    int connectionCount(Long adminId) {
        CopyOnWriteArrayList<SseEmitter> connections = emitters.get(adminId);
        return connections == null ? 0 : connections.size();
    }

    private void remove(Long adminId, SseEmitter emitter) {
        emitters.computeIfPresent(adminId, (id, list) -> { list.remove(emitter); return list.isEmpty() ? null : list; });
    }
}
