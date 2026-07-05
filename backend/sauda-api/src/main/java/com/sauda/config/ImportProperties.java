package com.sauda.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sauda.imports")
public record ImportProperties(int maxRows, Character csvDelimiter) {

    public ImportProperties {
        if (maxRows <= 0) {
            maxRows = 100_000;
        }
    }
}
