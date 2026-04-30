package com.t24extractor.bi_sync.tasklet;

import com.t24extractor.bi_sync.ops.SqlOps;
import com.t24extractor.bi_sync.ops.SqlServerOps;
import com.t24extractor.model.MetaDataTable;
import com.t24extractor.model.ScheduledOps;
import com.t24extractor.model.SchemaSource;
import com.t24extractor.model.SchemaSourceAudit;
import com.t24extractor.repository.bi_repos.MetaDataRepository;
import com.t24extractor.repository.bi_repos.ScheduledOpsRepository;
import com.t24extractor.repository.bi_repos.SchemaSourceAuditRepository;
import com.t24extractor.repository.bi_repos.SchemaSourceRepository;
import com.t24extractor.utils.Renamer;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.t24extractor.commons.Constants.*;

@Component
@Slf4j
public class BISyncTasklet implements AbstractBISyncTasklet, Tasklet {

    @Autowired
    SchemaSourceRepository schemaSourceRepository;

    @Autowired
    SchemaSourceAuditRepository schemaSourceAuditRepository;

    @Autowired
    MetaDataRepository metaDataRepository;

    @Autowired
    ScheduledOpsRepository scheduledOpsRepository;


    SqlOps sqlOps = new SqlServerOps();

    Renamer renamer = new Renamer();


    @Autowired
    @Qualifier("JdbcBI")
    JdbcTemplate jdbcTemplate;

    @Setter
    JSONObject ss;

    @Setter
    String t24TableName;

    @Setter
    String colonneRef;

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {

        String biTable = renamer.biRenamer(this.t24TableName);

        //Creation des colonnes par defaut _id,ORIGINAL_ID et INSERT_DATE
        log.info("Création des colonnes par défaut pour la table {}", biTable);
        sqlOps.createDestTable(jdbcTemplate, biTable, new HashSet<>(schemaSourceRepository.findSchemaSourceByTableNameAndEnabled(this.t24TableName, true)));

        //Vérification si la table en cours de chargement avait ses colonnes traitées dans la table SCHEMA_SOURCE ou pas.
        if (!statusSchemaSource(biTable)) {

            //Alimentation de la table SCHEMA_SOURCE avec les données la table
            log.info("Alimenter du schéma dans la table SchemaSource");
            loadSchemaSource(biTable, this.ss);

//            emailService.sendMailUnableToLoadTable(biTable, String.format("La création du schéma pour la table %s a été achevée. Veuillez paramétrer le schéma et ré-exécuter le job.", biTable));
            stepContribution.setExitStatus(new ExitStatus(NEW_TABLE));
            stepContribution.getStepExecution().getJobExecution().setExitStatus(new ExitStatus(NEW_TABLE));
            return RepeatStatus.FINISHED;
        } else {

            List<SchemaSourceAudit> changesList = schemaSourceAuditRepository.findAllBySourceTableName(biTable);
            if (!changesList.isEmpty()) {
                for (SchemaSourceAudit change : changesList) {
                    int diff = change.newSourceEnabled() - change.oldSourceEnabled();
                    if (diff == 1) {
                        if (change.dataType().contains(MAX)) {
                            SchemaSource sch = schemaSourceRepository.findAllByTableNameAndColumnName(change.sourceTableName(), change.ColumnName()).orElseThrow(() -> new RuntimeException("La colonne saisie n'existe pas dans la table sourceSchema!"));
                            List<MetaDataTable> metaDataTableList = new ArrayList<>();
                            metaDataTableList.add(new MetaDataTable(biTable, change.ColumnName(), sch.isLocalRef()));
                            sqlOps.save(jdbcTemplate, metaDataTableList);
                        }
                    }
                    if (diff == -1) {
                        if (change.dataType().contains(MAX)) {
                            SchemaSource sch = schemaSourceRepository.findAllByTableNameAndColumnName(change.sourceTableName(), change.ColumnName()).orElseThrow(() -> new RuntimeException("La colonne saisie n'existe pas dans la table sourceSchema!"));
                            metaDataRepository.delete(new MetaDataTable(biTable, change.ColumnName(), sch.isLocalRef()));
                        }
                    }
                    updateColumn(biTable, diff, change.ColumnName(), change.dataType());

                    int diffIndex = change.newSourceIndexed() - change.oldSourceIndexed();
                    updateIndex(biTable, diffIndex, change.ColumnName());
                }
                schemaSourceAuditRepository.deleteAll(changesList);
            }

            stepContribution.getStepExecution().getJobExecution().getExecutionContext().put(BI_STANDARD_SELECTION, ssMapClean(biTable, this.ss).toString());
            stepContribution.getStepExecution().getJobExecution().getExecutionContext().put("COLUMNS", sqlOps.fieldsToInsert(new HashSet<>(schemaSourceRepository.findSchemaSourceByTableNameAndEnabled(biTable, true))));
            stepContribution.getStepExecution().getJobExecution().getExecutionContext().put("SQL_QUERY", sqlOps.insertConstruction(biTable, new HashSet<>(schemaSourceRepository.findSchemaSourceByTableNameAndEnabled(biTable, true))));
        }
        return RepeatStatus.FINISHED;
    }


