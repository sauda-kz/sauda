package com.sauda.service;

import com.sauda.dto.HealthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    private final String applicationName;
    private final String applicationVersion;

    public HealthService(
            @Value("${spring.application.name:sauda-api}") String applicationName,
            @Value("${sauda.version:0.1.0-SNAPSHOT}") String applicationVersion) {
        this.applicationName = applicationName;
        this.applicationVersion = applicationVersion;
    }

    public HealthResponse getHealth() {
        return new HealthResponse("up", applicationName, applicationVersion);
    }
}
