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

//    @Override
//    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
//
//        JSONObject nameJs = new JSONObject(
//                stepContribution.getStepExecution()
//                        .getJobExecution()
//                        .getExecutionContext()
//                        .getString(BI_STANDARD_SELECTION)
//        );
//        String name = this.table;
//
//        Iterator<String> iterator = nameJs.keys();
//
//        while (iterator.hasNext()) {
//            String key = iterator.next();
//
//            // ✅ Vérifier que la colonne existe dans la table principale avant tout
//            if (!columnExistsInMainTable(name, key)) {
//                // La colonne est absente de la table principale (XMLRECORD ne contenait jamais ce champ)
//                // On skip silencieusement
//                continue;
//            }
//
//            if (!key.contains("Local_Ref") && !key.equals("DATE_TIME")
//                    && nameJs.get(key).toString().contains("MAX")) {
//
//                // ✅ Créer la table MV si elle n'existe pas
//                String createTable = "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name='"
//                        + name + "#" + key + "') "
//                        + "CREATE TABLE [" + name + "#" + key + "] ("
//                        + "[" + ID + "]" + ID_TYPE + ", \n"
//                        + "[" + key + "] nvarchar(355) ,\n"
//                        + "[MV_SEQUENCE] int ,\n"
//                        + "[SEQUENCE] int \n"  // ✅ virgule de fin supprimée
//                        + ")";
//                jdbcTemplate.execute(createTable);
//
//                // ✅ Exécuter chaque INSERT séparément
//                String insertQuery = "INSERT INTO [" + name + "#" + key + "] (_id, " + key + ", [MV_SEQUENCE], [SEQUENCE])\n"
//                        + "SELECT _id, lda." + key + ", lda.[MV_SEQUENCE], lda.[SEQUENCE]\n"
//                        + "FROM " + name + " AS ld\n"
//                        + "CROSS APPLY OPENJSON(ld." + key + ")\n"
//                        + "WITH (\n"
//                        + "  " + key + " nvarchar(255) N'strict $." + key + "',\n"
//                        + "  [MV_SEQUENCE] int N'$.mvsequence',\n"
//                        + "  [SEQUENCE] int N'$.sequence'\n"
//                        + ") AS lda";
//                jdbcTemplate.execute(insertQuery); // ✅ execute() et non batchUpdate()
//
//            } else if (key.contains("Local_Ref") && key.contains("MAX")) {
//
//                String createTable = "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name='"
//                        + name + "#" + key + "') "
//                        + "CREATE TABLE [" + name + "#" + key + "] ("
//                        + "[" + ID + "]" + ID_TYPE + ", \n"
//                        + "[" + key + "] nvarchar(355) ,\n"
//                        + "[SEQUENCE] int \n"  // ✅ virgule de fin supprimée
//                        + ")";
//                jdbcTemplate.execute(createTable);
//
//                String insertQuery = "INSERT INTO [" + name + "#" + key + "] (_id, [" + key + "], [SEQUENCE])\n"
//                        + "SELECT _id, lf." + key + ", lf.[SEQUENCE]\n"
//                        + "FROM " + name + " AS LF_table\n"
//                        + "CROSS APPLY OPENJSON(LF_table." + key + ")\n"
//                        + "WITH (\n"
//                        + "  " + key + " nvarchar(355) N'$." + key + "',\n"
//                        + "  [SEQUENCE] int N'$.sequence'\n"
//                        + ") AS lf";
//                jdbcTemplate.execute(insertQuery);
//            }
//        }
//
//        return RepeatStatus.FINISHED;
//    }
//
//    // ✅ Méthode utilitaire : vérifie que la colonne existe dans la table principale SQL Server
//    private boolean columnExistsInMainTable(String tableName, String columnName) {
//        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
//                + "WHERE TABLE_NAME = ? AND COLUMN_NAME = ?";
//        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName, columnName);
//        return count != null && count > 0;
//    }
}
