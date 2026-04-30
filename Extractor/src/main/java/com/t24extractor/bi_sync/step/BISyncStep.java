package com.t24extractor.bi_sync.step;

import com.t24extractor.bi_sync.tasklet.BISyncTasklet;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

import static com.t24extractor.commons.Constants.*;

@Slf4j
@Configuration
public class BISyncStep {


    @Bean
    @StepScope
    public BISyncTasklet biSyncTasklet(@Value("#{stepExecutionContext['" + BI_STANDARD_SELECTION + "']}") JSONObject ssMap) {
        BISyncTasklet tasklet = new BISyncTasklet();
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

//    @Bean
//    @JobScope
//    public step BISync(@Value("#{jobParameters['"+T24_SELECTION_NAME_PARAMETER+"']}") String t24TableName,BISyncTasklet biSyncTasklet, JobRepository jobRepository, PlatformTransactionManager platformTransactionManager) {
//        return new StepBuilder(String.join("-",t24TableName, BI_SYNC), jobRepository)
//                .tasklet(biSyncTasklet, platformTransactionManager)
//                .build();
//    }
    @Bean
    public Step BISync(BISyncTasklet biSyncTasklet, JobRepository jobRepository, PlatformTransactionManager platformTransactionManager) {
        return new StepBuilder(String.join("", BI_SYNC), jobRepository)
                .tasklet(biSyncTasklet, platformTransactionManager)
                .build();
    }

}
