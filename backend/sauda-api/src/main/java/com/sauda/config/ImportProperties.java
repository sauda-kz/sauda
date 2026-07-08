package com.sauda.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sauda.imports")
public record ImportProperties(
        int maxRows,
        Character csvDelimiter,
        int asyncCorePoolSize,
        int asyncMaxPoolSize,
        int asyncQueueCapacity,
        int saveBatchSize) {

    public ImportProperties {
        if (maxRows <= 0) {
            maxRows = 100_000;
        }
        if (asyncCorePoolSize <= 0) {
            asyncCorePoolSize = 2;
        }
        if (asyncMaxPoolSize <= 0) {
            asyncMaxPoolSize = 4;
        }
        if (asyncQueueCapacity <= 0) {
            asyncQueueCapacity = 50;
        }
        if (saveBatchSize <= 0) {
            saveBatchSize = 500;
        }
    }
}
