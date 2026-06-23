package com.sauda.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sauda.dto.HealthResponse;
import com.sauda.service.HealthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private HealthService healthService;

    @Test
    void healthReturnsUpStatus() throws Exception {
        when(healthService.getHealth())
                .thenReturn(new HealthResponse("up", "sauda-api", "0.1.0-SNAPSHOT"));

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("up"))
                .andExpect(jsonPath("$.service").value("sauda-api"))
                .andExpect(jsonPath("$.version").value("0.1.0-SNAPSHOT"));
    }
}
