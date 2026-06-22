package com.driftwood.api;

import com.driftwood.api.dto.WorkflowInstanceResponse;
import com.driftwood.service.OrchestratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final OrchestratorService orchestratorService;

    @PostMapping("/{workflowId}/instances")
    public ResponseEntity<WorkflowInstanceResponse> createInstance(
            @PathVariable String workflowId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.accepted().body(orchestratorService.submit(workflowId, idempotencyKey));
    }

    @GetMapping("/instances/{instanceId}")
    public ResponseEntity<WorkflowInstanceResponse> getInstance(@PathVariable UUID instanceId) {
        return ResponseEntity.ok(orchestratorService.getStatus(instanceId));
    }
}
