package com.driftwood.api;

import com.driftwood.repository.DeadLetterRepository;
import com.driftwood.repository.IdempotencyKeyRepository;
import com.driftwood.repository.StepExecutionRepository;
import com.driftwood.repository.WorkflowInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DeadLetterRepository deadLetterRepository;
    private final StepExecutionRepository stepExecutionRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final WorkflowInstanceRepository instanceRepository;

    // FK order: dead_letter_entries → step_executions → idempotency_keys → workflow_instances
    @PostMapping("/reset")
    @Transactional
    public ResponseEntity<Void> reset() {
        deadLetterRepository.deleteAllInBatch();
        stepExecutionRepository.deleteAllInBatch();
        idempotencyKeyRepository.deleteAllInBatch();
        instanceRepository.deleteAllInBatch();
        return ResponseEntity.noContent().build();
    }
}
