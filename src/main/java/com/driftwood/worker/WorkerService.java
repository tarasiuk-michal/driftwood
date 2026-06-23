package com.driftwood.worker;

import com.driftwood.messaging.StepDispatchMessage;
import com.driftwood.messaging.StepResultMessage;
import com.driftwood.messaging.Topics;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerService {

    private final KafkaTemplate<String, StepResultMessage> kafkaTemplate;
    private final WorkerProperties workerProperties;
    private final ConcurrentHashMap<String, Integer> remainingFailures = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        remainingFailures.putAll(workerProperties.getStepFailures());
    }

    @KafkaListener(topics = Topics.STEP_DISPATCH, groupId = "driftwood-worker")
    public void execute(StepDispatchMessage message) {
        log.info("Worker executing step {} for instance {} (attempt {})",
                message.stepId(), message.instanceId(), message.attemptCount());

        simulateWork();

        boolean success = shouldSucceed(message.stepId());
        String errorMessage = success ? null : "Simulated step failure";

        kafkaTemplate.send(Topics.STEP_RESULT,
                new StepResultMessage(message.instanceId(), message.stepId(), message.attemptCount(), success, errorMessage));

        log.info("Worker published result for step {}: success={}", message.stepId(), success);
    }

    private boolean shouldSucceed(String stepId) {
        Integer remaining = remainingFailures.get(stepId);
        if (remaining != null && remaining > 0) {
            remainingFailures.put(stepId, remaining - 1);
            return false;
        }
        double rate = workerProperties.getStepFailureRates()
                .getOrDefault(stepId, workerProperties.getFailureRate());
        return ThreadLocalRandom.current().nextDouble() >= rate;
    }

    private void simulateWork() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
