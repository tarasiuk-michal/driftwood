package com.driftwood.api;

import com.driftwood.api.dto.WorkflowInstanceResponse;
import com.driftwood.service.OrchestratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final OrchestratorService orchestratorService;

    @PostMapping("/{workflowId}/instances")
    public ResponseEntity<WorkflowInstanceResponse> createInstance(@PathVariable String workflowId) {
        return ResponseEntity.ok(orchestratorService.createAndRun(workflowId));
    }
}
