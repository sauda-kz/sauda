package com.sauda.service.matching;

import com.sauda.domain.entity.Lot;
import com.sauda.domain.entity.Offer;
import java.util.Optional;

/**
 * Strategy for a single lot-offer matching heuristic. Each implementation is independent and
 * contributes at most one {@link MatchSignal}; new heuristics are added by introducing a new bean
 * without touching the aggregating service (Open/Closed).
 */
public interface MatchSignalEvaluator {

    Optional<MatchSignal> evaluate(Lot lot, Offer offer);
}
