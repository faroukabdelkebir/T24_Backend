package wevioo.se.monitoringms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import wevioo.se.monitoringms.dto.ReconciliationDto;
import wevioo.se.monitoringms.model.StatsRecord;
import wevioo.se.monitoringms.repository.StatsRepository;

import java.util.List;

/**
 * Réconciliation des comptages Oracle ↔ SQL Server.
 *
 * Pour chaque table extraite, on compare :
 *   - oracle_count  : lignes source (comptées sur Oracle avant extraction)
 *   - mongo_count   : lignes réellement insérées en SQL Server
 *
 * Statuts :
 *   OK      → taux ≥ (100% - tolerance)
 *   WARNING → taux entre 80% et le seuil de tolérance
 *   KO      → taux < 80%
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationService {

    private final StatsRepository statsRepo;

    @Value("${monitor.alert.reconciliation-tolerance-pct:5}")
    private double tolerancePct;

    @Cacheable(value = "kpis", key = "'reconciliation'")
    public List<ReconciliationDto> getLatestReconciliation() {
        List<StatsRecord> latestStats = statsRepo.findLatestPerTable();

        return latestStats.stream()
                .map(this::toDto)
                .toList();
    }

    public List<ReconciliationDto> getReconciliationForTable(String tableName) {
        return statsRepo.findByTable(tableName, 30).stream()
                .map(this::toDto)
                .toList();
    }

    // ── Conversion ────────────────────────────────────────────────────────────

    private ReconciliationDto toDto(StatsRecord s) {
        int    delta  = s.getOracleCount() - s.getSqlCount();
        double rate   = s.getOracleCount() == 0 ? 100.0
                : (double) s.getSqlCount() / s.getOracleCount() * 100.0;
        String status = resolveStatus(rate);

        return ReconciliationDto.builder()
                .tableName(s.getMssqlTable())
                .businessDate(s.getBusinessDate())
                .oracleCount(s.getOracleCount())
                .sqlServerCount(s.getSqlCount())
                .delta(delta)
                .reconciliationRate(Math.round(rate * 100.0) / 100.0)
                .reconciled(rate >= (100.0 - tolerancePct))
                .status(status)
                .build();
    }

    private String resolveStatus(double rate) {
        if (rate >= (100.0 - tolerancePct)) return "Conforme";
        if (rate >= 80.0)                   return "Avertissement";
        return "Non conforme";
    }
}

