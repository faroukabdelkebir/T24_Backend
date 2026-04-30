package com.t24extractor.tasklet;

import com.t24extractor.bi_sync.ops.SqlOps;
import com.t24extractor.bi_sync.ops.SqlServerOps;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;


@Slf4j
public class QueryTasklet implements Tasklet {
    public static final String WHERE_CLAUSE = "WHERE";

    public static final String AND_CLAUSE = "AND";
    private static final CharSequence SPACE = " ";

    @Autowired
    @Qualifier("jdbcTemplateT24")
    private JdbcTemplate jdbcTemplate;

    private final SqlOps sqlOps = new SqlServerOps();

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


        StringBuilder partitioningQuery = new StringBuilder("WHERE ABS(CHECKSUM(%% physloc %%)) %  ? = ?");

        if (getQueryCondition().contains(WHERE_CLAUSE))
            partitioningQuery.replace(0, WHERE_CLAUSE.length(), AND_CLAUSE);

        if (sqlOps.countAll(jdbcTemplate, tableName) >= 1_000)
            dataQuery = String.join(SPACE, dataQuery, partitioningQuery);


        return dataQuery;

    }

    private String getQueryCondition() {
        String queryCondition = "";

        switch (chargement) {
            case "Full", "Incremental-id" -> queryCondition = "";
            case "Incremental" -> {
                if ((columnRef == null) || (columnRef.isEmpty())) {
                    queryCondition = "";
                } else {
                    if ((futureDate == null) || (futureDate.isEmpty())) {
                        if (lastDate.equals("00000000")) {
                            queryCondition = "";
                        } else {
                            if ((dateFormat == null) || dateFormat.isEmpty() || dateFormat.equalsIgnoreCase("varchar")) {
                                queryCondition = " WHERE " + columnRef + " > " + "'" + lastDate + "'";
                            } else {
                                queryCondition = " WHERE " + columnRef + " > " + lastDate;
                            }
                        }
                    } else {
                        if ((dateFormat == null) || dateFormat.isEmpty() || dateFormat.equalsIgnoreCase("varchar")) {
                            queryCondition = " WHERE " + columnRef + " > '" + lastDate + "'" +
                                    " AND " + columnRef + " <= '" + futureDate + "'";
                        } else {
                            queryCondition = " WHERE " + columnRef + " > " + lastDate +
                                    " AND " + columnRef + " <= " + futureDate;
                        }
                    }
                }
            }
            case "Incremental-specific" -> {
                if (lastDate.equals("000000")) {
                    queryCondition = "";
                } else {
                    queryCondition = " WHERE RECID like CONCAT('%-', to_char(sysdate, 'yyyymm'))" +
                            " OR RECID like CONCAT('%-', to_char(sysdate, 'yyyymm')-1)";
                }
            }
        }
        return queryCondition;
    }
}