package com.sauda.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sauda.domain.enums.LotMatchStatus;
import com.sauda.dto.lotmatch.DistributorLotMatchCardResponse;
import com.sauda.dto.lotmatch.UpdateLotMatchStatusRequest;
import com.sauda.service.LotMatchService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LotMatchController.class)
class LotMatchControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private LotMatchService lotMatchService;

    @Test
    void listForDistributorReturnsCard() throws Exception {
        UUID distributorId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();

        when(lotMatchService.listForDistributor(eq(distributorId), eq(null), any()))
                .thenReturn(
                        new PageImpl<>(
                                List.of(
                                        new DistributorLotMatchCardResponse(
                                                matchId,
                                                LotMatchStatus.suggested,
                                                "SSD 1TB",
                                                "АО Заказчик",
                                                new BigDecimal("500000"),
                                                "KZT",
                                                null,
                                                null,
                                                "Алматы",
                                                "SSD",
                                                100,
                                                "шт",
                                                "Сертификат",
                                                "https://goszakup.kz/lot/1",
                                                "Samsung 990 PRO 1TB",
                                                "Samsung",
                                                "990 PRO",
                                                120,
                                                new BigDecimal("45000"),
                                                new BigDecimal("4500000"),
                                                new BigDecimal("-4000000"),
                                                new BigDecimal("0.85"),
                                                "Model match",
                                                List.of("brand"),
                                                List.of(),
                                                List.of(),
                                                true,
                                                null))));

        mockMvc.perform(get("/api/v1/distributors/{distributorId}/lot-matches", distributorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].matchId").value(matchId.toString()))
                .andExpect(jsonPath("$.content[0].title").value("SSD 1TB"))
                .andExpect(jsonPath("$.content[0].offerName").value("Samsung 990 PRO 1TB"));
    }

    @Test
    void updateStatusForDistributorReturnsUpdatedCard() throws Exception {
        UUID distributorId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();

        when(lotMatchService.updateStatusForDistributor(
                        eq(distributorId), eq(matchId), any(UpdateLotMatchStatusRequest.class)))
                .thenReturn(
                        new DistributorLotMatchCardResponse(
                                matchId,
                                LotMatchStatus.interested,
                                "SSD 1TB",
                                "АО Заказчик",
                                new BigDecimal("500000"),
                                "KZT",
                                null,
                                null,
                                "Алматы",
                                "SSD",
                                100,
                                "шт",
                                "Сертификат",
                                "https://goszakup.kz/lot/1",
                                "Samsung 990 PRO 1TB",
                                "Samsung",
                                "990 PRO",
                                120,
                                new BigDecimal("45000"),
                                new BigDecimal("4500000"),
                                null,
                                null,
                                "Model match",
                                List.of(),
                                List.of(),
                                List.of(),
                                false,
                                "Готовы участвовать"));

        mockMvc.perform(
                        patch(
                                        "/api/v1/distributors/{distributorId}/lot-matches/{matchId}/status",
                                        distributorId,
                                        matchId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"status":"interested","distributorComment":"Готовы участвовать"}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("interested"))
                .andExpect(jsonPath("$.distributorComment").value("Готовы участвовать"));
    }
}
