package com.t24extractor.tasklet;

import lombok.Setter;
import org.json.JSONObject;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.t24extractor.commons.Constants.*;

@Component
@StepScope
public class Json_Flattener implements Tasklet {

    private final JdbcTemplate jdbcTemplate;

    @Setter
    protected String table;

    public Json_Flattener(@Qualifier("jdbcBI") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    @Override
    public RepeatStatus execute(StepContribution stepContribution, @NonNull ChunkContext chunkContext) throws Exception {

        JSONObject nameJs = new JSONObject(
                stepContribution.getStepExecution()
                        .getJobExecution()
                        .getExecutionContext()
                        .getString(BI_STANDARD_SELECTION)
        );
        String name = this.table;

        Iterator<String> iterator = nameJs.keys();

        StringBuilder sb = new StringBuilder();
        List<String> insertQueries = new ArrayList<>();
        while (iterator.hasNext()) {
            String key = iterator.next();

            if (!columnExistsInMainTable(name, key)) {
                continue;
            }

            if (!key.contains("Local_Ref") && !key.equals("DATE_TIME")
                    && nameJs.get(key).toString().contains("MAX")) {

                sb.append("IF NOT EXISTS (SELECT * FROM sys.tables WHERE name='")
                        .append(name).append("#").append(key).append("') ")
                        .append("CREATE TABLE [").append(name).append("#").append(key).append("] (")
                        .append("[").append(ID).append("]").append(ID_TYPE).append(", \n")
                        .append("[").append(key).append("] nvarchar(355) ,\n")
                        .append("[MV_SEQUENCE] int ,\n")
                        .append("[SEQUENCE] int \n")
                        .append(")");

                String insertQuery = String.format("""
                        INSERT INTO [%s#%s] (_id, %s, [MV_SEQUENCE], [SEQUENCE])
                        SELECT _id, lda.%s, lda.[MV_SEQUENCE], lda.[SEQUENCE]
                        FROM %s AS ld
                        CROSS APPLY OPENJSON(ld.%s)
                        WITH (
                          %s nvarchar(255) N'strict $.%s',
                          [MV_SEQUENCE] int N'$.mvsequence',
                          [SEQUENCE] int N'$.sequence'
                        ) AS lda""", name, key, key, key, name, key, key, key);
                insertQueries.add(insertQuery);

            } else if (key.contains("Local_Ref") && key.contains("MAX")) {

                sb.append("IF NOT EXISTS (SELECT * FROM sys.tables WHERE name='")
                        .append(name).append("#").append(key).append("') ")
                        .append("CREATE TABLE [").append(name).append("#").append(key).append("] (")
                        .append("[").append(ID).append("]").append(ID_TYPE).append(", \n")
                        .append("[").append(key).append("] nvarchar(355) ,\n")
                        .append("[SEQUENCE] int \n")
                        .append(")");

                String insertQuery = STR."""
                        INSERT INTO [\{name}#\{key}] (_id, [\{key}], [SEQUENCE])
                        SELECT _id, lf.\{key}, lf.[SEQUENCE]
                        FROM \{name} AS LF_table
                        CROSS APPLY OPENJSON(LF_table.\{key})
                        WITH (
                        \{key} nvarchar(355) N'$.\{key}',
                        [SEQUENCE] int N'$.sequence'
                        ) AS lf""";
                insertQueries.add(insertQuery);
            }
        }

        if (!sb.isEmpty()) {
            jdbcTemplate.execute(sb.toString());
        }

        if (!insertQueries.isEmpty()) {
            jdbcTemplate.batchUpdate(insertQueries.toArray(new String[0]));
        }

        return RepeatStatus.FINISHED;
    }

    private boolean columnExistsInMainTable(String tableName, String columnName) {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_NAME = ? AND COLUMN_NAME = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }
}
