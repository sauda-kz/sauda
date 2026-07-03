package com.sauda.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sauda.domain.enums.LotDataQualityStatus;
import com.sauda.domain.enums.LotMatchStatus;
import com.sauda.domain.enums.LotStatus;
import com.sauda.dto.lot.LotResponse;
import com.sauda.repository.AppUserRepository;
import com.sauda.service.LotMatchService;
import com.sauda.service.LotMatchSuggestionService;
import com.sauda.service.LotService;
import com.sauda.testsupport.LotTestFixtures;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LotController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSecurityTestConfig.class)
class LotControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private LotService lotService;
    @MockitoBean private LotMatchSuggestionService lotMatchSuggestionService;
    @MockitoBean private LotMatchService lotMatchService;
    @MockitoBean private AppUserRepository appUserRepository;

    @Test
    @WithMockUser(authorities = "lot:read")
    void listLotsReturnsPage() throws Exception {
        UUID lotId = UUID.randomUUID();
        when(lotService.listLots(eq(null), eq(null), eq(null), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(sampleLotResponse(lotId))));

        mockMvc.perform(get("/api/v1/lots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("SSD 1TB"))
                .andExpect(jsonPath("$.content[0].matchCount").value(0));
    }

    @Test
    @WithMockUser(authorities = "lot:read")
    void listLotsAcceptsSearchFilters() throws Exception {
        when(lotService.listLots(eq(LotStatus.active), eq("SSD"), eq("SSD"), eq("manual"), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(
                        get("/api/v1/lots")
                                .param("status", "active")
                                .param("q", "SSD")
                                .param("category", "SSD")
                                .param("source", "manual"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "lot_match:manage")
    void sendToDistributorReturnsMatch() throws Exception {
        UUID lotId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();

        when(lotMatchService.sendToDistributor(eq(lotId), any()))
                .thenReturn(
                        new com.sauda.dto.lotmatch.LotMatchResponse(
                                matchId,
                                lotId,
                                offerId,
                                UUID.randomUUID(),
                                LotMatchStatus.matched,
                                null,
                                "Category match",
                                List.of(),
                                List.of(),
                                List.of(),
                                10,
                                15,
                                com.sauda.domain.enums.CheckResult.ok,
                                com.sauda.domain.enums.CheckResult.ok,
                                com.sauda.domain.enums.CheckResult.ok,
                                null,
                                null,
                                null,
                                null,
                                false,
                                "",
                                null,
                                java.time.Instant.now(),
                                null,
                                null));

        mockMvc.perform(
                        post("/api/v1/lots/{id}/send-to-distributor", lotId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"offerId":"%s","matchReason":"Category match"}
                                        """
                                                .formatted(offerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("matched"))
                .andExpect(jsonPath("$.sentToDistributorAt").exists());
    }

    @Test
    @WithMockUser(authorities = "lot:create")
    void createLotReturnsCreated() throws Exception {
        when(lotService.createLot(any())).thenReturn(sampleLotResponse(UUID.randomUUID()));

        mockMvc.perform(
                        post("/api/v1/lots")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createLotJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.externalLotId").value("LOT-001"));
    }

    @Test
    @WithMockUser(authorities = "lot:manage")
    void updateLotReturnsOk() throws Exception {
        UUID lotId = UUID.randomUUID();
        when(lotService.updateLot(eq(lotId), any())).thenReturn(sampleLotResponse(lotId));

        mockMvc.perform(
                        put("/api/v1/lots/{id}", lotId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createLotJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(lotId.toString()));
    }

    @Test
    @WithMockUser(authorities = "lot:manage")
    void archiveLotReturnsArchivedStatus() throws Exception {
        UUID lotId = UUID.randomUUID();
        when(lotService.archiveLot(lotId))
                .thenReturn(
                        new LotResponse(
                                lotId,
                                "manual",
                                "PUR-001",
                                "LOT-001",
                                "SSD 1TB",
                                "Description",
                                "АО Заказчик",
                                "SSD",
                                null,
                                "товар",
                                10,
                                "шт",
                                new BigDecimal("500000"),
                                "KZT",
                                "Алматы",
                                Instant.parse("2026-07-01T00:00:00Z"),
                                null,
                                null,
                                "NVMe",
                                "Сертификат",
                                null,
                                null,
                                null,
                                LotStatus.archived,
                                "https://goszakup.kz/lot/1",
                                null,
                                null,
                                Instant.now(),
                                Instant.now(),
                                LotDataQualityStatus.complete,
                                List.of(),
                                0L));

        mockMvc.perform(patch("/api/v1/lots/{id}/archive", lotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("archived"));
    }

    private static LotResponse sampleLotResponse(UUID lotId) {
        var request = LotTestFixtures.sampleCreateLotRequest();
        return new LotResponse(
                lotId,
                request.source(),
                request.externalPurchaseId(),
                request.externalLotId(),
                request.title(),
                request.description(),
                request.customerName(),
                request.category(),
                request.procurementMethod(),
                request.lotType(),
                request.quantity(),
                request.unit(),
                request.budgetAmount(),
                request.currency(),
                request.deliveryLocation(),
                request.deliveryDeadline(),
                request.submissionDeadline(),
                request.warrantyRequirements(),
                request.technicalRequirements(),
                request.requiredDocuments(),
                request.qualificationRequirements(),
                request.contractTermsSummary(),
                request.publishedAt(),
                request.status(),
                request.sourceUrl(),
                request.rawText(),
                UUID.randomUUID(),
                Instant.now(),
                Instant.now(),
                LotDataQualityStatus.complete,
                List.of(),
                0L);
    }

    private static String createLotJson() {
        return """
                {
                  "source": "manual",
                  "externalPurchaseId": "PUR-001",
                  "externalLotId": "LOT-001",
                  "title": "SSD 1TB",
                  "description": "Description",
                  "customerName": "АО Заказчик",
                  "category": "SSD",
                  "procurementMethod": "запрос ценовых предложений",
                  "lotType": "товар",
                  "quantity": 10,
                  "unit": "шт",
                  "budgetAmount": 500000,
                  "currency": "KZT",
                  "deliveryLocation": "Алматы",
                  "deliveryDeadline": "2026-07-01T00:00:00Z",
                  "submissionDeadline": "2026-06-15T00:00:00Z",
                  "warrantyRequirements": "24 месяца",
                  "technicalRequirements": "NVMe, 1TB",
                  "requiredDocuments": "Сертификат соответствия",
                  "qualificationRequirements": "Опыт от 3 лет",
                  "contractTermsSummary": "Оплата по факту",
                  "publishedAt": "2026-06-01T00:00:00Z",
                  "status": "active",
                  "sourceUrl": "https://goszakup.kz/lot/1",
                  "rawText": "raw text"
                }
                """;
    }
}
