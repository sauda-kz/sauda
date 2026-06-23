package com.sauda.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class HealthServiceTest {

    @Autowired private HealthService healthService;

    @Test
    void getHealthReturnsUpStatus() {
        var response = healthService.getHealth();

        assertThat(response.status()).isEqualTo("up");
        assertThat(response.service()).isEqualTo("sauda-api");
        assertThat(response.version()).isNotBlank();
    }
}
