package com.sauda.service;

import com.sauda.domain.entity.Lot;
import com.sauda.domain.entity.LotMatch;
import com.sauda.domain.entity.Offer;
import com.sauda.domain.enums.CheckResult;
import com.sauda.domain.enums.StockStatus;
import com.sauda.domain.stock.StockTextMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class LotMatchCalculator {

    public boolean applyDerivedFields(LotMatch match, Lot lot, Offer offer) {
        int requiredQuantity = lot.getQuantity() != null ? lot.getQuantity() : 0;
        int availableQuantity = offer.getStockQuantity() != null ? offer.getStockQuantity() : 0;
        match.setRequiredQuantity(requiredQuantity);
        match.setAvailableQuantity(availableQuantity);
        match.setBudgetAmount(lot.getBudgetAmount());

        if (offer.getPrice() != null) {
            match.setEstimatedUnitPrice(offer.getPrice());
            if (requiredQuantity > 0) {
                BigDecimal total = offer.getPrice().multiply(BigDecimal.valueOf(requiredQuantity));
                match.setEstimatedTotalPrice(total);
                if (lot.getBudgetAmount() != null) {
                    match.setEstimatedMargin(lot.getBudgetAmount().subtract(total));
                }
            }
        }

        StockStatus stockStatus = offer.getStockStatus();
        match.setQuantityCheck(
                resolveQuantityCheck(requiredQuantity, availableQuantity, stockStatus));
        match.setStockCheck(resolveStockCheck(stockStatus, availableQuantity));
        match.setPriceCheck(
                resolvePriceCheck(lot.getBudgetAmount(), match.getEstimatedTotalPrice()));

        return applyReviewFlags(match, stockStatus);
    }

    private static boolean applyReviewFlags(LotMatch match, StockStatus stockStatus) {
        if (stockStatus != StockStatus.on_order) {
            return false;
        }
        match.setNeedsManualReview(true);
        List<String> flags = match.getRiskFlags();
        if (flags == null) {
            flags = new ArrayList<>();
            match.setRiskFlags(flags);
        }
        if (!flags.contains(StockTextMapper.ON_ORDER_RISK_FLAG)) {
            flags.add(StockTextMapper.ON_ORDER_RISK_FLAG);
        }
        return true;
    }

    private static CheckResult resolveQuantityCheck(
            int required, int available, StockStatus stockStatus) {
        if (stockStatus == StockStatus.on_order) {
            return CheckResult.unknown;
        }
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
        if (stockStatus == StockStatus.low_stock
                || stockStatus == StockStatus.on_order
                || stockStatus == StockStatus.unknown) {
            return CheckResult.unknown;
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
