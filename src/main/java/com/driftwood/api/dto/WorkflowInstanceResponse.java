package com.driftwood.api.dto;

import com.driftwood.domain.StepExecution;
import com.driftwood.domain.StepStatus;
import com.driftwood.domain.WorkflowInstance;
import com.driftwood.domain.WorkflowStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkflowInstanceResponse(
        UUID id,
        String workflowId,
        String workflowName,
        WorkflowStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<StepExecutionSummary> steps
) {

    public record StepExecutionSummary(
            UUID id,
            String stepId,
            String stepName,
            StepStatus status,
            int attemptCount,
            Instant startedAt,
            Instant completedAt
    ) {
        static StepExecutionSummary from(StepExecution se) {
            return new StepExecutionSummary(
                    se.getId(),
                    se.getStep().getId(),
                    se.getStep().getName(),
                    se.getStatus(),
                    se.getAttemptCount(),
                    se.getStartedAt(),
                    se.getCompletedAt()
            );
        }
    }

    public static WorkflowInstanceResponse from(WorkflowInstance instance) {
        return new WorkflowInstanceResponse(
                instance.getId(),
                instance.getWorkflow().getId(),
                instance.getWorkflow().getName(),
                instance.getStatus(),
                instance.getCreatedAt(),
                instance.getUpdatedAt(),
                instance.getStepExecutions().stream()
                        .map(StepExecutionSummary::from)
                        .toList()
        );
    }
}
