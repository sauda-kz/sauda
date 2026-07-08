package com.sauda.service.matching;

import com.sauda.domain.entity.Lot;
import com.sauda.domain.entity.Offer;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Order(40)
@Component
public class ModelMatchSignalEvaluator implements MatchSignalEvaluator {

    private static final BigDecimal WEIGHT = new BigDecimal("0.15");

    @Override
    public Optional<MatchSignal> evaluate(Lot lot, Offer offer) {
        if (!StringUtils.hasText(offer.getModelMpn())) {
            return Optional.empty();
        }
        String haystack = LotOfferText.combinedLotText(lot);
        if (!haystack.contains(offer.getModelMpn().toLowerCase(Locale.ROOT))) {
            return Optional.empty();
        }
        return Optional.of(new MatchSignal("Совпала модель: " + offer.getModelMpn(), WEIGHT));
    }
}
