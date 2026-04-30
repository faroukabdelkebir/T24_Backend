package com.t24extractor.tasklet;

import lombok.Setter;
import org.json.JSONObject;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Iterator;

import static com.t24extractor.commons.Constants.*;

@Component
@StepScope
public class Json_Flattener implements Tasklet {

    @Autowired
    @Qualifier("JdbcBI")
    private JdbcTemplate jdbcTemplate;

    @Setter
    protected String table;

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {

        JSONObject nameJs = new JSONObject(stepContribution.getStepExecution().getJobExecution().getExecutionContext().getString(BI_STANDARD_SELECTION));
        String name = this.table;

        StringBuilder query = new StringBuilder();
        Iterator<String> iterator = nameJs.keys();
        StringBuilder sb = new StringBuilder();
        while (iterator.hasNext()) {
            String key = iterator.next();

            if (!key.contains("Local_Ref") && !key.equals("DATE_TIME") && nameJs.get(key).toString().contains("MAX")) {
                sb.append("IF NOT EXISTS (SELECT * FROM sys.tables WHERE name='")
                        .append(name).append("#")
                        .append(key).append("') ")
                        .append("CREATE TABLE ").append(name).append("#").append(key)
                        .append("(")
                        .append("[").append(ID).append("]").append(ID_TYPE).append(", \n")
                        .append("[").append(key).append("]").append("nvarchar(355)").append(" ,\n")
                        .append("[MV_SEQUENCE]").append("int").append(" ,\n")
                        .append("[SEQUENCE]").append("int").append(" ,\n")
                        .append(")");
                query.append("INSERT INTO ").append("[").append(name).append("#").append(key).append("]").append(" (_id ").append(",")
                        .append(key).append(", [MV_SEQUENCE], [SEQUENCE])\n")
                        .append("SELECT _id, lda.").append(key).append(",lda.[MV_SEQUENCE],lda.[SEQUENCE]")
                        .append("  from ").append(name).append(" as ld\n").append(
                                "  cross apply openjson(ld.").append(key).append(")\n").append("  with(\n").append(key).append(" nvarchar(255) N'strict $.").append(key).append("',\n").append("    [MV_SEQUENCE] int N'$.mvsequence',\n").append("   [SEQUENCE]  int N'$.sequence') as lda;\n").append("\n");

            } else if (key.contains("Local_Ref") && key.contains("MAX")) {
                sb.append("IF NOT EXISTS (SELECT * FROM sys.tables WHERE name='")
                        .append(name).append("#")
                        .append(key).append("') ")
                        .append("CREATE TABLE ").append(name).append("#").append(key)
                        .append("(")
                        .append("[").append(ID).append("]").append(ID_TYPE).append(", \n")
                        .append("[").append(key).append("]").append("nvarchar(355)").append(" ,\n")
                        .append("[SEQUENCE]").append("int").append(" ,\n")
                        .append(")");


                String multi_lf =
                        "INSERT INTO " + name + "#" + key + "(_id, ["+key+"]"+", [SEQUENCE]\n"
                        + "select  _id, lf." + key + ",lf.[SEQUENCE]\n" +
                        "  from " + name + " as LF_table \n" +
                        "cross apply openjson (field_js." + key + ")\n" + "with \n (\n    " + key + "  nvarchar(355) '$." + key + "',\n" + "   [SEQUENCE]  int N'$.sequence') as lf;\n";
                query.append(multi_lf);
            }
        }

        jdbcTemplate.execute(sb.toString());
        jdbcTemplate.batchUpdate(query.toString());
        return RepeatStatus.FINISHED;
    }
}
