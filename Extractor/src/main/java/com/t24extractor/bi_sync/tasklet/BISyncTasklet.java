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

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.t24extractor.commons.Constants.*;

@Component
@Slf4j
public class BISyncTasklet implements AbstractBISyncTasklet, Tasklet {

    private final SchemaSourceRepository schemaSourceRepository;

    private final SchemaSourceAuditRepository schemaSourceAuditRepository;

    private final MetaDataRepository metaDataRepository;

    private final ScheduledOpsRepository scheduledOpsRepository;


    private final SqlOps sqlOps;

    private final Renamer renamer;


    private final JdbcTemplate jdbcTemplate;

    public BISyncTasklet(
            SchemaSourceRepository schemaSourceRepository,
            SchemaSourceAuditRepository schemaSourceAuditRepository,
            MetaDataRepository metaDataRepository,
            ScheduledOpsRepository scheduledOpsRepository,
            @Qualifier("jdbcBI") JdbcTemplate jdbcTemplate) {
        this.schemaSourceRepository = schemaSourceRepository;
        this.schemaSourceAuditRepository = schemaSourceAuditRepository;
        this.metaDataRepository = metaDataRepository;
        this.scheduledOpsRepository = scheduledOpsRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.sqlOps = new SqlServerOps();
        this.renamer = new Renamer();
    }

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

