package com.t24extractor.configuration;

import com.t24extractor.preparedstatementsetter.ItemPrepStmtSetter;
import com.t24extractor.bi_sync.tasklet.BISyncTasklet;
import com.t24extractor.commons.Constants;
import com.t24extractor.decider.ExecutionFlowDecider;
import com.t24extractor.listeners.JobListener;
import com.t24extractor.model.T24Table;
import com.t24extractor.partitioner.MssqlPartitioner;
import com.t24extractor.processor.BlobFilterProcessor;
import com.t24extractor.processor.XMLFilterProcessor;
import com.t24extractor.processor.XMLProcessor;
import com.t24extractor.repository.bi_repos.MetaDataRepository;
import com.t24extractor.repository.bi_repos.ScheduledOpsRepository;
import com.t24extractor.repository.bi_repos.SchemaSourceAuditRepository;
import com.t24extractor.repository.bi_repos.SchemaSourceRepository;
import com.t24extractor.repository.t24_repo.StandardSelectionRepository;
import com.t24extractor.repository.t24_repo.TafjVocRepository;
import com.t24extractor.rowmapper.BlobRowMapper;
import com.t24extractor.rowmapper.XMLRowMapper;
import com.t24extractor.tasklet.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.JdbcCursorItemReader;
import org.springframework.batch.infrastructure.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;

import static com.t24extractor.commons.Constants.*;


