package wevioo.se.monitoringms.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import wevioo.se.monitoringms.model.BatchJobExecution;
import wevioo.se.monitoringms.model.BatchStepExecution;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Accès en lecture aux tables Spring Batch Metadata dans SQL Server.
 *
 * Tables utilisées :
 *   - BATCH_JOB_INSTANCE
 *   - BATCH_JOB_EXECUTION
 *   - BATCH_STEP_EXECUTION
 */
@Repository
@RequiredArgsConstructor
public class BatchExecutionRepository {

    private final JdbcTemplate jdbc;

    // ── RowMappers ──────────────────────────────────────────────────────────

    private static final RowMapper<BatchJobExecution> JOB_EXEC_MAPPER = (rs, row) ->
            BatchJobExecution.builder()
                    .jobExecutionId(rs.getLong("JOB_EXECUTION_ID"))
                    .jobInstanceId(rs.getLong("JOB_INSTANCE_ID"))
                    .jobName(rs.getString("JOB_NAME"))
                    .version(rs.getInt("VERSION"))
                    .createTime(toLocalDateTime(rs.getTimestamp("CREATE_TIME")))
                    .startTime(toLocalDateTime(rs.getTimestamp("START_TIME")))
                    .endTime(toLocalDateTime(rs.getTimestamp("END_TIME")))
                    .status(rs.getString("STATUS"))
                    .exitCode(rs.getString("EXIT_CODE"))
                    .exitMessage(rs.getString("EXIT_MESSAGE"))
                    .lastUpdated(toLocalDateTime(rs.getTimestamp("LAST_UPDATED")))
                    .build();

    private static final RowMapper<BatchStepExecution> STEP_EXEC_MAPPER = (rs, row) ->
            BatchStepExecution.builder()
                    .stepExecutionId(rs.getLong("STEP_EXECUTION_ID"))
                    .jobExecutionId(rs.getLong("JOB_EXECUTION_ID"))
                    .stepName(rs.getString("STEP_NAME"))
                    .version(rs.getInt("VERSION"))
                    .createTime(toLocalDateTime(rs.getTimestamp("CREATE_TIME")))
                    .startTime(toLocalDateTime(rs.getTimestamp("START_TIME")))
                    .endTime(toLocalDateTime(rs.getTimestamp("END_TIME")))
                    .status(rs.getString("STATUS"))
                    .commitCount(rs.getLong("COMMIT_COUNT"))
                    .readCount(rs.getLong("READ_COUNT"))
                    .filterCount(rs.getLong("FILTER_COUNT"))
                    .writeCount(rs.getLong("WRITE_COUNT"))
                    .readSkipCount(rs.getLong("READ_SKIP_COUNT"))
                    .writeSkipCount(rs.getLong("WRITE_SKIP_COUNT"))
                    .processSkipCount(rs.getLong("PROCESS_SKIP_COUNT"))
                    .rollbackCount(rs.getLong("ROLLBACK_COUNT"))
                    .exitCode(rs.getString("EXIT_CODE"))
                    .exitMessage(rs.getString("EXIT_MESSAGE"))
                    .lastUpdated(toLocalDateTime(rs.getTimestamp("LAST_UPDATED")))
                    .build();

    // ── Queries ─────────────────────────────────────────────────────────────

    private static final String BASE_JOB_SELECT = """
        SELECT je.JOB_EXECUTION_ID, je.JOB_INSTANCE_ID, je.VERSION,
               je.CREATE_TIME, je.START_TIME, je.END_TIME,
               je.STATUS, je.EXIT_CODE, je.EXIT_MESSAGE, je.LAST_UPDATED,
               ji.JOB_NAME
        FROM BATCH_JOB_EXECUTION je
        INNER JOIN BATCH_JOB_INSTANCE ji ON ji.JOB_INSTANCE_ID = je.JOB_INSTANCE_ID
        """;

    /**
     * Jobs actuellement en cours d'exécution (STATUS = 'STARTED').
     */
    public List<BatchJobExecution> findRunning() {
        return jdbc.query(
                BASE_JOB_SELECT + " WHERE je.STATUS IN ('STARTED','STARTING') ORDER BY je.START_TIME DESC",
                JOB_EXEC_MAPPER);
    }

