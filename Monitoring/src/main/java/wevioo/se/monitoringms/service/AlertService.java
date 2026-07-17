package wevioo.se.monitoringms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import wevioo.se.monitoringms.dto.AlertDto;
import wevioo.se.monitoringms.dto.ReconciliationDto;
import wevioo.se.monitoringms.dto.SlaStatusDto;
import wevioo.se.monitoringms.repository.BatchExecutionRepository;
import wevioo.se.monitoringms.repository.SchemaRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Moteur d'alertes — évalue les conditions d'alerte et maintient
 * une liste en mémoire des alertes actives non résolues.
 *
 * Types d'alertes :
 *   JOB_FAILURE          → un job vient d'échouer
 *   CONSECUTIVE_FAILURE  → N échecs consécutifs sur le même job
 *   SLA_BREACH           → job terminé après la deadline SLA
 *   RECONCILIATION_KO    → écart Oracle / SQL Server hors tolérance
 *   SCHEMA_DRIFT         → modifications récentes du schéma
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    // Codes de sévérité (la traduction se fait côté client).
    private static final String SEVERITY_CRITICAL = "CRITICAL";
    private static final String SEVERITY_WARNING  = "WARNING";
    private static final String SEVERITY_INFO     = "INFO";

    /** Statut métier (FR) renvoyé par ReconciliationService pour le cas « avertissement ». */
    private static final String RECON_STATUS_WARNING = "Avertissement";

    private final BatchExecutionRepository batchRepo;
    private final ReconciliationService     reconciliationService;
    private final SlaService                slaService;
    private final SchemaRepository schemaRepo;

    @Value("${monitor.alert.consecutive-failure-threshold:3}")
    private int consecutiveFailureThreshold;

    /** Stockage en mémoire des alertes actives (clé = jobName + type). */
    private final Map<String, AlertDto> activeAlertsMap = new ConcurrentHashMap<>();

    // ── Accès public ─────────────────────────────────────────────────────────

    public List<AlertDto> getActiveAlerts() {
        return new ArrayList<>(activeAlertsMap.values());
    }

    public void resolveAlert(String alertKey) {
        activeAlertsMap.computeIfPresent(alertKey, (k, v) -> {
            v.setResolved(true);
            return v;
        });
        activeAlertsMap.remove(alertKey);
    }

    // ── Évaluation périodique ────────────────────────────────────────────────

    /**
     * Évaluation toutes les minutes — rafraîchit les alertes actives.
     */
    @Scheduled(fixedDelayString = "${extractor.scheduler.poll-ms:60000}")
    public void evaluateAlerts() {
        log.debug("Évaluation des alertes...");
        evaluateJobFailures();
        evaluateSlaBreaches();
        evaluateReconciliation();
        evaluateSchemaDrift();
    }

    // ── Évaluation des jobs ──────────────────────────────────────────────────

    private void evaluateJobFailures() {
        List<String> jobNames = List.of("T24", "CUSTOMER.JOB", "AA.BILL.DETAILS.JOB", "FT.HIS.JOB");

        for (String jobName : jobNames) {
            int consecutive = batchRepo.countConsecutiveFailures(jobName);

            if (consecutive >= consecutiveFailureThreshold) {
                String key = "CONSECUTIVE_FAILURE_" + jobName;
                activeAlertsMap.put(key, AlertDto.builder()
                        .alertKey(key)
                        .typeKey("alerts.kind.consecutiveFailure")
                        .severity(SEVERITY_CRITICAL)
                        .jobName(jobName)
                        .messageKey("alerts.msg.consecutiveFailure")
                        .params(Map.of("count", consecutive, "job", jobName))
                        .triggeredAt(LocalDateTime.now())
                        .resolved(false)
                        .build());
                log.warn("[ALERTE] Échecs consécutifs sur {} : {}", jobName, consecutive);

            } else if (consecutive == 1) {
                String key = "JOB_FAILURE_" + jobName;
                activeAlertsMap.put(key, AlertDto.builder()
                        .alertKey(key)
                        .typeKey("alerts.kind.jobFailure")
                        .severity(SEVERITY_WARNING)
                        .jobName(jobName)
                        .messageKey("alerts.msg.jobFailure")
                        .params(Map.of("job", jobName))
                        .triggeredAt(LocalDateTime.now())
                        .resolved(false)
                        .build());
            } else {
                // Succès → nettoyer les alertes liées à ce job
                activeAlertsMap.remove("JOB_FAILURE_" + jobName);
                activeAlertsMap.remove("CONSECUTIVE_FAILURE_" + jobName);
            }
        }
    }

    // ── SLA ──────────────────────────────────────────────────────────────────

    private void evaluateSlaBreaches() {
        for (SlaStatusDto sla : slaService.getAllSlaStatuses()) {
            String key = "SLA_BREACH_" + sla.getJobName();

            if (sla.isSlaBreached()) {
                long lateMinutes = sla.getLateBySeconds() / 60;
                activeAlertsMap.put(key, AlertDto.builder()
                        .alertKey(key)
                        .typeKey("alerts.kind.slaBreach")
                        .severity(lateMinutes > 60 ? SEVERITY_CRITICAL : SEVERITY_WARNING)
                        .jobName(sla.getJobName())
                        .messageKey("alerts.msg.slaBreach")
                        .params(Map.of("job", sla.getJobName(), "minutes", lateMinutes))
                        .triggeredAt(LocalDateTime.now())
                        .resolved(false)
                        .build());
            } else {
                activeAlertsMap.remove(key);
            }
        }
    }

    // ── Réconciliation ───────────────────────────────────────────────────────

    private void evaluateReconciliation() {
        for (ReconciliationDto r : reconciliationService.getLatestReconciliation()) {
            String key = "RECONCILIATION_KO_" + r.getTableName();

            if ("Non conforme".equals(r.getStatus())) {
                activeAlertsMap.put(key, AlertDto.builder()
                        .alertKey(key)
                        .typeKey("alerts.kind.reconciliation")
                        .severity(SEVERITY_CRITICAL)
                        .jobName(r.getTableName())
                        .messageKey("alerts.msg.reconciliationKo")
                        .params(Map.of(
                                "table", r.getTableName(),
                                "oracle", r.getOracleCount(),
                                "sql", r.getSqlServerCount(),
                                "rate", String.format("%.1f", r.getReconciliationRate())))
                        .triggeredAt(LocalDateTime.now())
                        .resolved(false)
                        .build());
            } else if (RECON_STATUS_WARNING.equals(r.getStatus())) {
                activeAlertsMap.put(key, AlertDto.builder()
                        .alertKey(key)
                        .typeKey("alerts.kind.reconciliation")
                        .severity(SEVERITY_WARNING)
                        .jobName(r.getTableName())
                        .messageKey("alerts.msg.reconciliationWarn")
                        .params(Map.of(
                                "table", r.getTableName(),
                                "rate", String.format("%.1f", r.getReconciliationRate())))
                        .triggeredAt(LocalDateTime.now())
                        .resolved(false)
                        .build());
            } else {
                activeAlertsMap.remove(key);
            }
        }
    }

    // ── Schema drift ─────────────────────────────────────────────────────────

    private void evaluateSchemaDrift() {
        int drifts = schemaRepo.countRecentDrifts(1); // dernières 24h
        String key = "SCHEMA_DRIFT";

        if (drifts > 0) {
            activeAlertsMap.put(key, AlertDto.builder()
                    .alertKey(key)
                    .typeKey("alerts.kind.schemaDrift")
                    .severity(SEVERITY_INFO)
                    .jobName("SCHEMA_SOURCE")
                    .messageKey("alerts.msg.schemaDrift")
                    .params(Map.of("count", drifts))
                    .triggeredAt(LocalDateTime.now())
                    .resolved(false)
                    .build());
        } else {
            activeAlertsMap.remove(key);
        }
    }
}