@Configuration
@ComponentScan(basePackages = "com.t24extractor")
@Import({BIDataSourceConfiguration.class, T24DataSourceConfiguration.class})
public class BatchConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(BatchConfiguration.class);


    @Value("${workers.number}")
    private int workersNumber;

    private final DataSource dataSource;

    private final DataSource biDataSource;

    private final CobStatusTasklet cobStatusTasklet;

    @Autowired
    public BatchConfiguration(
            CobStatusTasklet cobStatusTasklet,
            @Qualifier("t24") DataSource dataSource,
            @Qualifier("bi") DataSource biDataSource) {
        this.cobStatusTasklet = cobStatusTasklet;
        this.dataSource = dataSource;
        this.biDataSource = biDataSource;
    }

    /***************************************************************************
     * <p>
     *	Listeners
     *
     ***************************************************************************/

    @Bean
    public JobExecutionListener jobListener(@Qualifier("jdbcBI") JdbcTemplate jdbcTemplate) {
        return new JobListener(jdbcTemplate);
    }


    /***************************************************************************
     * <p>
     *	Tasklets
     *
     ***************************************************************************/

    @Bean
    public StandardSelectionTasklet
    standardSelectionCustomer(@Qualifier("jdbcTemplateT24") JdbcTemplate jdbcExecutor,
                              @Qualifier("jdbcBI") JdbcTemplate jdbcBatchExecutor,
                              TafjVocRepository tafjVocRepository,
                              StandardSelectionRepository standardSelectionRepository) {
        StandardSelectionTasklet myTasklet = new StandardSelectionTasklet(jdbcBatchExecutor, tafjVocRepository, standardSelectionRepository);

        myTasklet.setJdbcExecutor(jdbcExecutor);
        myTasklet.setT24TableName(CUSTOMER);
        myTasklet.setTableType("X");
        myTasklet.setOracleTableName(FBNK_CUSTOMER);

        return myTasklet;
    }

    @Bean
    public StandardSelectionTasklet
    standardSelectionFT(@Qualifier("jdbcTemplateT24") JdbcTemplate jdbcExecutor,
                        @Qualifier("jdbcBI") JdbcTemplate jdbcBatchExecutor,
                        TafjVocRepository tafjVocRepository,
                        StandardSelectionRepository standardSelectionRepository) {
        StandardSelectionTasklet myTasklet = new StandardSelectionTasklet(jdbcBatchExecutor, tafjVocRepository, standardSelectionRepository);

        myTasklet.setJdbcExecutor(jdbcExecutor);
        myTasklet.setT24TableName(FUNDS_TRANSFER_HIS);
        myTasklet.setTableType("X");
        myTasklet.setOracleTableName("FBNK_FUNDS_TRANSFER001");

        return myTasklet;
    }

    @Bean
    public StandardSelectionTasklet
    standardSelectionAABill(@Qualifier("jdbcTemplateT24") JdbcTemplate jdbcExecutor,
                            @Qualifier("jdbcBI") JdbcTemplate jdbcBatchExecutor,
                            TafjVocRepository tafjVocRepository,
                            StandardSelectionRepository standardSelectionRepository) {
        StandardSelectionTasklet myTasklet = new StandardSelectionTasklet(jdbcBatchExecutor, tafjVocRepository, standardSelectionRepository);

        myTasklet.setJdbcExecutor(jdbcExecutor);
        myTasklet.setT24TableName(AA_BILL_DETAILS);
        myTasklet.setTableType("X");
        myTasklet.setOracleTableName(FBNK_BILL_DETAILS);

        return myTasklet;
    }

    @Bean
    public TableNameTasklet tableNameCustomer(TafjVocRepository tafjVocRepository, FileControlServiceImpl fileControlService) {
        TableNameTasklet tableNameTasklet = new TableNameTasklet(tafjVocRepository, fileControlService);
        tableNameTasklet.setT24TableName(CUSTOMER);
        return tableNameTasklet;
    }

    @Bean
    public TableNameTasklet tableNameFT(TafjVocRepository tafjVocRepository, FileControlServiceImpl fileControlService) {
        TableNameTasklet tableNameTasklet = new TableNameTasklet(tafjVocRepository, fileControlService);
        tableNameTasklet.setT24TableName(FUNDS_TRANSFER_HIS);
        return tableNameTasklet;
    }

    @Bean
    public TableNameTasklet tableNameAABill(TafjVocRepository tafjVocRepository, FileControlServiceImpl fileControlService) {
        TableNameTasklet tableNameTasklet = new TableNameTasklet(tafjVocRepository, fileControlService);
        tableNameTasklet.setT24TableName(AA_BILL_DETAILS);
        return tableNameTasklet;
    }

    @Bean
    public QueryTasklet queryCustomerTasklet(@Qualifier("jdbcTemplateT24") JdbcTemplate jdbcTemplateT24) {
        QueryTasklet queryCusTasklet = new QueryTasklet(jdbcTemplateT24);
        queryCusTasklet.setColumnRef(null);
        queryCusTasklet.setDateFormat(null);
        queryCusTasklet.setTableName(FBNK_CUSTOMER);
        queryCusTasklet.setChargement("Full");
        queryCusTasklet.setLastDate(null);
        return queryCusTasklet;
    }

    @Bean
    public QueryTasklet queryFtTasklet(@Qualifier("jdbcTemplateT24") JdbcTemplate jdbcTemplateT24) {
        QueryTasklet queryCusTasklet = new QueryTasklet(jdbcTemplateT24);
        queryCusTasklet.setColumnRef("DEBIT_VALUE_DATE");
        queryCusTasklet.setDateFormat("VARCHAR");
        queryCusTasklet.setTableName("FBNK_FUNDS_TRANSFER001");
        queryCusTasklet.setChargement("Incremental");
        queryCusTasklet.setLastDate(dateString());
        return queryCusTasklet;
    }

    private String dateString() {
        return "00000000";    }

    @Bean
    public QueryTasklet queryAABillTasklet(@Qualifier("jdbcTemplateT24") JdbcTemplate jdbcTemplateT24) {
        QueryTasklet queryCusTasklet = new QueryTasklet(jdbcTemplateT24);
        queryCusTasklet.setColumnRef("PAYMENT_DATE");
        queryCusTasklet.setDateFormat("VARCHAR");
        queryCusTasklet.setTableName(FBNK_BILL_DETAILS);
        queryCusTasklet.setChargement("Incremental");
        queryCusTasklet.setLastDate(dateString());
        return queryCusTasklet;
    }



    /***************************************************************************
     * <p>
     *	XML Reader
     *
     ***************************************************************************/

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<T24Table>
    oracleXmlReader(@Value("#{jobExecutionContext['QUERY']}") String query,
                    @Value("#{stepExecutionContext['value']}") int value)  {


        LOG.info("[Reader Query] : \n{}", query);

        JdbcCursorItemReader<T24Table> reader = new JdbcCursorItemReader<>(dataSource, query, xmlRowMapper());
        reader.setFetchSize(FETCH_SIZE);
        reader.setConnectionAutoCommit(false);
        if (query.contains("ABS")) {
            reader.setPreparedStatementSetter(ps ->
            {
                ps.setInt(1, workersNumber);
                ps.setInt(2, value);
            });
        }
        return new SynchronizedItemStreamReader<>(reader);
    }

    @Bean
    public XMLRowMapper xmlRowMapper() {
        return new XMLRowMapper();
    }

    /***************************************************************************
     * <p>
     *	BLOB/CLOB Reader
     *
     ***************************************************************************/

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<HashMap<String, Object>>
    oracleBlobReader(@Value("#{jobExecutionContext['VIEW_QUERY']}") String query,
                     @Value("#{stepExecutionContext['minValue']}") Long minValue,
                     @Value("#{stepExecutionContext['maxValue']}") Long maxValue)  {
        String readerQuery = "SELECT * FROM ( " + query + " ) " +
                "WHERE rowNumber >= " + minValue + " " +
                "AND rowNumber <= " + maxValue;

        LOG.trace("[minValue - maxValue] : {} -> {}", minValue, maxValue);
        LOG.trace("[Reader Query] : \n{}", readerQuery);

        JdbcCursorItemReader<HashMap<String, Object>> reader = new JdbcCursorItemReader<>(dataSource, readerQuery, blobRowMapper());
        reader.setFetchSize(FETCH_SIZE);
        reader.setConnectionAutoCommit(false);
        return new SynchronizedItemStreamReader<>(reader);
    }

    @Bean
    @StepScope
    public BlobRowMapper blobRowMapper() {
        return new BlobRowMapper();
    }

    /***************************************************************************
     * <p>
     *	Steps
     *
     ***************************************************************************/

    @Bean
    public Step standardSelectionCustomerStep(@Qualifier("standardSelectionCustomer") StandardSelectionTasklet standardSelectionCustomer, JobRepository jobRepository,@Qualifier("t24TransactionManager") PlatformTransactionManager transactionManager) {
        return new StepBuilder("Standard.Selection.CUSTOMER", jobRepository)
                .tasklet(standardSelectionCustomer, transactionManager)
                .build();
    }

    @Bean
    public Step standardSelectionFTStep(@Qualifier("standardSelectionFT") StandardSelectionTasklet standardSelectionFT, JobRepository jobRepository,@Qualifier("t24TransactionManager") PlatformTransactionManager transactionManager) {
        return new StepBuilder("Standard.Selection.FUNDS.TRANSFER", jobRepository)
                .tasklet(standardSelectionFT, transactionManager)
                .build();
    }

    @Bean
    public Step standardSelectionAABillStep(@Qualifier("standardSelectionAABill") StandardSelectionTasklet standardSelectionAABill, JobRepository jobRepository,@Qualifier("t24TransactionManager") PlatformTransactionManager transactionManager) {
        return new StepBuilder("Standard.Selection.AA.BILL.DETAILS", jobRepository)
                .tasklet(standardSelectionAABill, transactionManager)
                .build();
    }

    @Bean
    public Step tableNameCustomerStep(@Qualifier("tableNameCustomer") TableNameTasklet tableNameCustomer, JobRepository jobRepository,@Qualifier("t24TransactionManager") PlatformTransactionManager transactionManager) {
        return new StepBuilder("TableName.CUSTOMER", jobRepository)
                .tasklet(tableNameCustomer, transactionManager)
                .build();
    }

    @Bean
    public Step tableNameFTStep(@Qualifier("tableNameFT") TableNameTasklet tableNameFT, JobRepository jobRepository,@Qualifier("t24TransactionManager") PlatformTransactionManager transactionManager) {
        return new StepBuilder("TableName.FT$HIS", jobRepository)
                .tasklet(tableNameFT, transactionManager)
                .build();
    }

    @Bean
    public Step tableNameAABillStep(@Qualifier("tableNameAABill") TableNameTasklet tableNameAABill, JobRepository jobRepository,@Qualifier("t24TransactionManager") PlatformTransactionManager transactionManager) {
        return new StepBuilder("TableName.AA.BILL.DETAILS", jobRepository)
                .tasklet(tableNameAABill, transactionManager)
                .build();
    }

    @Bean
    @JobScope
    public Step cusQueryBuildingStep(@Qualifier("queryCustomerTasklet") QueryTasklet queryCustomerTasklet, JobRepository jobRepository,@Qualifier("t24TransactionManager") PlatformTransactionManager transactionManager) {
        return new StepBuilder(QUERY_BUILDING_STEP, jobRepository)
                .tasklet(queryCustomerTasklet, transactionManager)
                .build();
    }

    @Bean
    @JobScope
    public Step ftQueryBuildingStep(@Qualifier("queryFtTasklet") QueryTasklet queryFtTasklet, JobRepository jobRepository,@Qualifier("t24TransactionManager") PlatformTransactionManager transactionManager) {
        return new StepBuilder(QUERY_BUILDING_STEP, jobRepository)
                .tasklet(queryFtTasklet, transactionManager)
                .build();
    }

    @Bean
    @JobScope
    public Step aaQueryBuildingStep(@Qualifier("queryAABillTasklet") QueryTasklet queryAABillTasklet, JobRepository jobRepository,@Qualifier("t24TransactionManager") PlatformTransactionManager transactionManager) {
        return new StepBuilder(QUERY_BUILDING_STEP, jobRepository)
                .tasklet(queryAABillTasklet, transactionManager)
                .build();
    }

    /**********************
     * <p>
     *	Managers Steps
     *
     **********************/

    @Bean
    @JobScope
    public Step cusManager(PartitionHandler cusXMLPartitionHandler, JobRepository jobRepository)
    {
        return new StepBuilder(CUSTOMER, jobRepository)
                .partitioner("cusWorker", partitioner())
                .partitionHandler(cusXMLPartitionHandler)
                //.listener(managerStepListener(null, null, null, null))
                .build();
    }

    @Bean
    @JobScope
    public Step ftManager(PartitionHandler ftXMLPartitionHandler, JobRepository jobRepository) {
        return new StepBuilder(FUNDS_TRANSFER_HIS, jobRepository)
                .partitioner("ftWorker", partitioner())
                .partitionHandler(ftXMLPartitionHandler)
                .build();
    }

    @Bean
    @JobScope
    public Step aaManager(PartitionHandler aaXMLPartitionHandler, JobRepository jobRepository)
    {
        return new StepBuilder(AA_BILL_DETAILS, jobRepository)
                .partitioner("aaWorker", partitioner())
                .partitionHandler(aaXMLPartitionHandler)
                .build();
    }

    @Bean
    public MssqlPartitioner partitioner() {
        return new MssqlPartitioner();
    }

    /**********************
     * <p>
     *	Workers Steps
     *
     **********************/

    @Bean
    public Step cusWorker(JdbcBatchItemWriter<com.alibaba.fastjson2.JSONObject> itemWriter, JobRepository jobRepository,@Qualifier("biTransactionManager") PlatformTransactionManager transactionManager) {
        return new StepBuilder("cusWorker", jobRepository)
                .<T24Table, com.alibaba.fastjson2.JSONObject>chunk(CHUNKS_NUMBER)
                .transactionManager(transactionManager)
                .reader(oracleXmlReader(null, 0))
                .processor(cusXmlProcessor())
                .writer(itemWriter)
                .build();
    }

    @Bean
    public Step ftWorker(JdbcBatchItemWriter<com.alibaba.fastjson2.JSONObject> itemWriter, JobRepository jobRepository,@Qualifier("biTransactionManager") PlatformTransactionManager transactionManager) {
        return new StepBuilder("ftWorker", jobRepository)
                .<T24Table, com.alibaba.fastjson2.JSONObject>chunk(CHUNKS_NUMBER)
                .transactionManager(transactionManager)
                .reader(oracleXmlReader(null, 0))
                .processor(ftXmlProcessor())
                .writer(itemWriter)
                .build();
    }

    @Bean
    public Step aaWorker(JdbcBatchItemWriter<com.alibaba.fastjson2.JSONObject> itemWriter, JobRepository jobRepository,@Qualifier("biTransactionManager") PlatformTransactionManager transactionManager) {
        return new StepBuilder("aaWorker", jobRepository)
                .<T24Table, com.alibaba.fastjson2.JSONObject>chunk(CHUNKS_NUMBER)
                .transactionManager(transactionManager)
                .reader(oracleXmlReader(null, 0))
                .processor(aaBillXmlProcessor())
                .writer(itemWriter)
                .build();
    }


    /***************************************************************************
     * <p>
     *	Processors
     *
     ***************************************************************************/


    @Bean
    @StepScope
    public XMLFilterProcessor xmlFilterProcessor() {
        return new XMLFilterProcessor();
    }

    @Bean
    @StepScope
    public XMLProcessor cusXmlProcessor() {
        XMLProcessor processor = new XMLProcessor();
        processor.setOracleTableName(FBNK_CUSTOMER);
        return processor;
    }

    @Bean
    @StepScope
    public XMLProcessor ftXmlProcessor() {
        XMLProcessor processor = new XMLProcessor();
        processor.setOracleTableName("FBNK_FUNDS_TRANSFER#HIS");
        return processor;
    }

    @Bean
    @StepScope
    public XMLProcessor aaBillXmlProcessor() {
        XMLProcessor processor = new XMLProcessor();
        processor.setOracleTableName(FBNK_BILL_DETAILS);
        return processor;
    }


    @Bean
    @StepScope
    public BlobFilterProcessor blobFilterProcessor() {
        return new BlobFilterProcessor();
    }



    /**
     * *************************************************************************
     * <p>
     * Handlers
     * </p>
     * *************************************************************************
     */

    @Bean
    public PartitionHandler cusXMLPartitionHandler(Step cusWorker) throws Exception {
        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();

        handler.setTaskExecutor(taskExecutor());
        handler.setGridSize(workersNumber);
        handler.setStep(cusWorker);

        handler.afterPropertiesSet();

        return handler;
    }


    @Bean
    public PartitionHandler ftXMLPartitionHandler(Step ftWorker) throws Exception {
        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();

        handler.setTaskExecutor(taskExecutor());
        handler.setGridSize(workersNumber);
        handler.setStep(ftWorker);

        handler.afterPropertiesSet();

        return handler;
    }

    @Bean
    public PartitionHandler aaXMLPartitionHandler(Step aaWorker) throws Exception {
        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();

        handler.setTaskExecutor(taskExecutor());
        handler.setGridSize(workersNumber);
        handler.setStep(aaWorker);

        handler.afterPropertiesSet();

        return handler;
    }

    @Bean
    public Step cobStatus(JobRepository jobRepository,@Qualifier("t24TransactionManager") PlatformTransactionManager platformTransactionManager) {
        return new StepBuilder(String.join("", Constants.STEP_COB_STATUS), jobRepository)
                .tasklet(cobStatusTasklet, platformTransactionManager)
                .build();
    }

    @Bean
    @JobScope
    public Step bISyncCus(BISyncTasklet biSyncCusTasklet, JobRepository jobRepository,@Qualifier("biTransactionManager") PlatformTransactionManager platformTransactionManager) {
        return new StepBuilder(String.join("", Constants.BI_SYNC), jobRepository)
                .tasklet(biSyncCusTasklet, platformTransactionManager)
                .build();
    }

    @Bean
    @StepScope
    public BISyncTasklet biSyncCusTasklet(
            @Value("#{jobExecutionContext['" + Constants.BI_STANDARD_SELECTION + "']}") JSONObject ssMap,
            SchemaSourceRepository schemaSourceRepository,
            SchemaSourceAuditRepository schemaSourceAuditRepository,
            MetaDataRepository metaDataRepository,
            ScheduledOpsRepository scheduledOpsRepository,
            @Qualifier("jdbcBI") JdbcTemplate jdbcTemplate) {
        BISyncTasklet tasklet = new BISyncTasklet(
                schemaSourceRepository,
                schemaSourceAuditRepository,
                metaDataRepository,
                scheduledOpsRepository,
                jdbcTemplate);
        tasklet.setSs(ssMap);
        tasklet.setT24TableName(CUSTOMER);
        tasklet.setColonneRef(null);
        return tasklet;
    }

    @Bean
    @JobScope
    public Step bISyncFt(BISyncTasklet biSyncFtTasklet, JobRepository jobRepository,@Qualifier("biTransactionManager")  PlatformTransactionManager platformTransactionManager) {
        return new StepBuilder(String.join("", Constants.BI_SYNC), jobRepository)
                .tasklet(biSyncFtTasklet, platformTransactionManager)
                .build();
    }

    @Bean
    @StepScope
    public BISyncTasklet biSyncFtTasklet(
            @Value("#{jobExecutionContext['" + Constants.BI_STANDARD_SELECTION + "']}") JSONObject ssMap,
            SchemaSourceRepository schemaSourceRepository,
            SchemaSourceAuditRepository schemaSourceAuditRepository,
            MetaDataRepository metaDataRepository,
            ScheduledOpsRepository scheduledOpsRepository,
            @Qualifier("jdbcBI") JdbcTemplate jdbcTemplate) {
        BISyncTasklet tasklet = new BISyncTasklet(
                schemaSourceRepository,
                schemaSourceAuditRepository,
                metaDataRepository,
                scheduledOpsRepository,
                jdbcTemplate);
        tasklet.setSs(ssMap);
        tasklet.setT24TableName(FUNDS_TRANSFER_HIS);
        tasklet.setColonneRef("DEBIT_VALUE_DATE");
        return tasklet;
    }

    @Bean
    @JobScope
    public Step bISyncAaBill(BISyncTasklet biSyncAaBillTasklet, JobRepository jobRepository,@Qualifier("biTransactionManager")  PlatformTransactionManager platformTransactionManager) {
        return new StepBuilder(String.join("", Constants.BI_SYNC), jobRepository)
                .tasklet(biSyncAaBillTasklet, platformTransactionManager)
                .build();
    }

    @Bean
    @StepScope
    public BISyncTasklet biSyncAaBillTasklet(
            @Value("#{jobExecutionContext['" + Constants.BI_STANDARD_SELECTION + "']}") JSONObject ssMap,
            SchemaSourceRepository schemaSourceRepository,
            SchemaSourceAuditRepository schemaSourceAuditRepository,
            MetaDataRepository metaDataRepository,
            ScheduledOpsRepository scheduledOpsRepository,
            @Qualifier("jdbcBI") JdbcTemplate jdbcTemplate) {
        BISyncTasklet tasklet = new BISyncTasklet(
                schemaSourceRepository,
                schemaSourceAuditRepository,
                metaDataRepository,
                scheduledOpsRepository,
                jdbcTemplate);
        tasklet.setSs(ssMap);
        tasklet.setT24TableName(AA_BILL_DETAILS);
        tasklet.setColonneRef("PAYMENT_DATE");
        return tasklet;
    }

    @Bean
    @StepScope
    public Json_Flattener normalizationCusTasklet(@Qualifier("jdbcBI") JdbcTemplate jdbcTemplate) {
        Json_Flattener tasklet = new Json_Flattener(jdbcTemplate);
        tasklet.setTable(CUSTOMER);
        return tasklet;
    }

    @Bean
    @JobScope
    public Step jsonNormalizationCusStep(JobRepository jobRepository,@Qualifier("biTransactionManager")  PlatformTransactionManager platformTransactionManager,
                                         Json_Flattener normalizationCusTasklet) {
        return new StepBuilder(String.join("", JSON_NORMALIZATION), jobRepository)
                .tasklet(normalizationCusTasklet, platformTransactionManager)
                .build();
    }


    @Bean
    @StepScope
    public Json_Flattener normalizationFtTasklet(@Qualifier("jdbcBI") JdbcTemplate jdbcTemplate) {
        Json_Flattener tasklet = new Json_Flattener(jdbcTemplate);
        tasklet.setTable("FUNDS_TRANSFER#HIS");
        return tasklet;
    }

    @Bean
    @JobScope
    public Step jsonNormalizationFtStep(JobRepository jobRepository,@Qualifier("biTransactionManager")  PlatformTransactionManager platformTransactionManager,
                                        Json_Flattener normalizationFtTasklet) {
        return new StepBuilder(String.join("", JSON_NORMALIZATION), jobRepository)
                .tasklet(normalizationFtTasklet, platformTransactionManager)
                .build();
    }


    @Bean
    @StepScope
    public Json_Flattener normalizationAaBillTasklet(@Qualifier("jdbcBI") JdbcTemplate jdbcTemplate) {
        Json_Flattener tasklet = new Json_Flattener(jdbcTemplate);
        tasklet.setTable("AA_BILL_DETAILS");
        return tasklet;
    }

    @Bean
    @JobScope
    public Step jsonNormalizationAaBillStep(JobRepository jobRepository,@Qualifier("biTransactionManager")  PlatformTransactionManager platformTransactionManager,
                                            Json_Flattener normalizationAaBillTasklet) {
        return new StepBuilder(String.join("", JSON_NORMALIZATION), jobRepository)
                .tasklet(normalizationAaBillTasklet, platformTransactionManager)
                .build();
    }



    @Bean
    public AsyncTaskExecutor taskExecutor() {
        return new TaskExecutorAdapter(
                Executors.newVirtualThreadPerTaskExecutor()
        );
    }


    /***************************************************************************
     * <p>
     *	Job Implementation
     *
     ***************************************************************************/

    @Bean
    @StepScope
    public ItemPrepStmtSetter itm() {
        return new ItemPrepStmtSetter();
    }

    @Bean
    @StepScope
    public JdbcBatchItemWriter<com.alibaba.fastjson2.JSONObject> itemWriter(@Value("#{jobExecutionContext['SQL_QUERY']}") String query) {
        JdbcBatchItemWriter<com.alibaba.fastjson2.JSONObject> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(biDataSource);
        writer.setSql(query);
        writer.setItemPreparedStatementSetter(itm());
        return writer;
    }

    @Bean
    public Flow customerFlow(Step cobStatus, Step tableNameCustomerStep, Step standardSelectionCustomerStep, Step cusQueryBuildingStep, Step bISyncCus, Step cusManager, Step jsonNormalizationCusStep) {
        return new FlowBuilder<SimpleFlow>(CUSTOMER)
                .start(cobStatus)
                .next(tableNameCustomerStep)
                .next(standardSelectionCustomerStep)
                .next(cusQueryBuildingStep)
                .next(bISyncCus)
                .next(cusManager)
                .next(jsonNormalizationCusStep)
                .build();
    }

    @Bean
    public Flow aaBillFlow(Step cobStatus, Step tableNameAABillStep, Step standardSelectionAABillStep, Step aaQueryBuildingStep, Step bISyncAaBill, Step aaManager, Step jsonNormalizationAaBillStep) {
        return new FlowBuilder<SimpleFlow>(AA_BILL_DETAILS)
                .start(cobStatus)
                .next(tableNameAABillStep)
                .next(standardSelectionAABillStep)
                .next(aaQueryBuildingStep)
                .next(bISyncAaBill)
                .next(aaManager)
                .next(jsonNormalizationAaBillStep)
                .build();
    }

    @Bean
    public Flow ftFlow(Step cobStatus, Step tableNameFTStep, Step standardSelectionFTStep, Step ftQueryBuildingStep, Step bISyncFt, Step ftManager, Step jsonNormalizationFtStep) {
        return new FlowBuilder<SimpleFlow>(FUNDS_TRANSFER_HIS)
                .start(cobStatus)
                .next(tableNameFTStep)
                .next(standardSelectionFTStep)
                .next(ftQueryBuildingStep)
                .next(bISyncFt)
                .next(ftManager)
                .next(jsonNormalizationFtStep)
                .build();
    }

    @Bean(name = "t24Job")
    public Job job(Flow ftFlow, Flow customerFlow, Flow aaBillFlow, JobRepository jobRepository)
    {
        return new JobBuilder("T24", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(customerFlow)
                .next(aaBillFlow)
                .next(ftFlow)
                .end()
                .build();
    }

    @Bean("loadSchema")
    public Job loadJob(Step tableNameCustomerStep, Step standardSelectionCustomerStep, Step cusQueryBuildingStep, Step bISyncCus, Step tableNameAABillStep, Step standardSelectionAABillStep, Step aaQueryBuildingStep, Step bISyncAaBill, Step tableNameFTStep, Step standardSelectionFTStep, Step ftQueryBuildingStep, Step bISyncFt, JobRepository jobRepository) {
        return new JobBuilder("SCHEMA_LOADER", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(tableNameFTStep)
                .next(standardSelectionFTStep)
                .next(ftQueryBuildingStep)
                .next(bISyncFt)
                .next(tableNameAABillStep)
                .next(standardSelectionAABillStep)
                .next(aaQueryBuildingStep)
                .next(bISyncAaBill)
                .next(tableNameCustomerStep)
                .next(standardSelectionCustomerStep)
                .next(cusQueryBuildingStep)
                .next(bISyncCus)
                .build();
    }

    @Bean(name = "customer")
    public Job customerJob(Flow customerFlow, JobRepository jobRepository) {
        return new JobBuilder("CUSTOMER.JOB", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(customerFlow)
                .end()
                .build();
    }

    @Bean(name = "billDetails")
    public Job billDetailsJob(Flow aaBillFlow, JobRepository jobRepository) {
        return new JobBuilder("AA.BILL.DETAILS.JOB", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(aaBillFlow)
                .end()
                .build();
    }

    @Bean(name = "ftHis")
    public Job ftJob(Flow ftFlow, JobRepository jobRepository) {
        return new JobBuilder("FT.HIS.JOB", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(ftFlow)
                .end()
                .build();
    }

    @Bean
    public ExecutionFlowDecider executionFlowDecider() {
        return new ExecutionFlowDecider();
    }
}
