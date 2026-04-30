package com.t24extractor.configuration;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jdbc.core.dialect.JdbcSqlServerDialect;
import org.springframework.data.jdbc.core.convert.DefaultJdbcTypeFactory;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.core.convert.MappingJdbcConverter;
import org.springframework.data.jdbc.core.convert.RelationResolver;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.mapping.DefaultNamingStrategy;
import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.Optional;

@Configuration
@EnableJdbcRepositories(
        basePackages = "com.t24extractor.repository.bi_repos",
        jdbcOperationsRef = "biJdbcOperations")

public class BIDataSourceConfiguration {

    @Bean
    @ConfigurationProperties("spring.datasource.bi")
    public DataSourceProperties biDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Qualifier("bi")
    @Primary
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(Objects.requireNonNull(biDataSourceProperties().getUrl()));
        ds.setUsername(Objects.requireNonNull(biDataSourceProperties().getUsername()));
        ds.setPassword(Objects.requireNonNull(biDataSourceProperties().getPassword()));
        ds.setDriverClassName(Objects.requireNonNull(biDataSourceProperties().getDriverClassName()));
        ds.setAutoCommit(true);
        ds.setMaximumPoolSize(20);
        return ds;
    }


    @Bean("JdbcBI")
    @Qualifier("JdbcBI")
    @Primary
    public JdbcTemplate jdbcSqlServer() {
        return new JdbcTemplate(dataSource());
    }

//    @Bean
//    @Primary
//    public DataSourceInitializer dataSourceInitializer2() {
//        ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator();
//        resourceDatabasePopulator.addScripts(new ClassPathResource("/schemas/mssql-schema.sql"));
//        resourceDatabasePopulator.setSeparator("GO");
//        DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
//        dataSourceInitializer.setDataSource(dataSource());
//        dataSourceInitializer.setDatabasePopulator(resourceDatabasePopulator);
//        return dataSourceInitializer;
//    }

    @Bean
    @Qualifier("biJdbcOperations")
    @Primary
    NamedParameterJdbcOperations biJdbcOperations(@Qualifier("bi") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    @Primary
    Dialect mssqlJdbcDialect() {
        return JdbcSqlServerDialect.INSTANCE;
    }


    @Bean
    @Primary
    JdbcCustomConversions customConversions() {
        return new JdbcCustomConversions();
    }

    @Bean
    @Primary
    JdbcMappingContext jdbcMappingContext(Optional<NamingStrategy> namingStrategy, JdbcCustomConversions customConversions) {
        JdbcMappingContext mappingContext = new JdbcMappingContext(namingStrategy.orElse(DefaultNamingStrategy.INSTANCE));
        mappingContext.setSimpleTypeHolder(customConversions.getSimpleTypeHolder());
        return mappingContext;
    }

    @Bean
    @Primary
    JdbcConverter jdbcConverter(JdbcMappingContext mappingContext,
                                @Qualifier("biJdbcOperations") NamedParameterJdbcOperations jdbcOperationsDataBase1,
                                @Lazy RelationResolver relationResolver,
                                JdbcCustomConversions conversions) {

        DefaultJdbcTypeFactory jdbcTypeFactory = new DefaultJdbcTypeFactory(jdbcOperationsDataBase1.getJdbcOperations());
        return new MappingJdbcConverter(mappingContext, relationResolver, conversions, jdbcTypeFactory);
    }

//    @Bean
//    public IntegrationDataSourceScriptDatabaseInitializer customIntegrationDataSourceInitializer(@Qualifier("bi") DataSource dataSource) {
//        return new IntegrationDataSourceScriptDatabaseInitializer(dataSource, new DatabaseInitializationSettings());
//    }
}
