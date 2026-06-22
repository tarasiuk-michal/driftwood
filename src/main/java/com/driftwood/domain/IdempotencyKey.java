package com.driftwood.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys")
@Getter
@Setter
@NoArgsConstructor
public class IdempotencyKey {

    @Id
    private String idempotencyKey;

    @Column(nullable = false)
    private UUID workflowInstanceId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
