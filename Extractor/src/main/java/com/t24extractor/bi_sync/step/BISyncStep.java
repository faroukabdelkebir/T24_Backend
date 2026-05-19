package com.t24extractor.bi_sync.step;

import com.t24extractor.bi_sync.tasklet.BISyncTasklet;
import com.t24extractor.repository.bi_repos.MetaDataRepository;
import com.t24extractor.repository.bi_repos.ScheduledOpsRepository;
import com.t24extractor.repository.bi_repos.SchemaSourceAuditRepository;
import com.t24extractor.repository.bi_repos.SchemaSourceRepository;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;


import static com.t24extractor.commons.Constants.*;

@Slf4j
@Configuration
public class BISyncStep {


    @Bean
    @StepScope
    public BISyncTasklet biSyncTasklet(
            @Value("#{stepExecutionContext['" + BI_STANDARD_SELECTION + "']}") JSONObject ssMap,
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
        tasklet.setT24TableName("CUSTOMER");
        tasklet.setColonneRef("DATE_TIME");
        return tasklet;
    }

    /**
     * Déclaration du step qui contient le tasklet BISyncTasklet.
     *
     * @return step BI_SYNC
     */


    @Bean
    public Step bISync(BISyncTasklet biSyncTasklet, JobRepository jobRepository,@Qualifier("biTransactionManager") PlatformTransactionManager platformTransactionManager) {
        return new StepBuilder(String.join("", BI_SYNC), jobRepository)
                .tasklet(biSyncTasklet, platformTransactionManager)
                .build();
    }

}
