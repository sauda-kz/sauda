package com.sauda.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {

    @Bean
    OpenAPI openAPI(@Value("${sauda.version:0.1.0-SNAPSHOT}") String version) {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Sauda API")
                                .description("Sauda B2B Platform REST API")
                                .version(version));
    }
}
