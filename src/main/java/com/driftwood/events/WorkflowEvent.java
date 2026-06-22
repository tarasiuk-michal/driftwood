package com.driftwood.events;

import java.time.Instant;
import java.util.UUID;

public record WorkflowEvent(
        String type,
        UUID instanceId,
        String workflowId,
        String stepId,
        String status,
        Instant timestamp
) {
    public static WorkflowEvent submitted(UUID instanceId, String workflowId) {
        return new WorkflowEvent("SUBMITTED", instanceId, workflowId, null, "RUNNING", Instant.now());
    }

    public static WorkflowEvent stepDispatched(UUID instanceId, String workflowId, String stepId) {
        return new WorkflowEvent("STEP_DISPATCHED", instanceId, workflowId, stepId, "PENDING", Instant.now());
    }

    public static WorkflowEvent stepCompleted(UUID instanceId, String workflowId, String stepId) {
        return new WorkflowEvent("STEP_COMPLETED", instanceId, workflowId, stepId, "COMPLETED", Instant.now());
    }

    public static WorkflowEvent retrying(UUID instanceId, String workflowId, String stepId) {
        return new WorkflowEvent("RETRYING", instanceId, workflowId, stepId, "RETRYING", Instant.now());
    }

    public static WorkflowEvent deadLettered(UUID instanceId, String workflowId, String stepId) {
        return new WorkflowEvent("DEAD_LETTERED", instanceId, workflowId, stepId, "DEAD_LETTERED", Instant.now());
    }

    public static WorkflowEvent workflowCompleted(UUID instanceId, String workflowId) {
        return new WorkflowEvent("WORKFLOW_COMPLETED", instanceId, workflowId, null, "COMPLETED", Instant.now());
    }
}
