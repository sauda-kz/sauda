package com.sauda.dto.offer;

import com.sauda.domain.enums.StockStatus;
import java.util.UUID;

public record OfferSearchCriteria(
        UUID distributorId,
        String category,
        String brand,
        String query,
        StockStatus stockStatus,
        boolean activeOnly) {}
