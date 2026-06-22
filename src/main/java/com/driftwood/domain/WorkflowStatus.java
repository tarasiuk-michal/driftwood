package com.driftwood.domain;

public enum WorkflowStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    RETRYING,
    DEAD_LETTERED
}
