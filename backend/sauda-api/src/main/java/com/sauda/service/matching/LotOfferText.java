package com.sauda.service.matching;

import com.sauda.domain.entity.Lot;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

/** Shared text-normalisation helpers used by the matching heuristics. */
final class LotOfferText {

    private static final Pattern TOKEN_SPLITTER = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final int MIN_TOKEN_LENGTH = 2;

    private LotOfferText() {}

    static Set<String> tokenize(String... parts) {
        Set<String> tokens = new HashSet<>();
        for (String part : parts) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            for (String token : TOKEN_SPLITTER.split(part.toLowerCase(Locale.ROOT))) {
                if (token.length() >= MIN_TOKEN_LENGTH) {
                    tokens.add(token);
                }
            }
        }
        return tokens;
    }

    static String combinedLotText(Lot lot) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(lot.getTitle())) {
            builder.append(lot.getTitle()).append(' ');
        }
        if (StringUtils.hasText(lot.getTechnicalRequirements())) {
            builder.append(lot.getTechnicalRequirements());
        }
        return builder.toString().toLowerCase(Locale.ROOT);
    }
}
