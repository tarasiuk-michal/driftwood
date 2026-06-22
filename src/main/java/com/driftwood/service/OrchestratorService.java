package com.driftwood.service;

import com.driftwood.api.dto.WorkflowInstanceResponse;
import com.driftwood.domain.*;
import com.driftwood.events.WorkflowEvent;
import com.driftwood.exception.WorkflowInstanceNotFoundException;
import com.driftwood.exception.WorkflowNotFoundException;
import com.driftwood.messaging.StepDispatchMessage;
import com.driftwood.messaging.StepResultMessage;
import com.driftwood.messaging.Topics;
import com.driftwood.repository.DeadLetterRepository;
import com.driftwood.repository.IdempotencyKeyRepository;
import com.driftwood.repository.StepExecutionRepository;
import com.driftwood.repository.StepRepository;
import com.driftwood.repository.WorkflowInstanceRepository;
import com.driftwood.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrchestratorService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowInstanceRepository instanceRepository;
    private final StepExecutionRepository stepExecutionRepository;
    private final StepRepository stepRepository;
    private final DeadLetterRepository deadLetterRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final KafkaTemplate<String, StepDispatchMessage> kafkaTemplate;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public WorkflowInstanceResponse submit(String workflowId, String idempotencyKey) {
        if (idempotencyKey != null) {
            IdempotencyKey existing = idempotencyKeyRepository.findById(idempotencyKey).orElse(null);
            if (existing != null) {
                log.info("Duplicate idempotency key {}, returning existing instance {}", idempotencyKey, existing.getWorkflowInstanceId());
                return instanceRepository.findById(existing.getWorkflowInstanceId())
                        .map(WorkflowInstanceResponse::from)
                        .orElseThrow(() -> new WorkflowInstanceNotFoundException(existing.getWorkflowInstanceId()));
            }
        }

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException(workflowId));

        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflow(workflow);
        instance.setStatus(WorkflowStatus.RUNNING);
        instanceRepository.save(instance);

        if (idempotencyKey != null) {
            IdempotencyKey key = new IdempotencyKey();
            key.setIdempotencyKey(idempotencyKey);
            key.setWorkflowInstanceId(instance.getId());
            idempotencyKeyRepository.save(key);
        }

        eventPublisher.publishEvent(WorkflowEvent.submitted(instance.getId(), workflowId));

        List<Step> steps = workflow.getSteps();
        if (steps.isEmpty()) {
            instance.setStatus(WorkflowStatus.COMPLETED);
            instance.setUpdatedAt(Instant.now());
            instanceRepository.save(instance);
            eventPublisher.publishEvent(WorkflowEvent.workflowCompleted(instance.getId(), workflowId));
            return WorkflowInstanceResponse.from(instance);
        }

        Step firstStep = steps.stream()
                .min(Comparator.comparingInt(Step::getStepOrder))
                .orElseThrow();

        dispatchStep(instance, firstStep);

        return WorkflowInstanceResponse.from(instance);
    }

    @Transactional(readOnly = true)
    public WorkflowInstanceResponse getStatus(UUID instanceId) {
        WorkflowInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new WorkflowInstanceNotFoundException(instanceId));
        return WorkflowInstanceResponse.from(instance);
    }

    @KafkaListener(topics = Topics.STEP_RESULT, groupId = "driftwood-orchestrator")
    @Transactional
    public void handleStepResult(StepResultMessage message) {
        log.info("Orchestrator received result for step {} instance {}: success={}",
                message.stepId(), message.instanceId(), message.success());

        StepExecution execution = stepExecutionRepository
                .findByWorkflowInstanceIdAndStepId(message.instanceId(), message.stepId())
                .orElseThrow(() -> new IllegalStateException(
                        "No StepExecution found for instance=" + message.instanceId() + " step=" + message.stepId()));

        if (execution.getStatus() == StepStatus.COMPLETED || execution.getStatus() == StepStatus.DEAD_LETTERED) {
            log.info("Duplicate result for step {} instance {} (status={}), skipping",
                    message.stepId(), message.instanceId(), execution.getStatus());
            return;
        }

        WorkflowInstance instance = execution.getWorkflowInstance();
        String workflowId = instance.getWorkflow().getId();

        if (message.success()) {
            execution.setStatus(StepStatus.COMPLETED);
            execution.setCompletedAt(Instant.now());
            stepExecutionRepository.save(execution);

            eventPublisher.publishEvent(WorkflowEvent.stepCompleted(instance.getId(), workflowId, message.stepId()));

            Step completedStep = execution.getStep();

            stepRepository.findByWorkflowIdAndStepOrder(workflowId, completedStep.getStepOrder() + 1)
                    .ifPresentOrElse(
                            nextStep -> dispatchStep(instance, nextStep),
                            () -> {
                                instance.setStatus(WorkflowStatus.COMPLETED);
                                instance.setUpdatedAt(Instant.now());
                                instanceRepository.save(instance);
                                eventPublisher.publishEvent(WorkflowEvent.workflowCompleted(instance.getId(), workflowId));
                                log.info("Workflow instance {} completed", instance.getId());
                            }
                    );
        } else {
            int nextAttempt = execution.getAttemptCount() + 1;

            if (nextAttempt >= execution.getMaxAttempts()) {
                deadLetter(execution, instance, message.errorMessage());
            } else {
                execution.setAttemptCount(nextAttempt);
                execution.setStatus(StepStatus.RETRYING);
                execution.setErrorMessage(message.errorMessage());
                execution.setNextRetryAt(Instant.now().plus(RetryBackoffCalculator.delayFor(nextAttempt)));
                stepExecutionRepository.save(execution);

                instance.setStatus(WorkflowStatus.RETRYING);
                instance.setUpdatedAt(Instant.now());
                instanceRepository.save(instance);

                eventPublisher.publishEvent(WorkflowEvent.retrying(instance.getId(), workflowId, message.stepId()));

                log.warn("Step {} failed for instance {}, scheduled retry attempt {} at {}",
                        message.stepId(), message.instanceId(), nextAttempt, execution.getNextRetryAt());
            }
        }
    }

    private void deadLetter(StepExecution execution, WorkflowInstance instance, String errorMessage) {
        execution.setStatus(StepStatus.DEAD_LETTERED);
        execution.setCompletedAt(Instant.now());
        execution.setErrorMessage(errorMessage);
        stepExecutionRepository.save(execution);

        DeadLetterEntry entry = new DeadLetterEntry();
        entry.setWorkflowInstance(instance);
        entry.setStepId(execution.getStep().getId());
        entry.setFinalAttemptCount(execution.getAttemptCount());
        entry.setErrorMessage(errorMessage);
        deadLetterRepository.save(entry);

        instance.setStatus(WorkflowStatus.DEAD_LETTERED);
        instance.setUpdatedAt(Instant.now());
        instanceRepository.save(instance);

        eventPublisher.publishEvent(WorkflowEvent.deadLettered(instance.getId(), instance.getWorkflow().getId(), execution.getStep().getId()));

        log.error("Step {} dead-lettered for instance {} after {} attempts",
                execution.getStep().getId(), instance.getId(), execution.getAttemptCount());
    }

    private void dispatchStep(WorkflowInstance instance, Step step) {
        StepExecution execution = new StepExecution();
        execution.setWorkflowInstance(instance);
        execution.setStep(step);
        execution.setStatus(StepStatus.PENDING);
        execution.setStartedAt(Instant.now());
        stepExecutionRepository.save(execution);

        instance.getStepExecutions().add(execution);

        kafkaTemplate.send(Topics.STEP_DISPATCH,
                new StepDispatchMessage(instance.getId(), step.getId(), execution.getAttemptCount()));

        eventPublisher.publishEvent(WorkflowEvent.stepDispatched(instance.getId(), instance.getWorkflow().getId(), step.getId()));

        log.info("Dispatched step {} for instance {}", step.getId(), instance.getId());
    }
}
