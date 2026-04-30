package com.t24extractor.configuration;

import com.t24extractor.PreparedStatementSetter.ItemPrepStmtSetter;
import com.t24extractor.bi_sync.tasklet.BISyncTasklet;
import com.t24extractor.commons.Constants;
import com.t24extractor.decider.ExecutionFlowDecider;
import com.t24extractor.listeners.JobListener;
import com.t24extractor.model.T24Table;
import com.t24extractor.partitioner.MssqlPartitioner;
import com.t24extractor.processor.BlobFilterProcessor;
import com.t24extractor.processor.XMLFilterProcessor;
import com.t24extractor.processor.XMLProcessor;
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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executors;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;



@Configuration
@ComponentScan(basePackages = "com.t24extractor")
@Import({BIDataSourceConfiguration.class, T24DataSourceConfiguration.class})
public class BatchConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(BatchConfiguration.class);

    private static final int CHUNKS_NUMBER = 700;
    private static final int FETCH_SIZE = 1000;

    @Value("${workers.number}")
    private int workersNumber;

    @Autowired
    @Qualifier("T24")
    private DataSource dataSource;

    @Autowired
    @Qualifier("bi")
    private DataSource biDataSource;

    private final CobStatusTasklet cobStatusTasklet;

    @Autowired
    public BatchConfiguration(CobStatusTasklet cobStatusTasklet) {
        this.cobStatusTasklet = cobStatusTasklet;
    }

    /***************************************************************************
     * <p>
     *	Listeners
     *
     ***************************************************************************/

    @Bean
    public JobExecutionListener jobListener() {
        return new JobListener();
    }


    /***************************************************************************
     * <p>
     *	Tasklets
     *
     ***************************************************************************/

    @Bean
    public StandardSelectionTasklet
    standardSelectionCustomer(@Qualifier("jdbcTemplateT24") JdbcTemplate jdbcExecutor) {
        StandardSelectionTasklet myTasklet = new StandardSelectionTasklet();

        myTasklet.setJdbcExecutor(jdbcExecutor);
        myTasklet.setT24TableName("CUSTOMER");
        myTasklet.setTableType("X");
        myTasklet.setOracleTableName("FBNK_CUSTOMER");

        return myTasklet;
    }

    @Bean
    public StandardSelectionTasklet
    standardSelectionFT(@Qualifier("jdbcTemplateT24") JdbcTemplate jdbcExecutor) {
        StandardSelectionTasklet myTasklet = new StandardSelectionTasklet();

        myTasklet.setJdbcExecutor(jdbcExecutor);
        myTasklet.setT24TableName("FUNDS.TRANSFER$HIS");
        myTasklet.setTableType("X");
        myTasklet.setOracleTableName("FBNK_FUNDS_TRANSFER001");

        return myTasklet;
    }

    @Bean
    public StandardSelectionTasklet
    standardSelectionAABill(@Qualifier("jdbcTemplateT24") JdbcTemplate jdbcExecutor) {
        StandardSelectionTasklet myTasklet = new StandardSelectionTasklet();

        myTasklet.setJdbcExecutor(jdbcExecutor);
        myTasklet.setT24TableName("AA.BILL.DETAILS");
        myTasklet.setTableType("X");
        myTasklet.setOracleTableName("FBNK_AA_BILL_DETAILS");

        return myTasklet;
    }

    @Bean
    public TableNameTasklet tableNameCustomer() {
        TableNameTasklet tableNameTasklet = new TableNameTasklet();
        tableNameTasklet.setT24TableName("CUSTOMER");
        return tableNameTasklet;
    }

    @Bean
    public TableNameTasklet tableNameFT() {
        TableNameTasklet tableNameTasklet = new TableNameTasklet();
        tableNameTasklet.setT24TableName("FUNDS.TRANSFER$HIS");
        return tableNameTasklet;
    }

    @Bean
    public TableNameTasklet tableNameAABill() {
        TableNameTasklet tableNameTasklet = new TableNameTasklet();
        tableNameTasklet.setT24TableName("AA.BILL.DETAILS");
        return tableNameTasklet;
    }

    @Bean
    public QueryTasklet queryCustomerTasklet() {
        QueryTasklet queryCusTasklet = new QueryTasklet();
        queryCusTasklet.setColumnRef(null);
        queryCusTasklet.setDateFormat(null);
        queryCusTasklet.setTableName("FBNK_CUSTOMER");
        queryCusTasklet.setChargement("Full");
        queryCusTasklet.setLastDate(null);
        return queryCusTasklet;
    }

    @Bean
    public QueryTasklet queryFtTasklet() {
        QueryTasklet queryCusTasklet = new QueryTasklet();
        queryCusTasklet.setColumnRef("DEBIT_VALUE_DATE");
        queryCusTasklet.setDateFormat("VARCHAR");
        queryCusTasklet.setTableName("FBNK_FUNDS_TRANSFER001");
        queryCusTasklet.setChargement("Incremental");
        queryCusTasklet.setLastDate(dateString());
        return queryCusTasklet;
    }

    private String dateString() {
//        return "20230401";
        return "00000000";
//        return YearMonth.now().minusMonths(3).atDay(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    @Bean
    public QueryTasklet queryAABillTasklet() {
        QueryTasklet queryCusTasklet = new QueryTasklet();
        queryCusTasklet.setColumnRef("PAYMENT_DATE");
        queryCusTasklet.setDateFormat("VARCHAR");
        queryCusTasklet.setTableName("FBNK_AA_BILL_DETAILS");
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
                    @Value("#{stepExecutionContext['value']}") int value) throws Exception {


        LOG.info("[Reader Query] : \n" + query);

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
        SynchronizedItemStreamReader<T24Table> synchronizedReader = new SynchronizedItemStreamReader<>(reader);

        return synchronizedReader;
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
                     @Value("#{stepExecutionContext['maxValue']}") Long maxValue) throws Exception {
        String readerQuery = "SELECT * FROM ( " + query + " ) " +
                "WHERE rowNumber >= " + minValue + " " +
                "AND rowNumber <= " + maxValue;

        LOG.trace("[minValue - maxValue] : " + minValue + " -> " + maxValue);
        LOG.trace("[Reader Query] : \n" + readerQuery);

        JdbcCursorItemReader<HashMap<String, Object>> reader = new JdbcCursorItemReader<>(dataSource, readerQuery, blobRowMapper());
        reader.setFetchSize(FETCH_SIZE);
        reader.setConnectionAutoCommit(false);
        SynchronizedItemStreamReader<HashMap<String, Object>> synchronizedReader = new SynchronizedItemStreamReader<>(reader);
        return synchronizedReader;
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
    public Step standardSelectionCustomerStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("Standard.Selection.CUSTOMER", jobRepository)
                .tasklet(standardSelectionCustomer(null), transactionManager)
                .build();
    }

    @Bean
    public Step standardSelectionFTStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("Standard.Selection.FUNDS.TRANSFER", jobRepository)
                .tasklet(standardSelectionFT(null), transactionManager)
                .build();
    }

    @Bean
    public Step standardSelectionAABillStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("Standard.Selection.AA.BILL.DETAILS", jobRepository)
                .tasklet(standardSelectionAABill(null), transactionManager)
                .build();
    }

    @Bean
    public Step TableNameCustomerStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("TableName.CUSTOMER", jobRepository)
                .tasklet(tableNameCustomer(), transactionManager)
                .build();
    }

    @Bean
    public Step TableNameFTStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("TableName.FT$HIS", jobRepository)
                .tasklet(tableNameFT(), transactionManager)
                .build();
    }

    @Bean
    public Step TableNameAABillStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("TableName.AA.BILL.DETAILS", jobRepository)
                .tasklet(tableNameAABill(), transactionManager)
                .build();
    }

    @Bean
    @JobScope
    public Step cusQueryBuildingStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("QueryBuildingStep", jobRepository)
                .tasklet(queryCustomerTasklet(), transactionManager)
                .build();
    }

    @Bean
    @JobScope
    public Step ftQueryBuildingStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("QueryBuildingStep", jobRepository)
                .tasklet(queryFtTasklet(), transactionManager)
                .build();
    }

    @Bean
    @JobScope
    public Step aaQueryBuildingStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("QueryBuildingStep", jobRepository)
                .tasklet(queryAABillTasklet(), transactionManager)
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
            throws Exception {
        return new StepBuilder("CUSTOMER", jobRepository)
                .partitioner("cusWorker", partitioner())
                .partitionHandler(cusXMLPartitionHandler)
                //.listener(managerStepListener(null, null, null, null))
                .build();
    }

    @Bean
    @JobScope
    public Step ftManager(PartitionHandler ftXMLPartitionHandler, JobRepository jobRepository) {
        return new StepBuilder("FUNDS.TRANSFER$HIS", jobRepository)
                .partitioner("ftWorker", partitioner())
                .partitionHandler(ftXMLPartitionHandler)
                .build();
    }

    @Bean
    @JobScope
    public Step aaManager(PartitionHandler aaXMLPartitionHandler, JobRepository jobRepository)
    {
        return new StepBuilder("AA.BILL.DETAILS", jobRepository)
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
    public Step cusWorker(JdbcBatchItemWriter<com.alibaba.fastjson2.JSONObject> ItemWriter, JobRepository jobRepository, PlatformTransactionManager transactionManager) throws Exception {
        return new StepBuilder("cusWorker", jobRepository)
                .<T24Table, com.alibaba.fastjson2.JSONObject>chunk(CHUNKS_NUMBER)
                .transactionManager(transactionManager)
                .reader(oracleXmlReader(null, 0))
                .processor(cusXmlProcessor())
                .writer(ItemWriter)
                .build();
    }

    @Bean
    public Step ftWorker(JdbcBatchItemWriter<com.alibaba.fastjson2.JSONObject> ItemWriter, JobRepository jobRepository, PlatformTransactionManager transactionManager) throws Exception {
        return new StepBuilder("ftWorker", jobRepository)
                .<T24Table, com.alibaba.fastjson2.JSONObject>chunk(CHUNKS_NUMBER)
                .transactionManager(transactionManager)
                .reader(oracleXmlReader(null, 0))
                .processor(ftXmlProcessor())
                .writer(ItemWriter)
                .build();
    }

    @Bean
    public Step aaWorker(JdbcBatchItemWriter<com.alibaba.fastjson2.JSONObject> ItemWriter, JobRepository jobRepository, PlatformTransactionManager transactionManager) throws Exception {
        return new StepBuilder("aaWorker", jobRepository)
                .<T24Table, com.alibaba.fastjson2.JSONObject>chunk(CHUNKS_NUMBER)
                .transactionManager(transactionManager)
                .reader(oracleXmlReader(null, 0))
                .processor(aaBillXmlProcessor())
                .writer(ItemWriter)
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
        processor.setOracleTableName("FBNK_CUSTOMER");
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
        processor.setOracleTableName("FBNK_AA_BILL_DETAILS");
        return processor;
    }


    @Bean
    @StepScope
    public BlobFilterProcessor BlobFilterProcessor() {
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

        try {
            handler.afterPropertiesSet();
        } catch (Exception e) {
//      e.printStackTrace();
        }

        return handler;
    }


    @Bean
    public PartitionHandler ftXMLPartitionHandler(Step ftWorker) throws Exception {
        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();

        handler.setTaskExecutor(taskExecutor());
        handler.setGridSize(workersNumber);
        handler.setStep(ftWorker);

        try {
            handler.afterPropertiesSet();
        } catch (Exception e) {
//      e.printStackTrace();
        }

        return handler;
    }

    @Bean
    public PartitionHandler aaXMLPartitionHandler(Step aaWorker) throws Exception {
        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();

        handler.setTaskExecutor(taskExecutor());
        handler.setGridSize(workersNumber);
        handler.setStep(aaWorker);

        try {
            handler.afterPropertiesSet();
        } catch (Exception e) {
//      e.printStackTrace();
        }

        return handler;
    }

    @Bean
    public Step cobStatus(JobRepository jobRepository, PlatformTransactionManager platformTransactionManager) {
        return new StepBuilder(String.join("", Constants.STEP_COB_STATUS), jobRepository)
                .tasklet(cobStatusTasklet, platformTransactionManager)
                .build();
    }

    @Bean
    @JobScope
    public Step BISyncCus(BISyncTasklet biSyncCusTasklet, JobRepository jobRepository, PlatformTransactionManager platformTransactionManager) {
        return new StepBuilder(String.join("", Constants.BI_SYNC), jobRepository)
                .tasklet(biSyncCusTasklet, platformTransactionManager)
                .build();
    }

    @Bean
    @StepScope
    public BISyncTasklet biSyncCusTasklet(@Value("#{jobExecutionContext['" + Constants.BI_STANDARD_SELECTION + "']}") JSONObject ssMap) {
        BISyncTasklet tasklet = new BISyncTasklet();
        tasklet.setSs(ssMap);
        tasklet.setT24TableName("CUSTOMER");
        tasklet.setColonneRef(null);
        return tasklet;
    }

    @Bean
    @JobScope
    public Step BISyncFt(BISyncTasklet biSyncFtTasklet, JobRepository jobRepository, PlatformTransactionManager platformTransactionManager) {
        return new StepBuilder(String.join("", Constants.BI_SYNC), jobRepository)
                .tasklet(biSyncFtTasklet, platformTransactionManager)
                .build();
    }

    @Bean
    @StepScope
    public BISyncTasklet biSyncFtTasklet(@Value("#{jobExecutionContext['" + Constants.BI_STANDARD_SELECTION + "']}") JSONObject ssMap) {
        BISyncTasklet tasklet = new BISyncTasklet();
        tasklet.setSs(ssMap);
        tasklet.setT24TableName("FUNDS.TRANSFER$HIS");
        tasklet.setColonneRef("DEBIT_VALUE_DATE");
        return tasklet;
    }

    @Bean
    @JobScope
    public Step BISyncAaBill(BISyncTasklet biSyncAaBillTasklet, JobRepository jobRepository, PlatformTransactionManager platformTransactionManager) {
        return new StepBuilder(String.join("", Constants.BI_SYNC), jobRepository)
                .tasklet(biSyncAaBillTasklet, platformTransactionManager)
                .build();
    }

    @Bean
    @StepScope
    public BISyncTasklet biSyncAaBillTasklet(@Value("#{jobExecutionContext['" + Constants.BI_STANDARD_SELECTION + "']}") JSONObject ssMap) {
        BISyncTasklet tasklet = new BISyncTasklet();
        tasklet.setSs(ssMap);
        tasklet.setT24TableName("AA.BILL.DETAILS");
        tasklet.setColonneRef("PAYMENT_DATE");
        return tasklet;
    }

    @Bean
    @StepScope
    public Json_Flattener normalizationCusTasklet() {
        Json_Flattener tasklet = new Json_Flattener();
        tasklet.setTable("CUSTOMER");
        return tasklet;
    }

    @Bean
    @JobScope
    public Step jsonNormalizationCusStep(JobRepository jobRepository, PlatformTransactionManager platformTransactionManager) {
        return new StepBuilder(String.join("", "JSON_NORMALIZATION"), jobRepository)
                .tasklet(normalizationCusTasklet(), platformTransactionManager)
                .build();
    }


    @Bean
    @StepScope
    public Json_Flattener normalizationFtTasklet() {
        Json_Flattener tasklet = new Json_Flattener();
        tasklet.setTable("FUNDS_TRANSFER#HIS");
        return tasklet;
    }

    @Bean
    @JobScope
    public Step jsonNormalizationFtStep(JobRepository jobRepository, PlatformTransactionManager platformTransactionManager) {
        return new StepBuilder(String.join("", "JSON_NORMALIZATION"), jobRepository)
                .tasklet(normalizationFtTasklet(), platformTransactionManager)
                .build();
    }


    @Bean
    @StepScope
    public Json_Flattener normalizationAaBillTasklet() {
        Json_Flattener tasklet = new Json_Flattener();
        tasklet.setTable("AA_BILL_DETAILS");
        return tasklet;
    }

    @Bean
    @JobScope
    public Step jsonNormalizationAaBillStep(JobRepository jobRepository, PlatformTransactionManager platformTransactionManager) {
        return new StepBuilder(String.join("", "JSON_NORMALIZATION"), jobRepository)
                .tasklet(normalizationAaBillTasklet(), platformTransactionManager)
                .build();
    }


//    @Bean
//    public ThreadPoolTaskExecutor taskExecutor() {
//        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//        executor.setCorePoolSize(workersNumber);
//        executor.setMaxPoolSize(workersNumber + 1);
//
//        return executor;
//    }

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
    public JdbcBatchItemWriter<com.alibaba.fastjson2.JSONObject> ItemWriter(@Value("#{jobExecutionContext['SQL_QUERY']}") String query) {
        JdbcBatchItemWriter<com.alibaba.fastjson2.JSONObject> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(biDataSource);
        writer.setSql(query);
        writer.setItemPreparedStatementSetter(itm());
        return writer;
    }

    @Bean
    public Flow customerFlow(Step cobStatus, Step TableNameCustomerStep, Step standardSelectionCustomerStep, Step cusQueryBuildingStep, Step BISyncCus, Step cusManager, Step jsonNormalizationCusStep) {
        return new FlowBuilder<SimpleFlow>("CUSTOMER")
                .start(cobStatus)
                .next(TableNameCustomerStep)
                .next(standardSelectionCustomerStep)
                .next(cusQueryBuildingStep)
                .next(BISyncCus)
                .next(cusManager)
                .next(jsonNormalizationCusStep)
                .build();
    }

    @Bean
    public Flow aaBillFlow(Step cobStatus, Step TableNameAABillStep, Step standardSelectionAABillStep, Step aaQueryBuildingStep, Step BISyncAaBill, Step aaManager, Step jsonNormalizationAaBillStep) {
        return new FlowBuilder<SimpleFlow>("AA.BILL.DETAILS")
                .start(cobStatus)
                .next(TableNameAABillStep)
                .next(standardSelectionAABillStep)
                .next(aaQueryBuildingStep)
                .next(BISyncAaBill)
                .next(aaManager)
                .next(jsonNormalizationAaBillStep)
                .build();
    }

    @Bean
    public Flow ftFlow(Step cobStatus, Step TableNameFTStep, Step standardSelectionFTStep, Step ftQueryBuildingStep, Step BISyncFt, Step ftManager, Step jsonNormalizationFtStep) {
        return new FlowBuilder<SimpleFlow>("FUNDS.TRANSFER$HIS")
                .start(cobStatus)
                .next(TableNameFTStep)
                .next(standardSelectionFTStep)
                .next(ftQueryBuildingStep)
                .next(BISyncFt)
                .next(ftManager)
                .next(jsonNormalizationFtStep)
                .build();
    }

    @Bean
    @Qualifier("t24")
    public Job job(Flow ftFlow, Flow customerFlow, Flow aaBillFlow, JobRepository jobRepository)
            throws Exception {
        return new JobBuilder("T24", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(customerFlow)
                .next(aaBillFlow)
                .next(ftFlow)
                .end()
                .build();
    }

    @Bean
    @Qualifier("loadSchema")
    public Job loadJob(Step TableNameCustomerStep, Step standardSelectionCustomerStep, Step cusQueryBuildingStep, Step BISyncCus, Step TableNameAABillStep, Step standardSelectionAABillStep, Step aaQueryBuildingStep, Step BISyncAaBill, Step TableNameFTStep, Step standardSelectionFTStep, Step ftQueryBuildingStep, Step BISyncFt, JobRepository jobRepository) {
        return new JobBuilder("SCHEMA_LOADER", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(TableNameFTStep)
                .next(standardSelectionFTStep)
                .next(ftQueryBuildingStep)
                .next(BISyncFt)
                .next(TableNameAABillStep)
                .next(standardSelectionAABillStep)
                .next(aaQueryBuildingStep)
                .next(BISyncAaBill)
                .next(TableNameCustomerStep)
                .next(standardSelectionCustomerStep)
                .next(cusQueryBuildingStep)
                .next(BISyncCus)
                .build();
    }

    @Bean
    @Qualifier("customer")
    public Job customerJob(Flow customerFlow, JobRepository jobRepository) {
        return new JobBuilder("CUSTOMER.JOB", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(customerFlow)
                .end()
                .build();
    }

    @Bean
    @Qualifier("bill.details")
    public Job billDetailsJob(Flow aaBillFlow, JobRepository jobRepository) {
        return new JobBuilder("AA.BILL.DETAILS.JOB", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(aaBillFlow)
                .end()
                .build();
    }

    @Bean
    @Qualifier("ft.his")
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
