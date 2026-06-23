package com.driftwood.worker;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "driftwood.worker")
public class WorkerProperties {
    private double failureRate = 0.0;
    private Map<String, Integer> stepFailures = new HashMap<>();
    private Map<String, Double> stepFailureRates = new HashMap<>();
}
