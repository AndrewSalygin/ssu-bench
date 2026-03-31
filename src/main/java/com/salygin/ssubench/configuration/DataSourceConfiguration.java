package com.salygin.ssubench.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "db.ssu-bench")
    public DataSourceProperties dbProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource appDataSource() {
        DataSourceProperties props = dbProperties();
        return props.initializeDataSourceBuilder()
                .build();
    }

    @Bean
    public PlatformTransactionManager txManager() {
        return new DataSourceTransactionManager(appDataSource());
    }
}

