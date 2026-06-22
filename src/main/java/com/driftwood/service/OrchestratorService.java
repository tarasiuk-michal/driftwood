package com.driftwood.service;

import com.driftwood.api.dto.WorkflowInstanceResponse;
import com.driftwood.domain.*;
import com.driftwood.exception.WorkflowInstanceNotFoundException;
import com.driftwood.exception.WorkflowNotFoundException;
import com.driftwood.messaging.StepDispatchMessage;
import com.driftwood.messaging.StepResultMessage;
import com.driftwood.messaging.Topics;
import com.driftwood.repository.StepExecutionRepository;
import com.driftwood.repository.StepRepository;
import com.driftwood.repository.WorkflowInstanceRepository;
import com.driftwood.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final KafkaTemplate<String, StepDispatchMessage> kafkaTemplate;

    @Transactional
    public WorkflowInstanceResponse submit(String workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException(workflowId));

        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflow(workflow);
        instance.setStatus(WorkflowStatus.RUNNING);
        instanceRepository.save(instance);

        List<Step> steps = workflow.getSteps();
        if (steps.isEmpty()) {
            instance.setStatus(WorkflowStatus.COMPLETED);
            instance.setUpdatedAt(Instant.now());
            instanceRepository.save(instance);
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

        WorkflowInstance instance = execution.getWorkflowInstance();

        if (message.success()) {
            execution.setStatus(StepStatus.COMPLETED);
            execution.setCompletedAt(Instant.now());
            stepExecutionRepository.save(execution);

            Step completedStep = execution.getStep();
            String workflowId = instance.getWorkflow().getId();

            stepRepository.findByWorkflowIdAndStepOrder(workflowId, completedStep.getStepOrder() + 1)
                    .ifPresentOrElse(
                            nextStep -> dispatchStep(instance, nextStep),
                            () -> {
                                instance.setStatus(WorkflowStatus.COMPLETED);
                                instance.setUpdatedAt(Instant.now());
                                instanceRepository.save(instance);
                                log.info("Workflow instance {} completed", instance.getId());
                            }
                    );
        } else {
            execution.setStatus(StepStatus.FAILED);
            execution.setCompletedAt(Instant.now());
            execution.setErrorMessage(message.errorMessage());
            stepExecutionRepository.save(execution);

            instance.setStatus(WorkflowStatus.FAILED);
            instance.setUpdatedAt(Instant.now());
            instanceRepository.save(instance);
            log.warn("Workflow instance {} failed at step {}", instance.getId(), message.stepId());
        }
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

        log.info("Dispatched step {} for instance {}", step.getId(), instance.getId());
    }
}
