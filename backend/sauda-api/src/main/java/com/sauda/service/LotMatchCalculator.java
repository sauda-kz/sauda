package com.sauda.service;

import com.sauda.domain.entity.Lot;
import com.sauda.domain.entity.LotMatch;
import com.sauda.domain.entity.Offer;
import com.sauda.domain.enums.CheckResult;
import com.sauda.domain.enums.StockStatus;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class LotMatchCalculator {

    public void applyDerivedFields(LotMatch match, Lot lot, Offer offer) {
        int requiredQuantity = lot.getQuantity() != null ? lot.getQuantity() : 0;
        int availableQuantity = offer.getStockQty() != null ? offer.getStockQty() : 0;
        match.setRequiredQuantity(requiredQuantity);
        match.setAvailableQuantity(availableQuantity);
        match.setBudgetAmount(lot.getBudgetAmount());

        if (offer.getPrice() != null) {
            match.setEstimatedUnitPrice(offer.getPrice());
            if (requiredQuantity > 0) {
                BigDecimal total =
                        offer.getPrice().multiply(BigDecimal.valueOf(requiredQuantity));
                match.setEstimatedTotalPrice(total);
                if (lot.getBudgetAmount() != null) {
                    match.setEstimatedMargin(lot.getBudgetAmount().subtract(total));
                }
            }
        }

        match.setQuantityCheck(resolveQuantityCheck(requiredQuantity, availableQuantity));
        match.setStockCheck(resolveStockCheck(offer.getStockStatus(), availableQuantity));
        match.setPriceCheck(resolvePriceCheck(lot.getBudgetAmount(), match.getEstimatedTotalPrice()));
    }

    private static CheckResult resolveQuantityCheck(int required, int available) {
        if (required <= 0) {
            return CheckResult.unknown;
        }
        return available >= required ? CheckResult.ok : CheckResult.fail;
    }

    private static CheckResult resolveStockCheck(StockStatus stockStatus, int availableQuantity) {
        if (stockStatus == StockStatus.in_stock && availableQuantity > 0) {
            return CheckResult.ok;
        }
        if (stockStatus == StockStatus.out_of_stock) {
            return CheckResult.fail;
        }
        return CheckResult.unknown;
    }

    private static CheckResult resolvePriceCheck(BigDecimal budget, BigDecimal estimatedTotal) {
        if (budget == null || estimatedTotal == null) {
            return CheckResult.unknown;
        }
        return estimatedTotal.compareTo(budget) <= 0 ? CheckResult.ok : CheckResult.fail;
    }
}
