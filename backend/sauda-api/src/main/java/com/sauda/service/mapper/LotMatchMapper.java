package com.sauda.service.mapper;

import com.sauda.domain.entity.LotMatch;
import com.sauda.dto.lotmatch.DistributorLotMatchCardResponse;
import com.sauda.dto.lotmatch.LotMatchResponse;
import java.util.Collections;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface LotMatchMapper {

    @Mapping(source = "lot.id", target = "lotId")
    @Mapping(source = "offer.id", target = "offerId")
    @Mapping(source = "distributor.id", target = "distributorId")
    @Mapping(source = "matchStatus", target = "status")
    @Mapping(
            source = "matchedRequirements",
            target = "matchedRequirements",
            qualifiedByName = "safeList")
    @Mapping(
            source = "missingRequirements",
            target = "missingRequirements",
            qualifiedByName = "safeList")
    @Mapping(source = "riskFlags", target = "riskFlags", qualifiedByName = "safeList")
    LotMatchResponse toResponse(LotMatch match);

    @Mapping(source = "id", target = "matchId")
    @Mapping(source = "matchStatus", target = "status")
    @Mapping(source = "lot.title", target = "title")
    @Mapping(source = "lot.customerName", target = "customerName")
    @Mapping(source = "lot.budgetAmount", target = "budgetAmount")
    @Mapping(source = "lot.currency", target = "currency")
    @Mapping(source = "lot.deliveryDeadline", target = "deliveryDeadline")
    @Mapping(source = "lot.submissionDeadline", target = "submissionDeadline")
    @Mapping(source = "lot.deliveryLocation", target = "deliveryLocation")
    @Mapping(source = "lot.category", target = "category")
    @Mapping(source = "lot.quantity", target = "quantity")
    @Mapping(source = "lot.unit", target = "unit")
    @Mapping(source = "lot.requiredDocuments", target = "requiredDocuments")
    @Mapping(source = "lot.sourceUrl", target = "sourceUrl")
    @Mapping(source = "offer.rawName", target = "offerName")
    @Mapping(source = "offer.brand", target = "brand")
    @Mapping(source = "offer.modelMpn", target = "modelMpn")
    @Mapping(source = "offer.stockQuantity", target = "stockQuantity")
    @Mapping(source = "offer.stockStatus", target = "stockStatus")
    @Mapping(source = "offer.priceIncludesVat", target = "priceIncludesVat")
    @Mapping(
            source = "matchedRequirements",
            target = "matchedRequirements",
            qualifiedByName = "safeList")
    @Mapping(
            source = "missingRequirements",
            target = "missingRequirements",
            qualifiedByName = "safeList")
    @Mapping(source = "riskFlags", target = "riskFlags", qualifiedByName = "safeList")
    DistributorLotMatchCardResponse toDistributorCard(LotMatch match);

    @Named("safeList")
    default List<String> safeList(List<String> values) {
        return values != null ? values : Collections.emptyList();
    }
}
