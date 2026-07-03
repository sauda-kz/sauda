package com.sauda.testsupport;

import com.sauda.domain.entity.CanonicalProduct;
import com.sauda.domain.entity.Offer;
import com.sauda.domain.entity.Organization;
import com.sauda.domain.enums.OrganizationType;
import com.sauda.domain.enums.StockStatus;
import java.math.BigDecimal;
import java.util.UUID;

public final class OfferTestFixtures {

    private OfferTestFixtures() {}

    public static Offer sampleOffer(UUID offerId, Organization distributor) {
        CanonicalProduct canonical = new CanonicalProduct();
        canonical.setId(UUID.randomUUID());
        canonical.setNormalizedName("Samsung 990 PRO 1TB");
        canonical.setCategory("SSD");
        canonical.setBrand("Samsung");
        canonical.setActive(true);

        Offer offer = new Offer();
        offer.setId(offerId);
        offer.setDistributor(distributor);
        offer.setCanonicalProduct(canonical);
        offer.setRawName("Samsung 990 PRO 1TB");
        offer.setBrand("Samsung");
        offer.setModelMpn("990 PRO");
        offer.setPrice(new BigDecimal("45000"));
        offer.setPriceIncludesVat(true);
        offer.setStockQuantity(120);
        offer.setStockStatus(StockStatus.in_stock);
        offer.setLeadTime("3 days");
        return offer;
    }

    public static Organization sampleDistributor(UUID distributorId) {
        Organization distributor = new Organization();
        distributor.setId(distributorId);
        distributor.setType(OrganizationType.distributor);
        distributor.setName("Tech Distributor");
        return distributor;
    }
}
