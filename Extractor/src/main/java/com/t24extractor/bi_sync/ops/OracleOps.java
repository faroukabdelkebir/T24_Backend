package com.t24extractor.bi_sync.ops;

import com.t24extractor.model.SchemaSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Set;

@Slf4j
public class OracleOps extends SqlOps {

    @Override
    public Set<String> destColumns(JdbcTemplate jdbcTemplate, String table) {
        throw new UnsupportedOperationException("destColumns is not supported in OracleOps");
    }

    @Override
    public void createRawTable(JdbcTemplate jdbcTemplate, String table) {
        throw new UnsupportedOperationException("createRawTable is not supported in OracleOps");
    }

    @Override
    public String existsQuery(String table) {
        throw new UnsupportedOperationException("existsQuery is not supported in OracleOps");
    }

    @Override
    public void createDestTable(JdbcTemplate jdbcTemplate, String table, Set<SchemaSource> set) {
        throw new UnsupportedOperationException("createDestTable is not supported in OracleOps");
    }

    @Override
    public void createDefaultDestTable(JdbcTemplate jdbcTemplate, String table) {
        throw new UnsupportedOperationException("createDefaultDestTable is not supported in OracleOps");
    }

    @Override
    public String insertConstruction(String table, Set<SchemaSource> set) {
        throw new UnsupportedOperationException("insertConstruction is not supported in OracleOps");
    }

    @Override
    public String mergeConstruction(String table, Set<SchemaSource> set) {
        throw new UnsupportedOperationException("mergeConstruction is not supported in OracleOps");
    }

    @Override
    public void addCol(JdbcTemplate jdbcTemplate, String table, String column, String dataType) {
        throw new UnsupportedOperationException("addCol is not supported in OracleOps");
    }

    @Override
    public void dropCol(JdbcTemplate jdbcTemplate, String table, String col) {
        throw new UnsupportedOperationException("dropCol is not supported in OracleOps");
    }

    @Override
    public boolean existIndexQuery(JdbcTemplate jdbcTemplate, String tableName, String column) {
        throw new UnsupportedOperationException("existIndexQuery is not supported in OracleOps");
    }

    @Override
    public String escapeColumn(String column) {
        return column;
    }

    @Override
    public String escapeTableName(String table) {
        return table;
    }

    @Override
    public void createIndex(JdbcTemplate jdbcTemplate, String tableName, String column) {
        throw new UnsupportedOperationException("createIndex is not supported in OracleOps");
    }

    @Override
    public void dropIndex(JdbcTemplate jdbcTemplate, String column) {
        throw new UnsupportedOperationException("dropIndex is not supported in OracleOps");
    }

    @Override
    public int jsonNormalize(JdbcTemplate jdbcTemplate, String tableName) {
        throw new UnsupportedOperationException("jsonNormalize is not supported in OracleOps");
    }

    @Override
    public Long countTopFive(JdbcTemplate jdbcTemplate, String tableName) {
        Long countTop = 0L;
        try {
            countTop = jdbcTemplate.queryForObject(
                    String.format("SELECT COUNT(*) FROM (SELECT 1 FROM %s FETCH FIRST 5 ROWS ONLY)", tableName),
                    Long.class
            );
        } catch (Exception e) {
            throw new IllegalStateException("Erreur COUNT TOP 5 sur la table " + tableName, e);
        }
        return countTop;
    }

    @Override
    public Long countAll(JdbcTemplate jdbcTemplate, String table) {
        try {
            String metadataCountQuery = """
                    SELECT NVL(
                        (SELECT SUM(num_rows) FROM user_tab_partitions WHERE table_name = UPPER(?)),
                        (SELECT num_rows FROM user_tables WHERE table_name = UPPER(?))
                    ) AS total_rows
                    FROM dual
                    """;

            Long metadataCount = jdbcTemplate.queryForObject(metadataCountQuery, Long.class, table, table);
            if (metadataCount != null) {
                return metadataCount;
            }

            // Fallback exact count when Oracle stats are missing or not yet gathered.
            return jdbcTemplate.queryForObject(String.format("SELECT COUNT(1) FROM %s", table), Long.class);
        } catch (Exception e) {
            log.error("Erreur lors du count sur la table {}", table, e);
            throw new IllegalStateException("Echec lors du count sur la table " + table, e);
        }
    }
}
