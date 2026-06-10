package wevioo.se.monitoringms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import wevioo.se.monitoringms.config.CacheConfig;
import wevioo.se.monitoringms.dto.DailyVolumeDto;
import wevioo.se.monitoringms.dto.KpiDto;
import wevioo.se.monitoringms.repository.BatchExecutionRepository;
import wevioo.se.monitoringms.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Calcul de tous les KPIs — mis en cache 2 minutes.
 *
 * KPIs produits :
 *  - Taux de succès global et par job
 *  - Volume extrait par table
 *  - Débit moyen (rows/sec)
 *  - Durée moyenne par job
 *  - Tendance quotidienne (N derniers jours)
 *  - Compteur d'échecs consécutifs
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KpiService {

    private final BatchExecutionRepository batchRepo;
    private final StatsRepository statsRepo;

    /**
     * KPIs complets sur la fenêtre demandée.
     * @param windowDays  nombre de jours d'historique (ex : 7, 30)
     */
    @Cacheable(value = CacheConfig.CACHE_KPIS, key = "#windowDays")
    public KpiDto computeKpis(int windowDays) {
        log.debug("Calcul des indicateurs sur une période de {} jours", windowDays);

        List<BatchExecutionRepository.JobAggregate> jobAggs    = batchRepo.computeJobAggregates(windowDays);
        List<BatchExecutionRepository.DailyAggregate> dailyAggs  = batchRepo.computeDailyTrend(windowDays);

        // ── Success rate ──────────────────────────────────────────────────────
        double overallSuccessRate = batchRepo.computeSuccessRate(windowDays);

        Map<String, Double> successRateByJob = jobAggs.stream()
                .collect(Collectors.toMap(BatchExecutionRepository.JobAggregate::jobName, BatchExecutionRepository.JobAggregate::successRate));

        // ── Volume ────────────────────────────────────────────────────────────
        long totalRowsExtracted = statsRepo.sumRowsExtracted(windowDays);

        Map<String, Long> rowsByTable = statsRepo.sumRowsByTable(windowDays).stream()
                .collect(Collectors.toMap(
                        StatsRepository.TableVolumeRecord::tableName,
                        StatsRepository.TableVolumeRecord::totalLoaded));

        // ── Throughput (avg rows/sec) ────────────────────────────────────────
        // Agrégé depuis les daily trends : total_write / total_duration_sec
        double avgThroughput = dailyAggs.stream()
                .filter(d -> d.totalWrite() > 0)
                .mapToDouble(d -> (double) d.totalWrite() / Math.max(1, d.jobCount()))
                .average().orElse(0.0);

        Map<String, Double> throughputByJob = jobAggs.stream()
                .collect(Collectors.toMap(
                        BatchExecutionRepository.JobAggregate::jobName,
                        agg -> agg.avgDurationSec() > 0
                                ? agg.completed() / agg.avgDurationSec()
                                : 0.0));

        // ── Durées moyennes ──────────────────────────────────────────────────
        double avgDuration = jobAggs.stream()
                .mapToDouble(BatchExecutionRepository.JobAggregate::avgDurationSec)
                .average().orElse(0.0);

        Map<String, Double> avgDurationByJob = jobAggs.stream()
                .collect(Collectors.toMap(BatchExecutionRepository.JobAggregate::jobName, BatchExecutionRepository.JobAggregate::avgDurationSec));

        // ── Failures ─────────────────────────────────────────────────────────
        int totalFailed = jobAggs.stream().mapToInt(BatchExecutionRepository.JobAggregate::failed).sum();
        int maxConsecutiveFailures = jobAggs.stream()
                .mapToInt(agg -> batchRepo.countConsecutiveFailures(agg.jobName()))
                .max().orElse(0);

        // ── Trend journalier ─────────────────────────────────────────────────
        List<DailyVolumeDto> trend = dailyAggs.stream()
                .map(d -> DailyVolumeDto.builder()
                        .date(d.date())
                        .rowsRead(d.totalRead())
                        .rowsWritten(d.totalWrite())
                        .jobCount(d.jobCount())
                        .successRate(d.jobCount() > 0
                                ? (double) d.completed() / d.jobCount() * 100.0
                                : 0.0)
                        .build())
                .toList();

        return KpiDto.builder()
                .windowDays(windowDays)
                .overallSuccessRate(Math.round(overallSuccessRate * 100.0) / 100.0)
                .successRateByJob(successRateByJob)
                .totalRowsExtracted(totalRowsExtracted)
                .rowsByTable(rowsByTable)
                .avgThroughputPerSec(Math.round(avgThroughput * 100.0) / 100.0)
                .throughputByJob(throughputByJob)
                .avgDurationSeconds(Math.round(avgDuration * 10.0) / 10.0)
                .avgDurationByJob(avgDurationByJob)
                .failureCount(totalFailed)
                .consecutiveFailureCount(maxConsecutiveFailures)
                .volumeTrend(trend)
                .computedAt(LocalDateTime.now())
                .build();
    }
}
