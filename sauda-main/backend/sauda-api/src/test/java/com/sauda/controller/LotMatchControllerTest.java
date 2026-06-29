package com.sauda.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sauda.domain.enums.CheckResult;
import com.sauda.domain.enums.LotMatchStatus;
import com.sauda.dto.lotmatch.CreateLotMatchRequest;
import com.sauda.dto.lotmatch.DistributorLotMatchCardResponse;
import com.sauda.dto.lotmatch.LotMatchResponse;
import com.sauda.dto.lotmatch.UpdateLotMatchStatusRequest;
import com.sauda.repository.AppUserRepository;
import com.sauda.service.LotMatchService;
import com.sauda.testsupport.WebMvcSecurityTestConfig;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LotMatchController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSecurityTestConfig.class)
class LotMatchControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private LotMatchService lotMatchService;
    @MockitoBean private AppUserRepository appUserRepository;

    @Test
    @WithMockUser(authorities = "lot_match:read")
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
    @WithMockUser(authorities = "lot_match:review")
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

    @Test
    @WithMockUser(authorities = "lot_match:manage")
    void createMatchReturnsCreated() throws Exception {
        UUID lotId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();

        when(lotMatchService.createMatch(any(CreateLotMatchRequest.class)))
                .thenReturn(
                        sampleLotMatchResponse(matchId, lotId, offerId, LotMatchStatus.suggested));

        mockMvc.perform(
                        post("/api/v1/lot-matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "lotId":"%s",
                                          "offerId":"%s",
                                          "matchReason":"Model match"
                                        }
                                        """
                                                .formatted(lotId, offerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(matchId.toString()));
    }

    @Test
    @WithMockUser(authorities = "lot_match:read")
    void listMatchesByLotReturnsPage() throws Exception {
        UUID lotId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();

        when(lotMatchService.listByLot(eq(lotId), any()))
                .thenReturn(
                        new PageImpl<>(
                                List.of(
                                        sampleLotMatchResponse(
                                                matchId,
                                                lotId,
                                                UUID.randomUUID(),
                                                LotMatchStatus.suggested))));

        mockMvc.perform(get("/api/v1/lot-matches").param("lotId", lotId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].lotId").value(lotId.toString()));
    }

    @Test
    @WithMockUser(authorities = "lot_match:read")
    void getMatchReturnsResponse() throws Exception {
        UUID matchId = UUID.randomUUID();

        when(lotMatchService.getMatch(matchId))
                .thenReturn(
                        sampleLotMatchResponse(
                                matchId,
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                LotMatchStatus.matched));

        mockMvc.perform(get("/api/v1/lot-matches/{matchId}", matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("matched"));
    }

    private static LotMatchResponse sampleLotMatchResponse(
            UUID matchId, UUID lotId, UUID offerId, LotMatchStatus status) {
        return new LotMatchResponse(
                matchId,
                lotId,
                offerId,
                UUID.randomUUID(),
                status,
                null,
                "reason",
                List.of(),
                List.of(),
                List.of(),
                10,
                15,
                CheckResult.unknown,
                CheckResult.unknown,
                CheckResult.unknown,
                null,
                null,
                null,
                null,
                true,
                "",
                null,
                null,
                null);
    }
}
