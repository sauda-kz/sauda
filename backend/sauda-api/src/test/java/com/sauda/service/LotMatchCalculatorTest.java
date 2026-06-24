package com.sauda.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sauda.domain.entity.Lot;
import com.sauda.domain.entity.LotMatch;
import com.sauda.domain.entity.Offer;
import com.sauda.domain.enums.CheckResult;
import com.sauda.domain.enums.StockStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class LotMatchCalculatorTest {

    private final LotMatchCalculator calculator = new LotMatchCalculator();

    @Test
    void applyDerivedFieldsCalculatesMarginAndChecks() {
        Lot lot = new Lot();
        lot.setQuantity(10);
        lot.setBudgetAmount(new BigDecimal("100000"));

        Offer offer = new Offer();
        offer.setPrice(new BigDecimal("8000"));
        offer.setStockQty(15);
        offer.setStockStatus(StockStatus.in_stock);

        LotMatch match = new LotMatch();
        calculator.applyDerivedFields(match, lot, offer);

        assertThat(match.getRequiredQuantity()).isEqualTo(10);
        assertThat(match.getAvailableQuantity()).isEqualTo(15);
        assertThat(match.getEstimatedTotalPrice()).isEqualByComparingTo("80000");
        assertThat(match.getEstimatedMargin()).isEqualByComparingTo("20000");
        assertThat(match.getQuantityCheck()).isEqualTo(CheckResult.ok);
        assertThat(match.getStockCheck()).isEqualTo(CheckResult.ok);
        assertThat(match.getPriceCheck()).isEqualTo(CheckResult.ok);
    }
}
