package com.driftwood.service;

import java.time.Duration;

public final class RetryBackoffCalculator {

    private static final Duration BASE = Duration.ofSeconds(2);
    private static final Duration CAP = Duration.ofMinutes(5);

    private RetryBackoffCalculator() {}

    public static Duration delayFor(int attemptCount) {
        long seconds = BASE.toSeconds() * (1L << Math.min(attemptCount, 8));
        return Duration.ofSeconds(Math.min(seconds, CAP.toSeconds()));
    }
}
