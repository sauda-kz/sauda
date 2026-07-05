package com.sauda.service.imports;

import com.sauda.config.ImportProperties;
import com.sauda.domain.entity.ImportRun;
import com.sauda.domain.entity.Offer;
import com.sauda.domain.entity.ParsedRow;
import com.sauda.domain.enums.ImportStatus;
import com.sauda.domain.enums.ParsedRowStatus;
import com.sauda.domain.enums.StockStatus;
import com.sauda.exception.SaudaException;
import com.sauda.repository.ImportRunRepository;
import com.sauda.repository.OfferRepository;
import com.sauda.repository.ParsedRowRepository;
import com.sauda.service.imports.model.ImportRowFields;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OfferUpsertService {

    private static final List<ParsedRowStatus> APPLICABLE_ROW_STATUSES =
            List.of(ParsedRowStatus.valid, ParsedRowStatus.edited);

    private final ParsedRowRepository parsedRowRepository;
    private final OfferRepository offerRepository;
    private final ImportRunRepository importRunRepository;
    private final ImportProperties importProperties;

    public OfferUpsertService(
            ParsedRowRepository parsedRowRepository,
            OfferRepository offerRepository,
            ImportRunRepository importRunRepository,
            ImportProperties importProperties) {
        this.parsedRowRepository = parsedRowRepository;
        this.offerRepository = offerRepository;
        this.importRunRepository = importRunRepository;
        this.importProperties = importProperties;
    }

    public void applyImportRun(ImportRun importRun) {
        ImportRunStatusTransitions.assertTransition(importRun.getStatus(), ImportStatus.applied);

        UUID runId = importRun.getId();
        UUID distributorId = importRun.getDistributor().getId();
        List<ParsedRow> applicableRows =
                parsedRowRepository.findByImportRunIdAndStatusIn(runId, APPLICABLE_ROW_STATUSES);

        Instant importedAt = Instant.now();
        int created = 0;
        int updated = 0;
        List<Offer> pendingOffers = new ArrayList<>();

        for (ParsedRow parsedRow : applicableRows) {
            ImportRowFields fields = ParsedRowEntityMapper.fromParsedData(parsedRow.getParsedData());
            assertApplicableFields(fields, parsedRow.getSourceRowNumber());

            Optional<Offer> existing =
                    offerRepository.findByDistributorIdAndInternalSku(distributorId, fields.sku());
            Offer offer = existing.orElseGet(Offer::new);
            if (existing.isPresent()) {
                updated++;
            } else {
                created++;
            }

            applyFieldsToOffer(offer, importRun, fields, importedAt);
            pendingOffers.add(offer);
        }

        saveOffersInBatches(pendingOffers);

        importRun.setStatus(ImportStatus.applied);
        importRunRepository.save(importRun);

        log.info(
                "Import run applied to offers: runId={}, distributorId={}, created={}, updated={}",
                runId,
                distributorId,
                created,
                updated);
    }

    private void saveOffersInBatches(List<Offer> offers) {
        int batchSize = importProperties.saveBatchSize();
        for (int index = 0; index < offers.size(); index += batchSize) {
            int end = Math.min(index + batchSize, offers.size());
            offerRepository.saveAll(offers.subList(index, end));
        }
    }

    private static void applyFieldsToOffer(
            Offer offer, ImportRun importRun, ImportRowFields fields, Instant importedAt) {
        offer.setDistributor(importRun.getDistributor());
        offer.setInternalSku(fields.sku());
        offer.setRawName(fields.name());
        offer.setBrand(fields.brand());
        offer.setModelMpn(fields.mpn());
        offer.setPrice(fields.price());
        offer.setPriceIncludesVat(fields.priceIncludesVat());
        offer.setStockQuantity(fields.stockQuantity());
        offer.setStockStatus(
                fields.stockStatus() != null ? fields.stockStatus() : StockStatus.unknown);
        offer.setLeadTime(formatLeadTime(fields.leadTimeDays()));
        offer.setSourceFile(importRun);
        offer.setLastImportedAt(importedAt);
        offer.setLastUpdatedAt(importedAt);
    }

    private static void assertApplicableFields(ImportRowFields fields, Integer sourceRowNumber) {
        if (fields.sku() == null || fields.sku().isBlank()) {
            throw new SaudaException(
                    "Parsed row "
                            + sourceRowNumber
                            + " cannot be applied: sku is required");
        }
        if (fields.name() == null || fields.name().isBlank()) {
            throw new SaudaException(
                    "Parsed row "
                            + sourceRowNumber
                            + " cannot be applied: name is required");
        }
    }

    private static String formatLeadTime(Integer leadTimeDays) {
        if (leadTimeDays == null) {
            return null;
        }
        return leadTimeDays + " days";
    }
}
