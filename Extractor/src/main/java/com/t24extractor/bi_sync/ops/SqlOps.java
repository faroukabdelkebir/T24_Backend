package com.t24extractor.bi_sync.ops;


import com.t24extractor.model.MetaDataTable;
import com.t24extractor.model.SchemaSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

import static com.t24extractor.commons.Constants.T24_METADATA;

@Slf4j
public abstract class SqlOps {

    public abstract Set<String> destColumns(JdbcTemplate jdbcTemplate, String table);

    public abstract void createRawTable(JdbcTemplate jdbcTemplate, String table);

    public abstract String existsQuery(String table);

    public abstract void createDestTable(JdbcTemplate jdbcTemplate, String table, Set<SchemaSource> set)
            throws Exception;

    public abstract void createDefaultDestTable(JdbcTemplate jdbcTemplate, String table) throws Exception;

    public abstract String insertConstruction(String table, Set<SchemaSource> set)
            throws Exception;

    public abstract String mergeConstruction(String table, Set<SchemaSource> set)
            throws Exception;

    public abstract void addCol(JdbcTemplate jdbcTemplate, String table, String column, String DataType);

    public abstract void dropCol(JdbcTemplate jdbcTemplate, String table, String col);

    public void truncateTable(JdbcTemplate jdbcTemplate, String table) {
        String sql = String.join(" ", "TRUNCATE TABLE", escapeTableName(table));
        jdbcTemplate.execute(sql);
    }

    public void dropTable(JdbcTemplate jdbcTemplate, String table) {
        String sql = String.join(" ", "DROP TABLE", escapeTableName(table));
        jdbcTemplate.execute(sql);
    }

    public Set<String> fieldsOnly(Set<SchemaSource> set) {
        Set<String> tset = new LinkedHashSet<>();
        for (SchemaSource bi : set) {
            String obj = String.join(" ", escapeColumn(bi.getColumnName()), getDataType(bi.getDataType()));
            tset.add(obj);
        }
        return tset;
    }


    public List<String> fieldsToInsert(Set<SchemaSource> set) {
        List<String> mandatoryFields = new ArrayList<>();
        mandatoryFields.add("_id");
        mandatoryFields.add("ORIGINAL_ID");

        log.info("*****************************************");
        log.info("Info {} ",set
                .stream()
                .map(SchemaSource::getColumnName)
                .toList());

        mandatoryFields.addAll(set
                .stream()
                .map(SchemaSource::getColumnName)
                .toList());
        return mandatoryFields;
    }

    public List<String> mssqlfieldsToInsert(Set<SchemaSource> set) {
        List<String> mandatoryFields = new ArrayList<>();
        mandatoryFields.add(escapeColumn("_id"));
        mandatoryFields.add(escapeColumn("ORIGINAL_ID"));
        mandatoryFields.addAll(set
                .stream()
                .map(c -> escapeColumn(c.getColumnName()))
                .toList());
        return mandatoryFields;
    }

    public String placeHolders(int fieldsNum) {
        return "?,".repeat(Math.max(0, fieldsNum - 1)) +
                "?";
    }

    public String getDataType(String ssdataType) {
        if (ssdataType.contains("StringMAX")) {
            return " VARCHAR(MAX) ";
        }
        return "VARCHAR(8000)";
    }

    public void deleteFrom(JdbcTemplate jdbcTemplate, String tableName, String columnName, Object var) {
        String deleteQuery = String.join(" ", "DELETE FROM", escapeTableName(tableName), " WHERE", escapeColumn(columnName), "= N'" + var + "'");
        var updCnt = jdbcTemplate.batchUpdate(deleteQuery);
    }


    public abstract boolean existIndexQuery(JdbcTemplate jdbcTemplate, String tableName, String column);

    public abstract String escapeColumn(String column);

    public abstract String escapeTableName(String table);

    public abstract void createIndex(JdbcTemplate jdbcTemplate, String tableName, String column);

    public abstract void dropIndex(JdbcTemplate jdbcTemplate, String column);

    public abstract int jsonNormalize(JdbcTemplate jdbcTemplate, String tableName);


    public abstract Long countTopFive(JdbcTemplate jdbcTemplate, String tableName);


    public Long countAll(JdbcTemplate jdbcTemplate, String table) {
        Long count = 0L;
        try {

            count = jdbcTemplate.queryForObject(String.format("SELECT COUNT(1) FROM %s", table), Long.class);
        } catch (Exception e) {
            log.error("Une erreur est survenue lors de l'exécution de la requete Count sur la table {}", table);
        }
        return count;
    }

    public void updateQuery(JdbcTemplate jdbcTemplate, String table, Map<String, Object> cols, String condition) {
        StringBuilder updateQuery = new StringBuilder("UPDATE")
                .append(" ")
                .append(table)
                .append("\n")
                .append(" SET ");
        Iterator<Map.Entry<String, Object>> it = cols.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            updateQuery.append(entry.getKey()).append("=").append(entry.getValue());

            if (it.hasNext()) {
                updateQuery.append(", ");
            }
        }
        updateQuery.append("\n")
                .append(" WHERE ")
                .append(condition);
        jdbcTemplate.batchUpdate(updateQuery.toString());

    }

    public void save(JdbcTemplate jdbcTemplate, List<MetaDataTable> metaDataTableList) {


        /* IF EXISTS(SELECT * from iplog Where [date]='20120101' and sip=1 and dip=2 and port=80)
         BEGIN
                 UPDATE
         iplog
                 SET
         t.size=t.size+1
         WHERE
                 [date]='20120101' and sip=1 and dip=2 and port=80
         END
                 ELSE
         BEGIN
         INSERT INTO iplog VALUES ('20120101',1,2,80,1)
         END */


        for (MetaDataTable meta : metaDataTableList) {
            jdbcTemplate.batchUpdate(String.format("IF NOT EXISTS(SELECT * from " + T24_METADATA + " Where [Table_Name]='%1$s' and MultiValueFields='%2$s')" +
                    "BEGIN\n" +
                    "         INSERT INTO " + T24_METADATA + " VALUES ('%1$s','%2$s','%3$s')\n" +
                    "         END", meta.tablename(), meta.multivalueFields(), meta.localref()));
        }

//        jdbcTemplate.batchUpdate("INSERT INTO T24_Metadata (Table_Name, MultiValueFields, LOCAL_REF) VALUES(?,?,?)",
//                new BatchPreparedStatementSetter() {
//
//                    @Override
//                    public void setValues(PreparedStatement ps, int i)
//                            throws SQLException {
//
//                        MetaDataTable metaDataTable = metaDataTableList.get(i);
//                        ps.setString(1, metaDataTable.tablename());
//                        ps.setString(2, metaDataTable.multivalueFields());
//                        ps.setBoolean(3, metaDataTable.localref());
//
//                    }
//
//                    @Override
//                    public int getBatchSize() {
//                        return metaDataTableList.size();
//                    }
//                });


    }
}
