package com.t24extractor.configuration;

import org.springframework.batch.core.configuration.support.JdbcDefaultBatchConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class BatchInfrastructureConfiguration extends JdbcDefaultBatchConfiguration {

    private final DataSource batchDataSource;
    private final PlatformTransactionManager batchTransactionManager;

    public BatchInfrastructureConfiguration(
            @Qualifier("bi") DataSource batchDataSource,
            @Qualifier("biTransactionManager") PlatformTransactionManager batchTransactionManager
    ) {
        this.batchDataSource = batchDataSource;
        this.batchTransactionManager = batchTransactionManager;
    }

    @Override
    protected DataSource getDataSource() {
        return batchDataSource;
    }

    @Override
    protected PlatformTransactionManager getTransactionManager() {
        return batchTransactionManager;
    }
}
