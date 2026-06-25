package com.sauda;

import com.sauda.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class SaudaApplication {

    public static void main(String[] args) {
        SpringApplication.run(SaudaApplication.class, args);
    }
}
