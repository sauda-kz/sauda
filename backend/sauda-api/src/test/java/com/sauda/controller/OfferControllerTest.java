package com.sauda.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sauda.domain.enums.StockStatus;
import com.sauda.dto.offer.OfferResponse;
import com.sauda.repository.AppUserRepository;
import com.sauda.service.OfferService;
import com.sauda.testsupport.WebMvcSecurityTestConfig;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OfferController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSecurityTestConfig.class)
class OfferControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private OfferService offerService;
    @MockitoBean private AppUserRepository appUserRepository;

    @Test
    @WithMockUser(authorities = "offer:read")
    void searchOffersReturnsPage() throws Exception {
        UUID offerId = UUID.randomUUID();
        UUID distributorId = UUID.randomUUID();

        when(offerService.search(any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse(offerId, distributorId))));

        mockMvc.perform(
                        get("/api/v1/offers")
                                .param("distributorId", distributorId.toString())
                                .param("brand", "Samsung")
                                .param("q", "990"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rawName").value("Samsung 990 PRO 1TB"));
    }

    @Test
    @WithMockUser(authorities = "offer:read")
    void getOfferReturnsDetails() throws Exception {
        UUID offerId = UUID.randomUUID();
        UUID distributorId = UUID.randomUUID();

        when(offerService.getById(offerId)).thenReturn(sampleResponse(offerId, distributorId));

        mockMvc.perform(get("/api/v1/offers/{id}", offerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(offerId.toString()))
                .andExpect(jsonPath("$.stockStatus").value("in_stock"));
    }

    @Test
    @WithMockUser(authorities = "offer:read")
    void searchPassesFiltersToService() throws Exception {
        UUID distributorId = UUID.randomUUID();
        when(offerService.search(any(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(
                        get("/api/v1/offers")
                                .param("distributorId", distributorId.toString())
                                .param("category", "SSD")
                                .param("stockStatus", "in_stock")
                                .param("activeOnly", "true"))
                .andExpect(status().isOk());
    }

    private static OfferResponse sampleResponse(UUID offerId, UUID distributorId) {
        return new OfferResponse(
                offerId,
                distributorId,
                "Tech Distributor",
                "Samsung 990 PRO 1TB",
                "Samsung",
                "990 PRO",
                "SSD",
                new BigDecimal("45000"),
                "KZT",
                true,
                120,
                StockStatus.in_stock,
                "3 days",
                Instant.now(),
                Instant.now());
    }
}
