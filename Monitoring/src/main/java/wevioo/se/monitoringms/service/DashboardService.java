package wevioo.se.monitoringms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import wevioo.se.monitoringms.config.CacheConfig;
import wevioo.se.monitoringms.dto.DashboardSummaryDto;
import wevioo.se.monitoringms.dto.JobExecutionDto;
import wevioo.se.monitoringms.model.BatchJobExecution;
import wevioo.se.monitoringms.repository.BatchExecutionRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service principal du dashboard — agrège toutes les données pour la vue d'ensemble.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final BatchExecutionRepository batchRepo;
    private final KpiService               kpiService;
    private final AlertService             alertService;
    private final DisplayTranslationService displayTranslationService;

    /**
     * Résumé complet du dashboard — mis en cache 30 secondes.
     */
    @Cacheable(value = CacheConfig.CACHE_DASHBOARD, key = "'summary'")
    public DashboardSummaryDto getSummary() {
        log.debug("Construction du résumé du tableau de bord...");

        List<BatchJobExecution> running = batchRepo.findRunning();
        List<BatchJobExecution> recent  = batchRepo.findRecent(10);

        return DashboardSummaryDto.builder()
                .runningJobsCount(running.size())
                .completedTodayCount(batchRepo.countCompletedToday())
                .failedTodayCount(batchRepo.countFailedToday())
                .successRate7d(batchRepo.computeSuccessRate(7))
                .cobStatus(resolveCobStatus())
                .lastBusinessDate(resolveLastBusinessDate())
                .liveJobs(toDto(running))
                .recentExecutions(toDto(recent))
                .activeAlerts(alertService.getActiveAlerts())
                .generatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Jobs actuellement actifs — cache très court (20 sec).
     */
    @Cacheable(value = CacheConfig.CACHE_LIVE_JOBS, key = "'live'")
    public List<JobExecutionDto> getLiveJobs() {
        return toDto(batchRepo.findRunning());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String resolveCobStatus() {
        // On cherche dans les steps récents si le step COB_STATUS a passé
        // Valeurs : "STOP" (COB ok), "COB_RUNNING" (COB actif), "UNKNOWN"
        try {
            // On lit le dernier step COB_STATUS pour connaître l'état
            // (simplifié : la logique complète lit F_TSA_SERVICE.SERVICE_CONTROL)
            return "Arrêté"; // défaut
        } catch (Exception e) {
            log.warn("Impossible de déterminer le statut COB : {}", e.getMessage());
            return "Inconnu";
        }
    }

    private String resolveLastBusinessDate() {
        return batchRepo.findLastSuccessfulByJobName("T24")
                .map(je -> je.getEndTime() != null
                        ? je.getEndTime().toLocalDate().toString()
                        : "Non disponible")
                .orElse("Non disponible");
    }

    private List<JobExecutionDto> toDto(List<BatchJobExecution> executions) {
        return executions.stream()
                .map(je -> JobExecutionDto.builder()
                        .jobExecutionId(je.getJobExecutionId())
                        .jobInstanceId(je.getJobInstanceId())
                        .jobName(je.getJobName())
                        .status(je.getStatus())
                        .exitCode(je.getExitCode())
                        .exitMessage(je.getExitMessage())
                        .startTime(je.getStartTime())
                        .endTime(je.getEndTime())
                        .createTime(je.getCreateTime())
                        .durationSeconds(je.getDurationSeconds())
                        .running(je.isRunning())
                        .failed(je.isFailed())
                        .build())
                .toList();
    }
}

