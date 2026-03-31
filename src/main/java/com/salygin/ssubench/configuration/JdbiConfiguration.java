package com.salygin.ssubench.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.salygin.ssubench.repository.BidRepository;
import com.salygin.ssubench.repository.PaymentRepository;
import com.salygin.ssubench.repository.TaskRepository;
import com.salygin.ssubench.repository.UserRepository;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.jdbi.v3.jackson2.Jackson2Config;
import org.jdbi.v3.jackson2.Jackson2Plugin;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;

@Configuration
public class JdbiConfiguration {

    @Bean
    public ObjectMapper jsonMapper() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return om;
    }

    @Bean
    public Jdbi jdbi(DataSource appDataSource, ObjectMapper jsonMapper) {
        TransactionAwareDataSourceProxy wrapped = new TransactionAwareDataSourceProxy(appDataSource);

        Jdbi jdbiInstance = Jdbi.create(wrapped);

        jdbiInstance.installPlugin(new PostgresPlugin());
        jdbiInstance.installPlugin(new SqlObjectPlugin());
        jdbiInstance.installPlugin(new Jackson2Plugin());

        jdbiInstance.installPlugin(new JdbiPlugin() {
            @Override
            public void customizeJdbi(Jdbi handle) {
                handle.getConfig(Jackson2Config.class)
                        .setMapper(jsonMapper.copy());
            }
        });

        return jdbiInstance;
    }

    @Bean
    public UserRepository userRepository(Jdbi jdbi) {
        return jdbi.onDemand(UserRepository.class);
    }

    @Bean
    public TaskRepository taskRepository(Jdbi jdbi) {
        return jdbi.onDemand(TaskRepository.class);
    }

    @Bean
    public BidRepository bidRepository(Jdbi jdbi) {
        return jdbi.onDemand(BidRepository.class);
    }

    @Bean
    public PaymentRepository paymentRepository(Jdbi jdbi) {
        return jdbi.onDemand(PaymentRepository.class);
    }
}

