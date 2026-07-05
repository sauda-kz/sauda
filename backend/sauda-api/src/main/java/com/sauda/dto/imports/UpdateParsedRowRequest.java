package com.sauda.dto.imports;

import com.sauda.domain.enums.StockStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record UpdateParsedRowRequest(
        @NotBlank String sku,
        @NotBlank String name,
        String brand,
        String mpn,
        @DecimalMin(value = "0", inclusive = true) BigDecimal price,
        Boolean priceIncludesVat,
        @PositiveOrZero Integer stockQuantity,
        StockStatus stockStatus,
        @PositiveOrZero Integer leadTimeDays) {}
