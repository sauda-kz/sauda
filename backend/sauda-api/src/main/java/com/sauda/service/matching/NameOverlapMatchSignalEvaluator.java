package com.sauda.service.matching;

import com.sauda.domain.entity.Lot;
import com.sauda.domain.entity.Offer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(20)
@Component
public class NameOverlapMatchSignalEvaluator implements MatchSignalEvaluator {

    private static final BigDecimal MAX_WEIGHT = new BigDecimal("0.30");

    @Override
    public Optional<MatchSignal> evaluate(Lot lot, Offer offer) {
        Set<String> lotTokens =
                LotOfferText.tokenize(lot.getTitle(), lot.getTechnicalRequirements());
        Set<String> offerTokens = LotOfferText.tokenize(offer.getRawName());
        if (lotTokens.isEmpty() || offerTokens.isEmpty()) {
            return Optional.empty();
        }

        long overlap = lotTokens.stream().filter(offerTokens::contains).count();
        if (overlap == 0) {
            return Optional.empty();
        }

        double ratio = (double) overlap / Math.max(lotTokens.size(), offerTokens.size());
        BigDecimal weight =
                MAX_WEIGHT.multiply(BigDecimal.valueOf(ratio)).setScale(4, RoundingMode.HALF_UP);
        return Optional.of(new MatchSignal("Совпадение по названию", weight));
    }
}
