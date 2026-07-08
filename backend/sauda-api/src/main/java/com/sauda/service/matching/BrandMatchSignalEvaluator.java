package com.sauda.service.matching;

import com.sauda.domain.entity.Lot;
import com.sauda.domain.entity.Offer;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Order(30)
@Component
public class BrandMatchSignalEvaluator implements MatchSignalEvaluator {

    private static final BigDecimal WEIGHT = new BigDecimal("0.20");

    @Override
    public Optional<MatchSignal> evaluate(Lot lot, Offer offer) {
        if (!StringUtils.hasText(offer.getBrand())) {
            return Optional.empty();
        }
        String haystack = LotOfferText.combinedLotText(lot);
        if (!haystack.contains(offer.getBrand().toLowerCase(Locale.ROOT))) {
            return Optional.empty();
        }
        return Optional.of(new MatchSignal("Совпал бренд: " + offer.getBrand(), WEIGHT));
    }
}
