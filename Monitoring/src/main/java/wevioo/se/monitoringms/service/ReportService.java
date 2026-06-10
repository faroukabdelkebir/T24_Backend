package wevioo.se.monitoringms.service;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import wevioo.se.monitoringms.dto.DailyVolumeDto;
import wevioo.se.monitoringms.dto.KpiDto;
import wevioo.se.monitoringms.dto.ReconciliationDto;
import wevioo.se.monitoringms.dto.SlaStatusDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Génération de rapports récapitulatifs.
 *
 * Rapports disponibles :
 *  - Rapport journalier  : KPIs du jour, réconciliation, SLA, alertes
 *  - Rapport hebdomadaire : tendances 7j, jobs les plus lents, taux de succès
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final KpiService            kpiService;
    private final ReconciliationService reconciliationService;
    private final SlaService            slaService;
    private final AlertService          alertService;

    /**
     * Rapport journalier complet — mis en cache 15 minutes.
     */
    @Cacheable(value = "reports", key = "'daily_' + T(java.time.LocalDate).now().toString()")
    public DailyReport generateDailyReport() {
        log.info("Génération du rapport journalier pour le {}", LocalDate.now());

        KpiDto kpis1d = kpiService.computeKpis(1);
        KpiDto kpis7d = kpiService.computeKpis(7);

        return DailyReport.builder()
                .reportDate(LocalDate.now())
                .generatedAt(LocalDateTime.now())
                .kpis1Day(kpis1d)
                .kpis7Days(kpis7d)
                .reconciliations(reconciliationService.getLatestReconciliation())
                .slaStatuses(slaService.getAllSlaStatuses())
                .activeAlerts(alertService.getActiveAlerts().stream()
                        .map(a -> AlertSummary.builder()
                                .type(a.getType())
                                .severity(a.getSeverity())
                                .jobName(a.getJobName())
                                .message(a.getMessage())
                                .build())
                        .toList())
                .overallStatus(computeOverallStatus(kpis1d))
                .build();
    }

    /**
     * Rapport hebdomadaire — tendances et comparaison semaine précédente.
     */
    @Cacheable(value = "reports", key = "'weekly_' + T(java.time.LocalDate).now().with(T(java.time.DayOfWeek).MONDAY).toString()")
    public WeeklyReport generateWeeklyReport() {
        KpiDto kpis7d  = kpiService.computeKpis(7);
        KpiDto kpis14d = kpiService.computeKpis(14);

        // Comparaison semaine N vs N-1
        double successRateDelta = kpis7d.getOverallSuccessRate()
                - computePreviousWeekRate(kpis14d, kpis7d);

        return WeeklyReport.builder()
                .weekStart(LocalDate.now().minusDays(6))
                .weekEnd(LocalDate.now())
                .generatedAt(LocalDateTime.now())
                .kpis(kpis7d)
                .successRateDeltaVsPreviousWeek(Math.round(successRateDelta * 100.0) / 100.0)
                .topVolumeTable(resolveTopVolumeTable(kpis7d))
                .trend(kpis7d.getVolumeTrend())
                .build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String computeOverallStatus(KpiDto kpis) {
        if (kpis.getFailureCount() > 0)            return "Dégradé";
        if (kpis.getOverallSuccessRate() == 100.0) return "Opérationnel";
        if (kpis.getOverallSuccessRate() >= 80.0)  return "Partiel";
        return "Critique";
    }

    private double computePreviousWeekRate(KpiDto kpis14d, KpiDto kpis7d) {
        // Estimation grossière : taux 14j = moyenne des deux semaines
        // On déduit : semaine N-1 ≈ 2 * taux14d - taux7d
        return 2 * kpis14d.getOverallSuccessRate() - kpis7d.getOverallSuccessRate();
    }

    private String resolveTopVolumeTable(KpiDto kpis) {
        return kpis.getRowsByTable().entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse("Non disponible");
    }

    // ── Inner DTOs ────────────────────────────────────────────────────────────

    @Data
    @Builder
    public static class DailyReport {
        private LocalDate                reportDate;
        private LocalDateTime            generatedAt;
        private KpiDto                   kpis1Day;
        private KpiDto                   kpis7Days;
        private List<ReconciliationDto> reconciliations;
        private List<SlaStatusDto>       slaStatuses;
        private List<AlertSummary>       activeAlerts;
        private String                   overallStatus;
    }

    @Data @Builder
    public static class WeeklyReport {
        private LocalDate              weekStart;
        private LocalDate              weekEnd;
        private LocalDateTime          generatedAt;
        private KpiDto kpis;
        private double                 successRateDeltaVsPreviousWeek;
        private String                 topVolumeTable;
        private List<DailyVolumeDto>   trend;
    }

    @Data @Builder
    public static class AlertSummary {
        private String type;
        private String severity;
        private String jobName;
        private String message;
    }
}
