package com.driftwood.worker;

import com.driftwood.messaging.StepDispatchMessage;
import com.driftwood.messaging.StepResultMessage;
import com.driftwood.messaging.Topics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.random.RandomGenerator;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerService {

    private final KafkaTemplate<String, StepResultMessage> kafkaTemplate;

    @Value("${driftwood.worker.failure-rate:0.0}")
    private double failureRate;

    @KafkaListener(topics = Topics.STEP_DISPATCH, groupId = "driftwood-worker")
    public void execute(StepDispatchMessage message) {
        log.info("Worker executing step {} for instance {} (attempt {})",
                message.stepId(), message.instanceId(), message.attemptCount());

        simulateWork();

        boolean success = RandomGenerator.getDefault().nextDouble() >= failureRate;
        String errorMessage = success ? null : "Simulated step failure";

        kafkaTemplate.send(Topics.STEP_RESULT,
                new StepResultMessage(message.instanceId(), message.stepId(), message.attemptCount(), success, errorMessage));

        log.info("Worker published result for step {}: success={}", message.stepId(), success);
    }

    private void simulateWork() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
