package com.sauda.service.matching;

import java.math.BigDecimal;

/**
 * A single positive matching signal contributing a human-readable reason and a weight towards the
 * overall confidence score of a lot-offer pair.
 */
public record MatchSignal(String reason, BigDecimal weight) {}
