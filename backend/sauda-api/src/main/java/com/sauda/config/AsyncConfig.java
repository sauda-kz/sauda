package com.sauda.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "importTaskExecutor")
    public Executor importTaskExecutor(ImportProperties importProperties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(importProperties.asyncCorePoolSize());
        executor.setMaxPoolSize(importProperties.asyncMaxPoolSize());
        executor.setQueueCapacity(importProperties.asyncQueueCapacity());
        executor.setThreadNamePrefix("import-");
        executor.initialize();
        return executor;
    }
}
