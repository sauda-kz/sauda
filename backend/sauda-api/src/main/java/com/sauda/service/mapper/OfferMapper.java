package com.sauda.service.mapper;

import com.sauda.domain.entity.Offer;
import com.sauda.dto.offer.OfferResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface OfferMapper {

    @Mapping(source = "distributor.id", target = "distributorId")
    @Mapping(source = "distributor.name", target = "distributorName")
    @Mapping(source = "offer", target = "category", qualifiedByName = "resolveCategory")
    OfferResponse toResponse(Offer offer);

    @Named("resolveCategory")
    default String resolveCategory(Offer offer) {
        if (offer.getCanonicalProduct() == null) {
            return null;
        }
        return offer.getCanonicalProduct().getCategory();
    }
}
