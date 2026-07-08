package com.sauda.service.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sauda.config.ImportProperties;
import com.sauda.domain.entity.ImportRun;
import com.sauda.domain.entity.Offer;
import com.sauda.domain.entity.Organization;
import com.sauda.domain.entity.ParsedRow;
import com.sauda.domain.enums.ImportStatus;
import com.sauda.domain.enums.OrganizationType;
import com.sauda.domain.enums.ParsedRowStatus;
import com.sauda.domain.enums.StockStatus;
import com.sauda.exception.SaudaException;
import com.sauda.repository.ImportRunRepository;
import com.sauda.repository.OfferRepository;
import com.sauda.repository.ParsedRowRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OfferUpsertServiceTest {

    @Mock private ParsedRowRepository parsedRowRepository;
    @Mock private OfferRepository offerRepository;
    @Mock private ImportRunRepository importRunRepository;

    private OfferUpsertService offerUpsertService;

    private UUID runId;
    private UUID distributorId;
    private ImportRun importRun;
    private Organization distributor;

    @BeforeEach
    void setUp() {
        offerUpsertService =
                new OfferUpsertService(
                        parsedRowRepository,
                        offerRepository,
                        importRunRepository,
                        new ImportProperties(100, null, 2, 4, 50, 2));

        runId = UUID.randomUUID();
        distributorId = UUID.randomUUID();

        distributor = new Organization();
        distributor.setId(distributorId);
        distributor.setType(OrganizationType.distributor);

        importRun = new ImportRun();
        importRun.setId(runId);
        importRun.setDistributor(distributor);
        importRun.setStatus(ImportStatus.approved);
    }

    @Test
    void applyImportRunCreatesNewOfferFromValidRow() {
        ParsedRow parsedRow =
                parsedRow(2, ParsedRowStatus.valid, parsedData("SKU-NEW", "New Item", true));
        when(parsedRowRepository.findByImportRunIdAndStatusIn(
                        runId, List.of(ParsedRowStatus.valid, ParsedRowStatus.edited)))
                .thenReturn(List.of(parsedRow));
        when(offerRepository.findByDistributorIdAndInternalSku(distributorId, "SKU-NEW"))
                .thenReturn(Optional.empty());
        when(offerRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(importRunRepository.save(any(ImportRun.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        offerUpsertService.applyImportRun(importRun);

        ArgumentCaptor<List<Offer>> offersCaptor = ArgumentCaptor.forClass(List.class);
        verify(offerRepository).saveAll(offersCaptor.capture());
        Offer savedOffer = offersCaptor.getValue().getFirst();

        assertThat(savedOffer.getDistributor()).isSameAs(distributor);
        assertThat(savedOffer.getInternalSku()).isEqualTo("SKU-NEW");
        assertThat(savedOffer.getRawName()).isEqualTo("New Item");
        assertThat(savedOffer.getBrand()).isEqualTo("Brand");
        assertThat(savedOffer.getModelMpn()).isEqualTo("MPN-1");
        assertThat(savedOffer.getPrice()).isEqualByComparingTo("100.50");
        assertThat(savedOffer.getPriceIncludesVat()).isTrue();
        assertThat(savedOffer.getStockQuantity()).isEqualTo(10);
        assertThat(savedOffer.getStockStatus()).isEqualTo(StockStatus.in_stock);
        assertThat(savedOffer.getLeadTime()).isEqualTo("3 days");
        assertThat(savedOffer.getSourceFile()).isSameAs(importRun);
        assertThat(savedOffer.getLastImportedAt()).isNotNull();
        assertThat(savedOffer.getCanonicalProduct()).isNull();

        assertThat(importRun.getStatus()).isEqualTo(ImportStatus.applied);
        verify(importRunRepository).save(importRun);
    }

    @Test
    void applyImportRunUpdatesExistingOfferBySku() {
        ParsedRow parsedRow =
                parsedRow(
                        3,
                        ParsedRowStatus.edited,
                        parsedData("SKU-EXISTING", "Updated Item", false));
        Offer existing = new Offer();
        existing.setId(UUID.randomUUID());
        existing.setDistributor(distributor);
        existing.setInternalSku("SKU-EXISTING");
        existing.setRawName("Old Item");
        existing.setPriceIncludesVat(true);

        when(parsedRowRepository.findByImportRunIdAndStatusIn(
                        runId, List.of(ParsedRowStatus.valid, ParsedRowStatus.edited)))
                .thenReturn(List.of(parsedRow));
        when(offerRepository.findByDistributorIdAndInternalSku(distributorId, "SKU-EXISTING"))
                .thenReturn(Optional.of(existing));
        when(offerRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(importRunRepository.save(any(ImportRun.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        offerUpsertService.applyImportRun(importRun);

        ArgumentCaptor<List<Offer>> offersCaptor = ArgumentCaptor.forClass(List.class);
        verify(offerRepository).saveAll(offersCaptor.capture());
        Offer savedOffer = offersCaptor.getValue().getFirst();

        assertThat(savedOffer.getId()).isEqualTo(existing.getId());
        assertThat(savedOffer.getRawName()).isEqualTo("Updated Item");
        assertThat(savedOffer.getPriceIncludesVat()).isFalse();
        assertThat(importRun.getStatus()).isEqualTo(ImportStatus.applied);
    }

    @Test
    void applyImportRunLoadsOnlyValidAndEditedRows() {
        when(parsedRowRepository.findByImportRunIdAndStatusIn(
                        runId, List.of(ParsedRowStatus.valid, ParsedRowStatus.edited)))
                .thenReturn(List.of());
        when(importRunRepository.save(any(ImportRun.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        offerUpsertService.applyImportRun(importRun);

        verify(offerRepository, never()).saveAll(anyList());
        verify(parsedRowRepository)
                .findByImportRunIdAndStatusIn(
                        eq(runId), eq(List.of(ParsedRowStatus.valid, ParsedRowStatus.edited)));
        assertThat(importRun.getStatus()).isEqualTo(ImportStatus.applied);
    }

    @Test
    void applyImportRunMapsPriceIncludesVatDirectly() {
        ParsedRow parsedRow =
                parsedRow(4, ParsedRowStatus.valid, parsedData("SKU-VAT", "Item", null));
        when(parsedRowRepository.findByImportRunIdAndStatusIn(
                        runId, List.of(ParsedRowStatus.valid, ParsedRowStatus.edited)))
                .thenReturn(List.of(parsedRow));
        when(offerRepository.findByDistributorIdAndInternalSku(distributorId, "SKU-VAT"))
                .thenReturn(Optional.empty());
        when(offerRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(importRunRepository.save(any(ImportRun.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        offerUpsertService.applyImportRun(importRun);

        ArgumentCaptor<List<Offer>> offersCaptor = ArgumentCaptor.forClass(List.class);
        verify(offerRepository).saveAll(offersCaptor.capture());
        assertThat(offersCaptor.getValue().getFirst().getPriceIncludesVat()).isNull();
    }

    @Test
    void applyImportRunRollsBackWhenSaveFails() {
        ParsedRow parsedRow =
                parsedRow(5, ParsedRowStatus.valid, parsedData("SKU-FAIL", "Item", true));
        when(parsedRowRepository.findByImportRunIdAndStatusIn(
                        runId, List.of(ParsedRowStatus.valid, ParsedRowStatus.edited)))
                .thenReturn(List.of(parsedRow));
        when(offerRepository.findByDistributorIdAndInternalSku(distributorId, "SKU-FAIL"))
                .thenReturn(Optional.empty());
        when(offerRepository.saveAll(anyList())).thenThrow(new RuntimeException("DB unavailable"));

        assertThatThrownBy(() -> offerUpsertService.applyImportRun(importRun))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB unavailable");

        assertThat(importRun.getStatus()).isEqualTo(ImportStatus.approved);
        verify(importRunRepository, never()).save(any(ImportRun.class));
    }

    @Test
    void applyImportRunRejectedFromAwaitingApprovalStatus() {
        importRun.setStatus(ImportStatus.awaiting_approval);

        assertThatThrownBy(() -> offerUpsertService.applyImportRun(importRun))
                .isInstanceOf(SaudaException.class)
                .hasMessageContaining("Invalid import run status transition");

        verify(offerRepository, never()).saveAll(anyList());
    }

    private static ParsedRow parsedRow(
            int rowNumber, ParsedRowStatus status, Map<String, Object> parsedData) {
        ParsedRow parsedRow = new ParsedRow();
        parsedRow.setSourceRowNumber(rowNumber);
        parsedRow.setStatus(status);
        parsedRow.setParsedData(parsedData);
        parsedRow.setRawRowData(Map.of());
        return parsedRow;
    }

    private static Map<String, Object> parsedData(
            String sku, String name, Boolean priceIncludesVat) {
        Map<String, Object> parsedData = new LinkedHashMap<>();
        parsedData.put("sku", sku);
        parsedData.put("name", name);
        parsedData.put("brand", "Brand");
        parsedData.put("mpn", "MPN-1");
        parsedData.put("price", new BigDecimal("100.50"));
        parsedData.put("price_includes_vat", priceIncludesVat);
        parsedData.put("stock_quantity", 10);
        parsedData.put("stock_status", StockStatus.in_stock.name());
        parsedData.put("lead_time_days", 3);
        return parsedData;
    }
}
