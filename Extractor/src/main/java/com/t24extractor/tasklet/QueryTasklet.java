package com.t24extractor.tasklet;

import com.t24extractor.bi_sync.ops.SqlOps;
import com.t24extractor.bi_sync.ops.OracleOps;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;


@Slf4j
public class QueryTasklet implements Tasklet {
    public static final String WHERE_CLAUSE = "WHERE";

    public static final String AND_CLAUSE = "AND";
    private static final CharSequence SPACE = " ";

    private final JdbcTemplate jdbcTemplate;

    private final SqlOps sqlOps = new OracleOps();

    public QueryTasklet(@Qualifier("jdbcTemplateT24") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Setter
    private String tableName;

    @Setter
    private String chargement;
    @Setter
    private String lastDate;
    @Setter
    private String columnRef;
    @Setter
    private String futureDate;
    @Setter
    private String dateFormat;

    @Override
    public RepeatStatus execute(StepContribution stepContribution, @NonNull ChunkContext chunkContext) throws Exception {
        String type = String.valueOf(stepContribution.getStepExecution().getJobExecution().getExecutionContext().get("Type", Character.class));

        String dataQuery = getDataQuery();

        if ("X".equalsIgnoreCase(type)) {
            chunkContext.getStepContext()
                    .getStepExecution()
                    .getJobExecution()
                    .getExecutionContext()
                    .put("QUERY", dataQuery);
        }

        return RepeatStatus.FINISHED;
    }

    private String getDataQuery() {
        String dataQuery = String.format("SELECT  RECID , XMLRECORD FROM %s", this.tableName);
        dataQuery = String.join(SPACE, dataQuery, getQueryCondition());


        StringBuilder partitioningQuery = new StringBuilder("WHERE MOD(ORA_HASH(ROWID), ?) = ?");

        if (getQueryCondition().contains(WHERE_CLAUSE))
            partitioningQuery.replace(0, WHERE_CLAUSE.length(), AND_CLAUSE);

        if (sqlOps.countAll(jdbcTemplate, tableName) >= 1_000)
            dataQuery = String.join(SPACE, dataQuery, partitioningQuery);


        return dataQuery;

    }

    private String getQueryCondition() {
        switch (chargement) {
            case "Full", "Incremental-id" -> {
                return "";
            }
            case "Incremental" -> {
                return incrementalCondition();
            }
            case "Incremental-specific" -> {
                return incrementalSpecificCondition();
            }
            default -> {
                return "";
            }
        }
    }

    private String incrementalCondition() {
        if (isBlank(columnRef)) {
            return "";
        }
        if (isBlank(futureDate)) {
            if ("00000000".equals(lastDate)) {
                return "";
            }
            return isVarcharDate()
                    ? WHERE_CLAUSE + columnRef + " > " + "'" + lastDate + "'"
                    : WHERE_CLAUSE + columnRef + " > " + lastDate;
        }
        return isVarcharDate()
                ? WHERE_CLAUSE + columnRef + " > '" + lastDate + "'" + " AND " + columnRef + " <= '" + futureDate + "'"
                : WHERE_CLAUSE + columnRef + " > " + lastDate + " AND " + columnRef + " <= " + futureDate;
    }

    private String incrementalSpecificCondition() {
        if ("000000".equals(lastDate)) {
            return "";
        }
        return " WHERE RECID like CONCAT('%-', to_char(sysdate, 'yyyymm'))" +
                " OR RECID like CONCAT('%-', to_char(sysdate, 'yyyymm')-1)";
    }

    private boolean isVarcharDate() {
        return isBlank(dateFormat) || "varchar".equalsIgnoreCase(dateFormat);
    }

    private boolean isBlank(String value) {
        return value == null || value.isEmpty();
    }
}
