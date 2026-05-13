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
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.jdbc.core.convert.DataAccessStrategy;
import org.springframework.data.jdbc.core.convert.DefaultDataAccessStrategy;
import org.springframework.data.jdbc.core.convert.SqlGeneratorSource;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.Optional;

@Configuration
@EnableJdbcRepositories(
        basePackages = "com.t24extractor.repository.bi_repos",
        jdbcAggregateOperationsRef = "biJdbcAggregateOperations",
        transactionManagerRef = "biTransactionManager"
)

public class BIDataSourceConfiguration {

    @Bean
    @ConfigurationProperties("spring.datasource.bi")
    public DataSourceProperties biDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Qualifier("bi")
    public DataSource biDataSource() {
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
    public JdbcTemplate jdbcSqlServer(@Qualifier("bi") DataSource ds) {
        return new JdbcTemplate(ds);
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

    @Bean(name = "biJdbcOperations")
    NamedParameterJdbcOperations biJdbcOperations(@Qualifier("bi") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean(name = "biTransactionManager")
    public PlatformTransactionManager biTransactionManager(@Qualifier("bi") DataSource ds) {
        return new DataSourceTransactionManager(ds);
    }

    @Bean(name = "biDialect")
    public Dialect biDialect() {
        return JdbcSqlServerDialect.INSTANCE;
    }

    @Bean(name = "biJdbcCustomConversions")
    public JdbcCustomConversions biJdbcCustomConversions() {
        return new JdbcCustomConversions();
    }

    @Bean(name = "biMappingContext")
    public RelationalMappingContext biMappingContext() {
        return new org.springframework.data.jdbc.core.mapping.JdbcMappingContext(DefaultNamingStrategy.INSTANCE);
    }

    @Bean(name = "biJdbcConverter")
    public JdbcConverter biJdbcConverter(
            @Qualifier("biMappingContext") RelationalMappingContext context,
            @Qualifier("biJdbcOperations") NamedParameterJdbcOperations ops,
            @Lazy @Qualifier("biDataAccessStrategy") RelationResolver relationResolver,
            @Qualifier("biJdbcCustomConversions") JdbcCustomConversions conversions
    ) {
        return new MappingJdbcConverter(context, relationResolver, conversions, new DefaultJdbcTypeFactory(ops.getJdbcOperations()));
    }

    @Bean(name = "biDataAccessStrategy")
    public DataAccessStrategy biDataAccessStrategy(
            @Qualifier("biMappingContext") RelationalMappingContext context,
            @Qualifier("biJdbcConverter") JdbcConverter converter,
            @Qualifier("biJdbcOperations") NamedParameterJdbcOperations ops,
            @Qualifier("biDialect") Dialect dialect
    ) {
        var sqlGeneratorSource = new SqlGeneratorSource(context, converter, dialect);
        return new DefaultDataAccessStrategy(
                sqlGeneratorSource,
                context,
                converter,
                ops,
                new org.springframework.data.jdbc.core.convert.SqlParametersFactory(context, converter),
                new org.springframework.data.jdbc.core.convert.InsertStrategyFactory(ops, dialect),
                org.springframework.data.jdbc.core.convert.QueryMappingConfiguration.EMPTY
        );
    }

    @Bean(name = "biJdbcAggregateOperations")
    public JdbcAggregateOperations biJdbcAggregateOperations(
            ApplicationContext applicationContext,
            @Qualifier("biMappingContext") RelationalMappingContext context,
            @Qualifier("biJdbcConverter") JdbcConverter converter,
            @Qualifier("biDataAccessStrategy") DataAccessStrategy dataAccessStrategy
    ) {
        return new JdbcAggregateTemplate(applicationContext, context, converter, dataAccessStrategy);
    }

//    @Bean
//    @Primary
//    Dialect mssqlJdbcDialect() {
//        return JdbcSqlServerDialect.INSTANCE;
//    }
//
//
//    @Bean
//    @Primary
//    JdbcCustomConversions customConversions() {
//        return new JdbcCustomConversions();
//    }
//
//    @Bean
//    @Primary
//    JdbcMappingContext jdbcMappingContext(Optional<NamingStrategy> namingStrategy, JdbcCustomConversions customConversions) {
//        JdbcMappingContext mappingContext = new JdbcMappingContext(namingStrategy.orElse(DefaultNamingStrategy.INSTANCE));
//        mappingContext.setSimpleTypeHolder(customConversions.getSimpleTypeHolder());
//        return mappingContext;
//    }
//
//    @Bean
//    @Primary
//    JdbcConverter jdbcConverter(JdbcMappingContext mappingContext,
//                                @Qualifier("biJdbcOperations") NamedParameterJdbcOperations jdbcOperationsDataBase1,
//                                @Lazy RelationResolver relationResolver,
//                                JdbcCustomConversions conversions) {
//
//        DefaultJdbcTypeFactory jdbcTypeFactory = new DefaultJdbcTypeFactory(jdbcOperationsDataBase1.getJdbcOperations());
//        return new MappingJdbcConverter(mappingContext, relationResolver, conversions, jdbcTypeFactory);
//    }

//    @Bean
//    public IntegrationDataSourceScriptDatabaseInitializer customIntegrationDataSourceInitializer(@Qualifier("bi") DataSource dataSource) {
//        return new IntegrationDataSourceScriptDatabaseInitializer(dataSource, new DatabaseInitializationSettings());
//    }
}