            stepContribution.setExitStatus(new ExitStatus(NEW_TABLE));
            stepContribution.getStepExecution().getJobExecution().setExitStatus(new ExitStatus(NEW_TABLE));
            return RepeatStatus.FINISHED;
        }

        processSchemaChanges(biTable);
        populateExecutionContext(stepContribution, biTable);
        return RepeatStatus.FINISHED;
    }

    private void processSchemaChanges(String biTable) {
        List<SchemaSourceAudit> changesList = schemaSourceAuditRepository.findAllBySourceTableName(biTable);
        if (changesList.isEmpty()) {
            return;
        }

        for (SchemaSourceAudit change : changesList) {
            int diff = change.newSourceEnabled() - change.oldSourceEnabled();
            if (diff == 1 && change.dataType().contains(MAX)) {
                SchemaSource sch = schemaSourceRepository.findAllByTableNameAndColumnName(change.sourceTableName(), change.ColumnName())
                        .orElseThrow(() -> new RuntimeException("La colonne saisie n'existe pas dans la table sourceSchema!"));
                List<MetaDataTable> metaDataTableList = new ArrayList<>();
                metaDataTableList.add(new MetaDataTable(biTable, change.ColumnName(), sch.isLocalRef()));
                sqlOps.save(jdbcTemplate, metaDataTableList);
            }
            if (diff == -1 && change.dataType().contains(MAX)) {
                SchemaSource sch = schemaSourceRepository.findAllByTableNameAndColumnName(change.sourceTableName(), change.ColumnName())
                        .orElseThrow(() -> new RuntimeException("La colonne saisie n'existe pas dans la table sourceSchema!"));
                metaDataRepository.delete(new MetaDataTable(biTable, change.ColumnName(), sch.isLocalRef()));
            }

            updateColumn(biTable, diff, change.ColumnName(), change.dataType());
            int diffIndex = change.newSourceIndexed() - change.oldSourceIndexed();
            updateIndex(biTable, diffIndex, change.ColumnName());
        }
        schemaSourceAuditRepository.deleteAll(changesList);
    }

    /**
     * Alimente le contexte d'exécution avec des artefacts COHÉRENTS entre eux :
     * le mapping XML (BI_STANDARD_SELECTION), la liste des colonnes (COLUMNS)
     * et l'INSERT (SQL_QUERY) sont désormais tous dérivés du MÊME ensemble.
     *
     * Corrige deux défauts de l'ancienne version :
     *  1. COLUMNS / SQL_QUERY étaient construits depuis les colonnes
     *     « _enabled = 1 » brutes, alors que le mapping XML était filtré par
     *     le STANDARD.SELECTION → une colonne active mais absente du
     *     STANDARD.SELECTION (ex. BILL_REFERENCE) entrait dans l'INSERT sans
     *     exister dans la table cible ni être alimentée par le processeur,
     *     faisant échouer le writer (« Unable to retrieve data from the
     *     source » en mode useBulkCopyForBatchInsert).
     *  2. Deux HashSet distincts étaient créés pour COLUMNS et SQL_QUERY :
     *     l'alignement ordre des colonnes ↔ ordre des placeholders ne tenait
     *     qu'à la stabilité d'itération de HashSet. Un unique LinkedHashSet
     *     est maintenant partagé par les deux constructions.
     */
    private void populateExecutionContext(StepContribution stepContribution, String biTable) {

        // 1. Nettoyage du STANDARD.SELECTION — la référence unique du pipeline.
        JSONObject cleanedSS = ssMapClean(biTable, this.ss);

        // 2. Colonnes actives, filtrées par le même critère que le mapping XML.
        Set<SchemaSource> insertable = schemaSourceRepository
                .findSchemaSourceByTableNameAndEnabled(biTable, true)
                .stream()
                .filter(column -> {
                    boolean mapped = cleanedSS.has(column.getColumnName());
                    if (!mapped) {
                        log.warn("Colonne '{}' active dans SCHEMA_SOURCE mais absente du "
                                        + "STANDARD.SELECTION : exclue de l'INSERT pour la table '{}'",
                                column.getColumnName(), biTable);
                    }
                    return mapped;
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // 3. Auto-réparation : toute colonne insérable encore absente de la
        //    table cible est matérialisée (couvre les activations dont l'audit
        //    a été consommé sans que l'ALTER TABLE ait réellement abouti).
        ensureDestColumns(biTable, insertable);

        stepContribution.getStepExecution().getJobExecution().getExecutionContext()
                .put(BI_STANDARD_SELECTION, cleanedSS.toString());
        stepContribution.getStepExecution().getJobExecution().getExecutionContext()
                .put("COLUMNS", sqlOps.fieldsToInsert(insertable));
        stepContribution.getStepExecution().getJobExecution().getExecutionContext()
                .put("SQL_QUERY", sqlOps.insertConstruction(biTable, insertable));
    }

    /**
     * Garantit que chaque colonne destinée à l'INSERT existe physiquement
     * dans la table BI — le pipeline redevient auto-cohérent même si la
     * table et SCHEMA_SOURCE ont divergé (run interrompu, audit purgé…).
     */
    private void ensureDestColumns(String biTable, Set<SchemaSource> insertable) {
        Set<String> existing = sqlOps.destColumns(jdbcTemplate, biTable);
        for (SchemaSource column : insertable) {
            if (!existing.contains(column.getColumnName())) {
                log.info("Colonne '{}' manquante dans la table '{}' : création automatique ({})",
                        column.getColumnName(), biTable, column.getDataType());
                sqlOps.addCol(jdbcTemplate, biTable, column.getColumnName(), column.getDataType());
            }
        }
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
        //
    }

    @Override
    public void updateIndex(String tableName, int diff, String column) {
        if (diff == 1) {
            if (countTable(jdbcTemplate, tableName) > 10000000) {
                scheduledOpsRepository.save(new ScheduledOps(null, sqlOps.escapeColumn(tableName), sqlOps.escapeColumn(column), "I"));
            } else {
                sqlOps.createIndex(jdbcTemplate, tableName, column);
            }
        }
        if (diff == -1) sqlOps.dropIndex(jdbcTemplate, column);
    }

    @Override
    public void updateColumn(String table, int diff, String column, String dataType) {
        if (diff == 1) {
            sqlOps.addCol(jdbcTemplate, table, column, dataType);

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
            String columnName = schemaSource.getColumnName();
            if (ssMap.has(columnName) && !ssMap.isNull(columnName)) {
                newSSMap.put(columnName, ssMap.get(columnName));
            } else {
                log.warn("Colonne '{}' absente du STANDARD.SELECTION pour la table '{}', ignorée pendant BI_SYNC", columnName, tableName);
            }
        }
        ssMap.clear();
        return newSSMap;
    }

    public Long countTable(JdbcTemplate jdbcTemplate, String tableName) {
        return sqlOps.countAll(jdbcTemplate, tableName);
    }


}