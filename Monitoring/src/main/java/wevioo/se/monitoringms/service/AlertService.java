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

    private static final String SEVERITY_CRITICAL = "Critique";
    private static final String SEVERITY_WARNING = "Avertissement";

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
                        .type("Échecs consécutifs")
                        .severity(SEVERITY_CRITICAL)
                        .jobName(jobName)
                        .message(consecutive + " échecs consécutifs détectés sur " + jobName)
                        .triggeredAt(LocalDateTime.now())
                        .resolved(false)
                        .build());
                log.warn("[ALERTE] Échecs consécutifs sur {} : {}", jobName, consecutive);

            } else if (consecutive == 1) {
                String key = "JOB_FAILURE_" + jobName;
                activeAlertsMap.put(key, AlertDto.builder()
                        .type("Échec du traitement")
                        .severity(SEVERITY_WARNING)
                        .jobName(jobName)
                        .message("Dernier run de " + jobName + " en échec")
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
                        .type("Délai SLA dépassé")
                        .severity(lateMinutes > 60 ? SEVERITY_CRITICAL : SEVERITY_WARNING)
                        .jobName(sla.getJobName())
                        .message(sla.getJobName() + " terminé avec " + lateMinutes + " min de retard")
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
                        .type("Écart de réconciliation")
                        .severity(SEVERITY_CRITICAL)
                        .jobName(r.getTableName())
                        .message(String.format(
                                "Table %s : Oracle=%d, SQLServer=%d (taux=%.1f%%)",
                                r.getTableName(), r.getOracleCount(),
                                r.getSqlServerCount(), r.getReconciliationRate()))
                        .triggeredAt(LocalDateTime.now())
                        .resolved(false)
                        .build());
            } else if (SEVERITY_WARNING.equals(r.getStatus())) {
                activeAlertsMap.put(key, AlertDto.builder()
                        .type("Écart de réconciliation")
                        .severity(SEVERITY_WARNING)
                        .jobName(r.getTableName())
                        .message(String.format(
                                "Table %s : taux de réconciliation à %.1f%%",
                                r.getTableName(), r.getReconciliationRate()))
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
                    .type("Modification du schéma")
                    .severity("Information")
                    .jobName("SCHEMA_SOURCE")
                    .message(drifts + " modification(s) de schéma détectée(s) dans les dernières 24h")
                    .triggeredAt(LocalDateTime.now())
                    .resolved(false)
                    .build());
        } else {
            activeAlertsMap.remove(key);
        }
    }
}

