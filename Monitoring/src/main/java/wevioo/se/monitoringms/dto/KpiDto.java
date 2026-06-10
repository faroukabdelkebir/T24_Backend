package wevioo.se.monitoringms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class KpiDto {

    private int    windowDays;

    // ── Taux de succès ──────────────────────────────
    private double overallSuccessRate;
    private Map<String, Double> successRateByJob;

    // ── Volume d'extraction ──────────────────────────
    private long   totalRowsExtracted;
    private Map<String, Long> rowsByTable;

    // ── Débit (throughput) ──────────────────────────
    private double avgThroughputPerSec;
    private Map<String, Double> throughputByJob;

    // ── Durées moyennes ─────────────────────────────
    private double avgDurationSeconds;
    private Map<String, Double> avgDurationByJob;

    // ── COB & Business Date ─────────────────────────
    private String lastSuccessfulBusinessDate;
    private int    consecutiveSuccessCount;

    // ── Erreurs ──────────────────────────────────────
    private int    failureCount;
    private int    consecutiveFailureCount;
    private String lastFailureMessage;

    // ── Tendance ────────────────────────────────────
    private List<DailyVolumeDto> volumeTrend;

    private LocalDateTime computedAt;
}

