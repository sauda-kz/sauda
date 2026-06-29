package com.sauda.testsupport;

import com.sauda.config.JwtProperties;
import com.sauda.repository.AppUserRepository;
import com.sauda.security.jwt.JwtAuthenticationFilter;
import com.sauda.security.jwt.JwtTokenProvider;
import com.sauda.security.user.SaudaUserDetailsService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
@EnableConfigurationProperties(JwtProperties.class)
public class WebMvcSecurityTestConfig {

    @Bean
    @Primary
    JwtTokenProvider jwtTokenProvider(JwtProperties jwtProperties) {
        return new JwtTokenProvider(jwtProperties);
    }

    @Bean
    @Primary
    JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider, SaudaUserDetailsService saudaUserDetailsService) {
        return new JwtAuthenticationFilter(jwtTokenProvider, saudaUserDetailsService);
    }

    @Bean
    @Primary
    SaudaUserDetailsService saudaUserDetailsService(AppUserRepository appUserRepository) {
        return new SaudaUserDetailsService(appUserRepository);
    }
}
