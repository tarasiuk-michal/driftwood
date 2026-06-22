package com.driftwood.api.dto;

public record MetricsSummary(
        long inFlight,
        long completed,
        long deadLettered,
        long retrying,
        double avgStepLatencyMs
) {}
