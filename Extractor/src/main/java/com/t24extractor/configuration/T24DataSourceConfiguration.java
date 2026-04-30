package com.t24extractor.configuration;


import com.t24extractor.model.CobStatus;
import com.t24extractor.model.FileControl;
import com.t24extractor.model.StandardSelectionRecord;
import com.t24extractor.model.TafjVoc;
import com.t24extractor.repository.t24_repo.CobRepository;
import com.t24extractor.repository.t24_repo.FileControlRepository;
import com.t24extractor.repository.t24_repo.StandardSelectionRepository;
import com.t24extractor.repository.t24_repo.TafjVocRepository;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jdbc.JdbcRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

@Configuration
@EnableJdbcRepositories(
        basePackageClasses = {StandardSelectionRecord.class, StandardSelectionRepository.class, FileControl.class, FileControlRepository.class, TafjVoc.class, TafjVocRepository.class, CobRepository.class, CobStatus.class},
        basePackages = "com.t24extractor.repository.t24_repo",
        jdbcOperationsRef = "t24JdbcOperations"
)
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        JdbcRepositoriesAutoConfiguration.class
})
public class T24DataSourceConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.t24")
    public DataSourceProperties t24DataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Qualifier("T24")
    public DataSource getDataSource() {
        HikariDataSource pds = new HikariDataSource();

        pds.setJdbcUrl(t24DataSourceProperties().getUrl());
        pds.setDriverClassName(t24DataSourceProperties().getDriverClassName());
        pds.setUsername(t24DataSourceProperties().getUsername());
        pds.setPassword(t24DataSourceProperties().getPassword());
        pds.setMaximumPoolSize(20);
        return pds;
    }

    @Bean
    @Qualifier("jdbcTemplateT24")
    public JdbcTemplate jdbcTemplateLogic(@Qualifier("T24") DataSource ds) {
        return new JdbcTemplate(ds);
    }


    @Bean
    @Qualifier("t24JdbcOperations")
    NamedParameterJdbcOperations t24JdbcOperations(@Qualifier("T24") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }
}
