package com.driftwood.service;

import com.driftwood.domain.StepExecution;
import com.driftwood.domain.StepStatus;
import com.driftwood.domain.WorkflowStatus;
import com.driftwood.messaging.StepDispatchMessage;
import com.driftwood.messaging.Topics;
import com.driftwood.repository.StepExecutionRepository;
import com.driftwood.repository.WorkflowInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetryPoller {

    private final StepExecutionRepository stepExecutionRepository;
    private final WorkflowInstanceRepository instanceRepository;
    private final KafkaTemplate<String, StepDispatchMessage> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void pollAndRedispatch() {
        List<StepExecution> due = stepExecutionRepository.findDueForRetry(StepStatus.RETRYING, Instant.now());
        for (StepExecution execution : due) {
            execution.setStatus(StepStatus.PENDING);
            execution.setNextRetryAt(null);
            stepExecutionRepository.save(execution);

            var instance = execution.getWorkflowInstance();
            instance.setStatus(WorkflowStatus.RUNNING);
            instance.setUpdatedAt(java.time.Instant.now());
            instanceRepository.save(instance);

            kafkaTemplate.send(Topics.STEP_DISPATCH,
                    new StepDispatchMessage(
                            execution.getWorkflowInstance().getId(),
                            execution.getStep().getId(),
                            execution.getAttemptCount()));

            log.info("Retrying step {} for instance {} (attempt {})",
                    execution.getStep().getId(),
                    execution.getWorkflowInstance().getId(),
                    execution.getAttemptCount());
        }
    }
}
