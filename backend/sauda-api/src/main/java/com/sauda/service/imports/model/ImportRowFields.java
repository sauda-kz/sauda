package com.sauda.service.imports.model;

import com.sauda.domain.enums.StockStatus;
import java.math.BigDecimal;

public record ImportRowFields(
        String sku,
        String name,
        String brand,
        String mpn,
        BigDecimal price,
        Boolean priceIncludesVat,
        Integer stockQuantity,
        StockStatus stockStatus,
        Integer leadTimeDays) {}
