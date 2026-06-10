package wevioo.se.monitoringms.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import wevioo.se.monitoringms.model.StatsRecord;

import java.util.List;

/**
 * Accès à la table `stats` alimentée par l'extracteur T24.
 * Utilisé pour le module de réconciliation Oracle ↔ SQL Server.
 */
@Repository
@RequiredArgsConstructor
public class StatsRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<StatsRecord> STATS_MAPPER = (rs, row) ->
            StatsRecord.builder()
                    .jobExecutionId(rs.getLong("job_execution_id"))
                    .mssqlTable(rs.getString("mssql_table"))
                    .businessDate(rs.getInt("business_date"))
                    .oracleCount(rs.getInt("oracle_count"))
                    .oracleChunkCount(rs.getInt("oracle_chunk_count"))
                    .sqlCount(rs.getInt("sql_count"))
                    .insertDate(rs.getLong("insert_date"))
                    .build();

    /**
     * Dernières statistiques par table (dernier business_date).
     */
    public List<StatsRecord> findLatestPerTable() {
        return jdbc.query(
                """
                SELECT s.job_execution_id, s.mssql_table, s.business_date,
                       s.oracle_count, s.oracle_chunk_count, s.mongo_count, s.insert_date
                FROM stats s
                INNER JOIN (
                    SELECT mssql_table, MAX(insert_date) AS max_date
                    FROM stats
                    GROUP BY mssql_table
                ) latest ON s.mssql_table = latest.mssql_table AND s.insert_date = latest.max_date
                ORDER BY s.mssql_table
                """,
                STATS_MAPPER);
    }

    /**
     * Historique des stats pour une table donnée.
     */
    public List<StatsRecord> findByTable(String tableName, int limit) {
        return jdbc.query(
                "SELECT job_execution_id, mssql_table, business_date, oracle_count, oracle_chunk_count, mongo_count, insert_date " +
                        "FROM stats WHERE mssql_table = ? ORDER BY insert_date DESC OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY",
                STATS_MAPPER, tableName, limit);
    }

    /**
     * Total de lignes extraites sur N jours.
     */
    public long sumRowsExtracted(int days) {
        Long total = jdbc.queryForObject(
                """
                SELECT ISNULL(SUM(mongo_count), 0)
                FROM stats
                WHERE CAST(CAST(business_date AS varchar(8)) AS DATE) >= DATEADD(day, -?, CAST(GETDATE() AS DATE))
                """,
                Long.class, days);
        return total != null ? total : 0L;
    }

    /**
     * Volume extrait par table sur N jours.
     */
    public List<TableVolumeRecord> sumRowsByTable(int days) {
        return jdbc.query(
                """
                SELECT mssql_table,
                       SUM(mongo_count)   AS total_loaded,
                       SUM(oracle_count)  AS total_source,
                       COUNT(*)           AS run_count
                FROM stats
                WHERE CAST(CAST(business_date AS varchar(8)) AS DATE) >= DATEADD(day, -?, CAST(GETDATE() AS DATE))
                GROUP BY mssql_table
                ORDER BY total_loaded DESC
                """,
                (rs, row) -> new TableVolumeRecord(
                        rs.getString("mssql_table"),
                        rs.getLong("total_loaded"),
                        rs.getLong("total_source"),
                        rs.getInt("run_count")
                ),
                days);
    }

    public record TableVolumeRecord(
            String tableName,
            long totalLoaded,
            long totalSource,
            int  runCount
    ) {}
}
