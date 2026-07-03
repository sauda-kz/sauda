package com.sauda.service.notification.event;

import java.util.UUID;

/**
 * Published when a lot has been sent to a distributor. Carries only the match id so listeners can
 * reload the managed entity within their own transaction (Observer pattern decoupling the sender
 * from any notification side effects).
 */
public record LotSentToDistributorEvent(UUID lotMatchId) {}
