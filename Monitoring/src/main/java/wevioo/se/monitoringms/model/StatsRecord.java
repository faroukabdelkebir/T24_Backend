package wevioo.se.monitoringms.model;

import lombok.Builder;
import lombok.Data;

/**
 * Mappe la table `stats` — renseignée par le microservice extracteur.
 * Utilisée pour la réconciliation Oracle ↔ SQL Server.
 */
@Data
@Builder
public class StatsRecord {

    private Long   jobExecutionId;
    private String mssqlTable;
    private int    businessDate;
    private int    oracleCount;
    private int    oracleChunkCount;
    private int    sqlCount;
    private long   insertDate;

    /**
     * Taux de réconciliation : lignes insérées / lignes source.
     * Retourne null si oracleCount = 0 .
     */
    public Double getReconciliationRate() {
        if (oracleCount == 0) return null;
        return (double) sqlCount / oracleCount * 100.0;
    }

    public boolean isReconciled(double tolerancePct) {
        Double rate = getReconciliationRate();
        return rate != null && rate >= (100.0 - tolerancePct);
    }
}
