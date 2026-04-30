package com.t24extractor.bi_sync.ops;

import com.t24extractor.model.SchemaSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

import java.sql.Types;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.t24extractor.commons.Constants.*;

@Slf4j
public class SqlServerOps extends SqlOps {

    @Override
    public void createDestTable(JdbcTemplate jdbcTemplate, String table, Set<SchemaSource> set) {

        String fields = String.join(", \n", fieldsOnly(set));
        if (!fieldsOnly(set).isEmpty()) {
            fields = ", "+fields;
        }
        String sb = "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name='" + table + "') " +
                "CREATE TABLE " + escapeTableName(table) + "(" +
                escapeColumn(ID) + ID_TYPE + ", \n" +
                escapeColumn(ORIGINAL_ID) + ORIGINAL_ID_TYPE + " ,\n" +
                escapeColumn("INSERT_DATE") + " " + "BIGINT default replace(replace(replace(CONVERT([varchar](19),sysdatetime(),20),'-',''),':',''),' ','')" +" \n" +
                fields + ", \n" +
                "INDEX ins_date_idx NONCLUSTERED ("+escapeColumn("INSERT_DATE")+") \n" +
                ")";

        jdbcTemplate.batchUpdate(sb);
    }

    @Override
    public void createDefaultDestTable(JdbcTemplate jdbcTemplate, String table) throws Exception {
        String sb = "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name='" + table + "') " +
                "CREATE TABLE " + escapeTableName(table) + "(" +
                escapeColumn(ID) + ID_TYPE + ", \n" +
                escapeColumn(ORIGINAL_ID) + ORIGINAL_ID_TYPE + " ,\n" +
                escapeColumn("INSERT_DATE") + " " + "BIGINT default replace(replace(replace(CONVERT([varchar](19),sysdatetime(),20),'-',''),':',''),' ','')" +", \n" +
                "INDEX ins_date_idx NONCLUSTERED ("+escapeColumn("INSERT_DATE")+") \n" +
                ")";
        jdbcTemplate.batchUpdate(sb);
    }

    @Override
    public void createRawTable(JdbcTemplate jdbcTemplate, String table) {
        String rawTable = RAW_PREFIX+table;
        String sb = "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name='" + rawTable + "') " +
                "CREATE TABLE " + escapeTableName(rawTable) + "(" +
                "raw_id   NVARCHAR(255)," + "\n" +
                "raw_data VARCHAR(MAX)" +
                ")";
        jdbcTemplate.execute(sb);
    }


    @Override
    public String insertConstruction(String table, Set<SchemaSource> set) throws Exception {


        return "INSERT INTO " + escapeTableName(table) + "(" +
                String.join(",", mssqlfieldsToInsert(set)) + ")" +
                " VALUES " + "(" +
                placeHolders(set.size() + 2) +
                ");";
    }

    @Override
    public String mergeConstruction(String table, Set<SchemaSource> set) throws Exception {

        StringBuilder sql = new StringBuilder();
        StringBuilder idCol = new StringBuilder(escapeColumn("_id"));
        sql.append("MERGE INTO ")
                .append(escapeTableName(table))
                .append(" trg USING (SELECT ")
                .append("*")
                .append(" FROM ")
                .append(escapeTableName(table))
                .append(" ) src ON ")
                .append(" (src.").append(idCol).append("= trg.").append(idCol);


        sql.append(" ) ");


        // Set all columns for UPDATE SET statement
        String allColSelect = set.stream()
                .map(colName -> String.format("trg.%s = src.%s", escapeColumn(colName.getColumnName()), escapeColumn(colName.getColumnName()))).collect(Collectors.joining(", "));

        sql.append(allColSelect);
        sql.append(" ); ");

         log.info("Merging staging table and sink table with this command: {}", sql);

        return sql.toString();
    }

    @Override
    public void addCol(JdbcTemplate jdbcTemplate, String table, String column, String DataType) {
        String sql = existsQuery(table);
        sql += "ALTER TABLE " + table + "\n" +
                "ADD " + escapeColumn(column) + " " + getDataType(DataType);
        jdbcTemplate.batchUpdate(sql);
    }

    @Override
    public void dropCol(JdbcTemplate jdbcTemplate, String table, String field) {
        String sql = existsQuery(table);

        sql += String.join(" ", "\n", "ALTER TABLE", table, "\n",
                "DROP COLUMN", field);

        jdbcTemplate.batchUpdate(sql);
    }

    @Override
    public Set<String> destColumns(JdbcTemplate jdbcTemplate, String table) {
        String sql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS\n" +
                "WHERE TABLE_NAME = N'" + table + "' AND COLUMN_NAME NOT IN ('_id', 'ORIGINAL_ID', 'INSERT_DATE')";

        return new HashSet<>(jdbcTemplate.queryForList(sql, String.class));
    }


    @Override
    public String existsQuery(String table) {
        return String.format("IF EXISTS (SELECT * FROM sys.tables WHERE name ='%s')", table);
    }

    @Override
    public boolean existIndexQuery(JdbcTemplate jdbcTemplate, String tableName, String column) {
        String query = String.format("""
                SELECT name
                FROM sys.indexes
                WHERE name='%s' AND object_id = OBJECT_ID('dbo.%s')""", column, tableName);
        List<Map<String, Object>> index = jdbcTemplate.queryForList(query);

        return index.isEmpty();
    }

    @Override
    public String escapeColumn(String column) {
        return "[" + column + "]";
    }

    @Override
    public String escapeTableName(String table) {
        return "[" + table + "]";
    }

    @Override
    public void createIndex(JdbcTemplate jdbcTemplate, String tableName, String column) {
        if (existIndexQuery(jdbcTemplate, tableName, column)) {
            jdbcTemplate.execute(String.format("CREATE NONCLUSTERED INDEX %1$s ON %2$s(%1$s)", escapeColumn(column), escapeTableName(tableName)));
        }
    }

    @Override
    public void dropIndex(JdbcTemplate jdbcTemplate, String column) {
        jdbcTemplate.execute(String.format("DROP INDEX %s", escapeColumn(column)));
    }

    @Override
    public int jsonNormalize(JdbcTemplate jdbcTemplate, String tableName) {
        SqlParameterSource in = new MapSqlParameterSource()
                .addValue("@tableName", tableName, Types.NVARCHAR)
                .addValue("rows_count", Types.INTEGER);
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate);
        Map<String, Object> result = simpleJdbcCall.withProcedureName("basic_json_normalization")
                .withSchemaName("dbo")
                .declareParameters(new SqlParameter("@tableName", Types.NVARCHAR))
                .execute(in);
        return (int) result.get("rows_count");
    }

    @Override
    public Long countTopFive(JdbcTemplate jdbcTemplate, String table){
        Long countTop = 0L;
        try{
            countTop = jdbcTemplate.queryForObject(String.format("SELECT TOP 5 count(*) FROM dbo.%s", table), Long.class);
        }catch (Exception e){
            log.error("Une erreur est survenue lors de l'exécution de la requete Count sur la table {}", table);
        }
        return countTop;
    }

    public Long countAll(JdbcTemplate jdbcTemplate, String table) {
        Long count = 0L;
        try {
            String s = """
            SELECT
               Total_Rows= SUM(st.row_count)
            FROM
               sys.dm_db_partition_stats st
            WHERE
                object_name(object_id) = '%s'""";
            count = jdbcTemplate.queryForObject(String.format(s, table), Long.class);
        } catch (Exception e) {
            log.error("Une erreur est survenue lors de l'exécution de la requete Count sur la table {}", table);
        }
        return count;
    }


}
