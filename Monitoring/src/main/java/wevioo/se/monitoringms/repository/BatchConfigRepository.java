package wevioo.se.monitoringms.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import wevioo.se.monitoringms.model.BatchConfigRecord;

import java.util.List;
import java.util.Optional;

/**
 * Accès à BATCH_CONFIG — configuration des extractions T24.
 */
@Repository
@RequiredArgsConstructor
public class BatchConfigRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<BatchConfigRecord> CONFIG_MAPPER = (rs, row) ->
            BatchConfigRecord.builder()
                    .id(rs.getString("id"))
                    .tableName(rs.getString("TableName"))
                    .t24SelectionName(rs.getString("T24SelectionName"))
                    .colonneRef(rs.getString("colonne_ref"))
                    .chargement(rs.getString("chargement"))
                    .lastDate(rs.getObject("last_date", Long.class))
                    .minDate(rs.getObject("min_date", Long.class))
                    .enabled(rs.getBoolean("enabled"))
                    .scheduler(rs.getBoolean("scheduler"))
                    .schedulerTime(rs.getString("scheduler_time"))
                    .build();

    public List<BatchConfigRecord> findAll() {
        return jdbc.query(
                "SELECT id, TableName, T24SelectionName, colonne_ref, chargement, last_date, min_date, enabled, scheduler, scheduler_time FROM BATCH_CONFIG ORDER BY id",
                CONFIG_MAPPER);
    }

    public Optional<BatchConfigRecord> findById(String id) {
        List<BatchConfigRecord> results = jdbc.query(
                "SELECT id, TableName, T24SelectionName, colonne_ref, chargement, last_date, min_date, enabled, scheduler, scheduler_time FROM BATCH_CONFIG WHERE id = ?",
                CONFIG_MAPPER, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Met à jour uniquement last_date (appelé après une extraction réussie).
     */
    public int updateLastDate(String id, Long lastDate) {
        return jdbc.update("UPDATE BATCH_CONFIG SET last_date = ? WHERE id = ?", lastDate, id);
    }

    /**
     * Active/désactive une config.
     */
    public int setEnabled(String id, boolean enabled) {
        return jdbc.update("UPDATE BATCH_CONFIG SET enabled = ? WHERE id = ?", enabled ? 1 : 0, id);
    }
}
