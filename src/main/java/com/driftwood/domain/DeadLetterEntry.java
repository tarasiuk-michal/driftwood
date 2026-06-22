package com.driftwood.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dead_letter_entries")
@Getter
@Setter
@NoArgsConstructor
public class DeadLetterEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_instance_id", nullable = false)
    private WorkflowInstance workflowInstance;

    @Column(nullable = false)
    private String stepId;

    @Column(nullable = false)
    private int finalAttemptCount;

    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
