package com.driftwood.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "steps")
@Getter
@Setter
@NoArgsConstructor
public class Step {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int stepOrder;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