    /**
     * @param tableName Table BI
     * @return True/False
     */
    @Override
    public boolean statusSchemaSource(String tableName) {
        return schemaSourceRepository.countByTableName(tableName) != 0;
    }

    /**
     * @param tableName
     * @param schemaSourceList
     */
    @Override
    public void loadMetaDataTable(String tableName, @NotNull List<SchemaSource> schemaSourceList) {
        List<MetaDataTable> metaList = new ArrayList<>();
        for (SchemaSource row : schemaSourceList) {
            if (row.isLocalRef()) metaList.add(new MetaDataTable(tableName, row.getColumnName(), true));
            if (!row.isLocalRef()) metaList.add(new MetaDataTable(tableName, row.getColumnName(), false));
        }
        sqlOps.save(jdbcTemplate, metaList);
    }

    @Override
    public void checkSchemaUpdate(String tableName) {
        schemaSourceAuditRepository.findAllBySourceTableName(tableName);
    }

    @Override
    public void scheduleOp(String tableName) {
        //TODO
    }

    @Override
    public void updateIndex(String tableName, int diff, String column) {
        if (diff == 1)
            if (countTable(jdbcTemplate, tableName) > 10000000) {
                scheduledOpsRepository.save(new ScheduledOps(null, sqlOps.escapeColumn(tableName), sqlOps.escapeColumn(column), "I"));
            } else {
                sqlOps.createIndex(jdbcTemplate, tableName, column);
            }
        if (diff == -1) sqlOps.dropIndex(jdbcTemplate, column);
    }

    @Override
    public void updateColumn(String table, int diff, String column, String dataType) {
        if (diff == 1) {
            sqlOps.addCol(jdbcTemplate, table, column, dataType);

            //TODO
           /* if (countTable(jdbcTemplate, tableName) > 10000000){
                scheduledOpsRepository.save(new ScheduledOps(sqlOps.escapeColumn(tableName), sqlOps.escapeColumn(column), 'C'));
            }*/
        }
        if (diff == -1) sqlOps.dropCol(jdbcTemplate, table, column);
    }

    /**
     * @param tableName Nom de la table t24 sur BI
     * @param ss        Le dictionnaire du STANDARD.SELETCION crée dans le step STANDARD_SELECTION
     */
    @Override
    public void loadSchemaSource(String tableName, @NotNull JSONObject ss) {

        // alimenting table in mysqlServer ( the data already exist )
    }

    @Override
    public JSONObject ssMapClean(String tableName, JSONObject ssMap) {
        List<SchemaSource> list = schemaSourceRepository.findAll(tableName);
        JSONObject newSSMap = new JSONObject();
        for (SchemaSource schemaSource : list) {
            newSSMap.put(schemaSource.getColumnName(), ssMap.get(schemaSource.getColumnName()));
        }
        ssMap.clear();
        return newSSMap;
    }

    public Long countTable(JdbcTemplate jdbcTemplate, String tableName) {
        return sqlOps.countAll(jdbcTemplate, tableName);
    }


    /**
     * TODO
     */

    /*public String scheduledQuery(String tableName, String colonneRef, long lastDate){
        return String.format("SELECT * FROM () WHERE %s < %s", colonneRef, lastDate);
    }*/

}
