package com.driftwood.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class WorkflowInstanceNotFoundException extends RuntimeException {

    public WorkflowInstanceNotFoundException(UUID instanceId) {
        super("Workflow instance not found: " + instanceId);
    }
}
