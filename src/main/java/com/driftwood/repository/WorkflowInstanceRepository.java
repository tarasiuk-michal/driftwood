package com.driftwood.repository;

import com.driftwood.domain.WorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, UUID> {
}
