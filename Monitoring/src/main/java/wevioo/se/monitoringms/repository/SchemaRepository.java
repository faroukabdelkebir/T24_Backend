package wevioo.se.monitoringms.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import wevioo.se.monitoringms.model.SchemaAuditRecord;
import wevioo.se.monitoringms.model.SchemaSourceRecord;

import java.util.List;

/**
 * Accès en lecture à SCHEMA_SOURCE et SCHEMA_SOURCE_AUDIT.
 */
@Repository
@RequiredArgsConstructor
public class SchemaRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<SchemaSourceRecord> SOURCE_MAPPER = (rs, row) ->
            SchemaSourceRecord.builder()
                    .id(rs.getString("id"))
                    .tableName(rs.getString("table_name"))
                    .columnName(rs.getString("column_name"))
                    .dataType(rs.getString("datatype"))
                    .enabled(rs.getBoolean("_enabled"))
                    .indexed(rs.getBoolean("indexed"))
                    .ssTag(rs.getString("ssTag"))
                    .singleMult(rs.getString("single_mult"))
                    .localRef(rs.getBoolean("local_ref"))
                    .insertDate(rs.getObject("insert_date", Long.class))
                    .build();

    private static final RowMapper<SchemaAuditRecord> AUDIT_MAPPER = (rs, row) ->
            SchemaAuditRecord.builder()
                    .id(rs.getInt("id"))
                    .sourceId(rs.getString("sourceId"))
                    .sourceTableName(rs.getString("sourceTable_name"))
                    .sourceColumnName(rs.getString("sourceColumn_name"))
                    .sourceDataType(rs.getString("sourceDatatype"))
                    .newSourceEnabled(rs.getInt("newSource_enabled"))
                    .oldSourceEnabled(rs.getInt("oldSource_enabled"))
                    .newSourceIndexed(rs.getInt("newSourceIndexed"))
                    .oldSourceIndexed(rs.getInt("oldSourceIndexed"))
                    .updatedBy(rs.getString("UpdatedBy"))
                    .updatedOn(rs.getObject("UpdatedOn", Long.class))
                    .build();

    // ── SCHEMA_SOURCE ────────────────────────────────────────────────────────

    public List<SchemaSourceRecord> findAll() {
        return jdbc.query(
                "SELECT id, table_name, column_name, datatype, _enabled, indexed, ssTag, single_mult, local_ref, insert_date FROM SCHEMA_SOURCE ORDER BY table_name, column_name",
                SOURCE_MAPPER);
    }

    public List<SchemaSourceRecord> findByTable(String tableName) {
        return jdbc.query(
                "SELECT id, table_name, column_name, datatype, _enabled, indexed, ssTag, single_mult, local_ref, insert_date FROM SCHEMA_SOURCE WHERE table_name = ? ORDER BY column_name",
                SOURCE_MAPPER, tableName);
    }

    public List<SchemaSourceRecord> findEnabled(String tableName) {
        return jdbc.query(
                "SELECT id, table_name, column_name, datatype, _enabled, indexed, ssTag, single_mult, local_ref, insert_date FROM SCHEMA_SOURCE WHERE table_name = ? AND _enabled = 1 ORDER BY column_name",
                SOURCE_MAPPER, tableName);
    }

    /**
     * Résumé par table : nombre de colonnes totales / actives / indexées.
     */
    public List<TableSchemaSummary> summarizeByTable() {
        return jdbc.query(
                """
                SELECT table_name,
                       COUNT(*)                                         AS total_columns,
                       SUM(CAST(_enabled AS int))                       AS enabled_columns,
                       SUM(CAST(indexed AS int))                        AS indexed_columns,
                       SUM(CASE WHEN single_mult = 'M' THEN 1 ELSE 0 END) AS mv_columns
                FROM SCHEMA_SOURCE
                GROUP BY table_name
                ORDER BY table_name
                """,
                (rs, row) -> new TableSchemaSummary(
                        rs.getString("table_name"),
                        rs.getInt("total_columns"),
                        rs.getInt("enabled_columns"),
                        rs.getInt("indexed_columns"),
                        rs.getInt("mv_columns")
                ));
    }

    // ── SCHEMA_SOURCE_AUDIT ──────────────────────────────────────────────────

    public List<SchemaAuditRecord> findRecentAudit(int limit) {
        return jdbc.query(
                "SELECT TOP (?) id, sourceId, sourceTable_name, sourceColumn_name, sourceDatatype, newSource_enabled, oldSource_enabled, newSourceIndexed, oldSourceIndexed, UpdatedBy, UpdatedOn FROM SCHEMA_SOURCE_AUDIT ORDER BY UpdatedOn DESC",
                AUDIT_MAPPER, limit);
    }

    public List<SchemaAuditRecord> findAuditByTable(String tableName) {
        return jdbc.query(
                "SELECT id, sourceId, sourceTable_name, sourceColumn_name, sourceDatatype, newSource_enabled, oldSource_enabled, newSourceIndexed, oldSourceIndexed, UpdatedBy, UpdatedOn FROM SCHEMA_SOURCE_AUDIT WHERE sourceTable_name = ? ORDER BY UpdatedOn DESC",
                AUDIT_MAPPER, tableName);
    }

    /**
     * Détecte les drifts récents : colonnes activées/désactivées dans les N derniers jours.
     */
    public int countRecentDrifts(int days) {
        Integer count = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM SCHEMA_SOURCE_AUDIT
                WHERE UpdatedOn >= CAST(FORMAT(DATEADD(day, -?, GETDATE()), 'yyyyMMddHHmmss') AS bigint)
                """,
                Integer.class, days);
        return count != null ? count : 0;
    }

    // ── Inner record ────────────────────────────────────────────────────────

    public record TableSchemaSummary(
            String tableName,
            int    totalColumns,
            int    enabledColumns,
            int    indexedColumns,
            int    mvColumns
    ) {}
}
