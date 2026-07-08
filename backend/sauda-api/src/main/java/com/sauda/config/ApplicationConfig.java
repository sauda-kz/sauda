package com.sauda.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
public class ApplicationConfig {

    private static final int MAX_PAGE_SIZE = 100;

    @Bean
    PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
        return pageable -> pageable.setMaxPageSize(MAX_PAGE_SIZE);
    }
}
