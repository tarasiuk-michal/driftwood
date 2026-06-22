package com.driftwood.service;

import com.driftwood.api.dto.WorkflowInstanceResponse;
import com.driftwood.domain.*;
import com.driftwood.exception.WorkflowNotFoundException;
import com.driftwood.repository.StepExecutionRepository;
import com.driftwood.repository.WorkflowInstanceRepository;
import com.driftwood.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrchestratorService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowInstanceRepository instanceRepository;
    private final StepExecutionRepository stepExecutionRepository;

    @Transactional
    public WorkflowInstanceResponse createAndRun(String workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException(workflowId));

        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflow(workflow);
        instance.setStatus(WorkflowStatus.RUNNING);
        instanceRepository.save(instance);

        List<Step> steps = workflow.getSteps();

        for (Step step : steps) {
            StepExecution execution = new StepExecution();
            execution.setWorkflowInstance(instance);
            execution.setStep(step);
            execution.setStatus(StepStatus.RUNNING);
            execution.setStartedAt(Instant.now());
            stepExecutionRepository.save(execution);

            simulateStep();

            execution.setStatus(StepStatus.COMPLETED);
            execution.setCompletedAt(Instant.now());
            stepExecutionRepository.save(execution);

            instance.getStepExecutions().add(execution);
        }

        instance.setStatus(WorkflowStatus.COMPLETED);
        instance.setUpdatedAt(Instant.now());
        instanceRepository.save(instance);

        return WorkflowInstanceResponse.from(instance);
    }

    private void simulateStep() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
