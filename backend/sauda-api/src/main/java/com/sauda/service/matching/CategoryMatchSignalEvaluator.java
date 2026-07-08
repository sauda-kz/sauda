package com.sauda.service.matching;

import com.sauda.domain.entity.Lot;
import com.sauda.domain.entity.Offer;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Order(10)
@Component
public class CategoryMatchSignalEvaluator implements MatchSignalEvaluator {

    private static final BigDecimal WEIGHT = new BigDecimal("0.40");

    @Override
    public Optional<MatchSignal> evaluate(Lot lot, Offer offer) {
        if (!StringUtils.hasText(lot.getCategory())
                || offer.getCanonicalProduct() == null
                || !StringUtils.hasText(offer.getCanonicalProduct().getCategory())) {
            return Optional.empty();
        }
        if (!lot.getCategory().equalsIgnoreCase(offer.getCanonicalProduct().getCategory())) {
            return Optional.empty();
        }
        return Optional.of(new MatchSignal("Совпала категория: " + lot.getCategory(), WEIGHT));
    }
}
