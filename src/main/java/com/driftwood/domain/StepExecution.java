package com.driftwood.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "step_executions")
@Getter
@Setter
@NoArgsConstructor
public class StepExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_instance_id", nullable = false)
    private WorkflowInstance workflowInstance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id", nullable = false)
    private Step step;

    @Column(nullable = false)
    private int attemptCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StepStatus status = StepStatus.PENDING;

    private Instant startedAt;
    private Instant completedAt;
    private String errorMessage;
    private Instant nextRetryAt;

    @Column(nullable = false)
    private int maxAttempts = 3;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
