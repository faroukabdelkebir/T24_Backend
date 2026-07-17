package wevioo.se.monitoringms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import wevioo.se.monitoringms.dto.JobExecutionDto;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Publisher SSE (Server-Sent Events) pour la mise à jour temps-réel du dashboard.
 *
 * Clients supportés : EventSource JavaScript (natif navigateur).
 * Endpoint : GET /api/v1/stream/jobs
 *
 * Événements émis :
 *   - "jobs"    → liste des jobs en cours (toutes les 10 sec)
 *   - "alerts"  → alertes actives (toutes les 30 sec)
 *   - "heartbeat" → keepalive (toutes les 25 sec)
 */
@Service
@Slf4j
public class SsePublisher {

    private final DashboardService dashboardService;
    private final AlertService     alertService;
    private final ObjectMapper objectMapper;

    private final Map<Long, SseEmitter> clients = new ConcurrentHashMap<>();
    private final AtomicLong idSeq   = new AtomicLong();

    public SsePublisher(ObjectMapper objectMapper, DashboardService dashboardService, AlertService alertService) {
        this.objectMapper     = objectMapper;
        this.dashboardService = dashboardService;
        this.alertService     = alertService;
    }

    // ── Abonnement ───────────────────────────────────────────────────────────

    /**
     * Enregistre un nouveau client SSE et retourne son emitter.
     * Timeout = 5 minutes ; le client se reconnecte automatiquement via EventSource.
     */
    public SseEmitter subscribe() {
        long clientId    = idSeq.incrementAndGet();
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        clients.put(clientId, emitter);
        log.info("[SSE] Nouveau client n°{} (total : {})", clientId, clients.size());

        emitter.onCompletion(() -> {
            clients.remove(clientId);
            log.debug("[SSE] Client n°{} déconnecté", clientId);
        });
        emitter.onTimeout(() -> {
            clients.remove(clientId);
            log.debug("[SSE] Délai d'attente dépassé pour le client n°{}", clientId);
        });
        emitter.onError(e -> {
            clients.remove(clientId);
            log.debug("[SSE] Erreur du client n°{} : {}", clientId, e.getMessage());
        });

        // Envoi immédiat du statut initial
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"clientId\":" + clientId + ",\"time\":\"" + LocalDateTime.now() + "\"}"));
        } catch (IOException e) {
            clients.remove(clientId);
        }

        return emitter;
    }

    // ── Broadcast périodique ─────────────────────────────────────────────────

    /** Broadcast des jobs live toutes les 10 secondes. */
    @Scheduled(fixedDelay = 10_000)
    public void broadcastLiveJobs() {
        if (clients.isEmpty()) return;
        try {
            List<JobExecutionDto> liveJobs = dashboardService.getLiveJobs();
            broadcast("jobs", liveJobs);
        } catch (Exception e) {
            log.debug("[SSE] Erreur de diffusion des traitements en cours : {}", e.getMessage());
        }
    }

    /** Broadcast des alertes actives toutes les 30 secondes. */
    @Scheduled(fixedDelay = 30_000)
    public void broadcastAlerts() {
        if (clients.isEmpty()) return;
        try {
            broadcast("alerts", alertService.getActiveAlerts());
        } catch (Exception e) {
            log.debug("[SSE] Erreur de diffusion des alertes : {}", e.getMessage());
        }
    }

    /** Heartbeat toutes les 25 secondes (évite le timeout des proxies). */
    @Scheduled(fixedDelay = 25_000)
    public void heartbeat() {
        if (clients.isEmpty()) return;
        broadcast("heartbeat", Map.of("time", LocalDateTime.now().toString()));
    }

    // ── Broadcast ponctuel (appelé par d'autres services) ───────────────────

    public void publishJobEvent(String eventType, Object data) {
        broadcast(eventType, data);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void broadcast(String eventName, Object data) {
        if (clients.isEmpty()) return;
        String json;
        try {
            json = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("[SSE] Erreur de sérialisation : {}", e.getMessage());
            return;
        }

        List<Long> stale = clients.entrySet().stream()
                .filter(entry -> {
                    try {
                        entry.getValue().send(SseEmitter.event().name(eventName).data(json));
                        return false;
                    } catch (IOException ex) {
                        return true; // client déconnecté
                    }
                })
                .map(Map.Entry::getKey)
                .toList();

        stale.forEach(clients::remove);
        if (!stale.isEmpty()) {
            log.debug("[SSE] Nettoyage de {} client(s) inactif(s)", stale.size());
        }
    }
}