    /**
     * Historique paginé — tous jobs, du plus récent au plus ancien.
     */
    public List<BatchJobExecution> findRecent(int limit) {
        return jdbc.query(
                BASE_JOB_SELECT + " ORDER BY je.CREATE_TIME DESC OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY",
                JOB_EXEC_MAPPER, limit);
    }

    /**
     * Jobs des N derniers jours.
     */
    public List<BatchJobExecution> findByDays(int days) {
        return jdbc.query(
                BASE_JOB_SELECT + " WHERE je.CREATE_TIME >= DATEADD(day, -?, GETDATE()) ORDER BY je.CREATE_TIME DESC",
                JOB_EXEC_MAPPER, days);
    }

    /**
     * Exécutions d'un job spécifique par nom, paginées.
     */
    public List<BatchJobExecution> findByJobName(String jobName, int limit) {
        return jdbc.query(
                BASE_JOB_SELECT + " WHERE ji.JOB_NAME = ? ORDER BY je.CREATE_TIME DESC OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY",
                JOB_EXEC_MAPPER, jobName, limit);
    }

    /**
     * Une exécution par ID.
     */
    public Optional<BatchJobExecution> findById(long jobExecutionId) {
        List<BatchJobExecution> result = jdbc.query(
                BASE_JOB_SELECT + " WHERE je.JOB_EXECUTION_ID = ?",
                JOB_EXEC_MAPPER, jobExecutionId);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    /**
     * Steps d'une exécution de job.
     */
    public List<BatchStepExecution> findStepsByJobExecutionId(long jobExecutionId) {
        return jdbc.query(
                """
                SELECT STEP_EXECUTION_ID, JOB_EXECUTION_ID, STEP_NAME, VERSION,
                       CREATE_TIME, START_TIME, END_TIME, STATUS,
                       COMMIT_COUNT, READ_COUNT, FILTER_COUNT, WRITE_COUNT,
                       READ_SKIP_COUNT, WRITE_SKIP_COUNT, PROCESS_SKIP_COUNT,
                       ROLLBACK_COUNT, EXIT_CODE, EXIT_MESSAGE, LAST_UPDATED
                FROM BATCH_STEP_EXECUTION
                WHERE JOB_EXECUTION_ID = ?
                ORDER BY CREATE_TIME ASC
                """,
                STEP_EXEC_MAPPER, jobExecutionId);
    }

    /**
     * Nombre de jobs COMPLETED aujourd'hui.
     */
    public int countCompletedToday() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM BATCH_JOB_EXECUTION WHERE STATUS = 'COMPLETED' AND CAST(END_TIME AS DATE) = CAST(GETDATE() AS DATE)",
                Integer.class);
        return count != null ? count : 0;
    }

    /**
     * Nombre de jobs FAILED aujourd'hui.
     */
    public int countFailedToday() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM BATCH_JOB_EXECUTION WHERE STATUS = 'FAILED' AND CAST(LAST_UPDATED AS DATE) = CAST(GETDATE() AS DATE)",
                Integer.class);
        return count != null ? count : 0;
    }

    /**
     * Taux de succès sur N jours : COMPLETED / (COMPLETED + FAILED) * 100.
     */
    public double computeSuccessRate(int days) {
        Double rate = jdbc.queryForObject(
                """
                SELECT CASE WHEN COUNT(*) = 0 THEN 100.0
                            ELSE CAST(SUM(CASE WHEN STATUS = 'COMPLETED' THEN 1 ELSE 0 END) AS FLOAT)
                                 / COUNT(*) * 100.0
                       END
                FROM BATCH_JOB_EXECUTION
                WHERE STATUS IN ('COMPLETED','FAILED')
                  AND CREATE_TIME >= DATEADD(day, -?, GETDATE())
                """,
                Double.class, days);
        return rate != null ? rate : 0.0;
    }

    /**
     * Statistiques agrégées par job sur N jours.
     */
    public List<JobAggregate> computeJobAggregates(int days) {
        return jdbc.query(
                """
                SELECT ji.JOB_NAME,
                       COUNT(*)                                              AS total_executions,
                       SUM(CASE WHEN je.STATUS='COMPLETED' THEN 1 ELSE 0 END) AS completed,
                       SUM(CASE WHEN je.STATUS='FAILED'    THEN 1 ELSE 0 END) AS failed,
                       AVG(DATEDIFF(second, je.START_TIME, je.END_TIME))     AS avg_duration_sec
                FROM BATCH_JOB_EXECUTION je
                INNER JOIN BATCH_JOB_INSTANCE ji ON ji.JOB_INSTANCE_ID = je.JOB_INSTANCE_ID
                WHERE je.STATUS IN ('COMPLETED','FAILED')
                  AND je.CREATE_TIME >= DATEADD(day, -?, GETDATE())
                GROUP BY ji.JOB_NAME
                """,
                (rs, row) -> new JobAggregate(
                        rs.getString("JOB_NAME"),
                        rs.getInt("total_executions"),
                        rs.getInt("completed"),
                        rs.getInt("failed"),
                        rs.getObject("avg_duration_sec") != null ? rs.getDouble("avg_duration_sec") : 0.0
                ),
                days);
    }

    /**
     * Trend journalier : volume de lignes lues/écrites par jour sur N jours.
     */
    public List<DailyAggregate> computeDailyTrend(int days) {
        return jdbc.query(
                """
                SELECT CONVERT(varchar(8), je.CREATE_TIME, 112) AS exec_date,
                       SUM(se.READ_COUNT)                        AS total_read,
                       SUM(se.WRITE_COUNT)                       AS total_write,
                       COUNT(DISTINCT je.JOB_EXECUTION_ID)       AS job_count,
                       SUM(CASE WHEN je.STATUS='COMPLETED' THEN 1 ELSE 0 END) AS completed
                FROM BATCH_JOB_EXECUTION je
                INNER JOIN BATCH_STEP_EXECUTION se ON se.JOB_EXECUTION_ID = je.JOB_EXECUTION_ID
                WHERE je.CREATE_TIME >= DATEADD(day, -?, GETDATE())
                GROUP BY CONVERT(varchar(8), je.CREATE_TIME, 112)
                ORDER BY exec_date DESC
                """,
                (rs, row) -> new DailyAggregate(
                        rs.getString("exec_date"),
                        rs.getLong("total_read"),
                        rs.getLong("total_write"),
                        rs.getInt("job_count"),
                        rs.getInt("completed")
                ),
                days);
    }

    /**
     * Dernière exécution réussie d'un job par nom.
     */
    public Optional<BatchJobExecution> findLastSuccessfulByJobName(String jobName) {
        List<BatchJobExecution> result = jdbc.query(
                BASE_JOB_SELECT + """
             WHERE ji.JOB_NAME = ? AND je.STATUS = 'COMPLETED'
             ORDER BY je.END_TIME DESC
             OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY
            """,
                JOB_EXEC_MAPPER, jobName);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    /**
     * Nombre d'échecs consécutifs pour un job donné.
     */
    public int countConsecutiveFailures(String jobName) {
        Integer count = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM (
                  SELECT TOP 10 je.STATUS
                  FROM BATCH_JOB_EXECUTION je
                  INNER JOIN BATCH_JOB_INSTANCE ji ON ji.JOB_INSTANCE_ID = je.JOB_INSTANCE_ID
                  WHERE ji.JOB_NAME = ? AND je.STATUS IN ('COMPLETED','FAILED')
                  ORDER BY je.CREATE_TIME DESC
                ) recent
                WHERE STATUS = 'FAILED'
                """,
                Integer.class, jobName);
        return count != null ? count : 0;
    }

    // ── Inner DTOs pour les agrégats ────────────────────────────────────────

    public record JobAggregate(
            String jobName,
            int totalExecutions,
            int completed,
            int failed,
            double avgDurationSec
    ) {
        public double successRate() {
            return totalExecutions == 0 ? 0.0 : (double) completed / totalExecutions * 100.0;
        }
    }

    public record DailyAggregate(
            String date,
            long totalRead,
            long totalWrite,
            int jobCount,
            int completed
    ) {}

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
