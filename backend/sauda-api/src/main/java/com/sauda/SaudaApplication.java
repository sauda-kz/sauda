package com.sauda;

import com.sauda.config.JwtProperties;
import com.sauda.config.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, StorageProperties.class})
public class SaudaApplication {

    public static void main(String[] args) {
        SpringApplication.run(SaudaApplication.class, args);
    }
}
