package com.salygin.ssubench.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class ControllerExecutorConfiguration {

    @Bean("controllerExecutor")
    public Executor controllerExecutorBean() {
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(6);
        pool.setMaxPoolSize(12);
        pool.setQueueCapacity(150);
        pool.setThreadNamePrefix("web-ctrl-");
        pool.initialize();

        return new DelegatingSecurityContextAsyncTaskExecutor(pool);
    }
}