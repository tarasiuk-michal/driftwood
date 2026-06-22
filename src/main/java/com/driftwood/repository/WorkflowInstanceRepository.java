package com.driftwood.repository;

import com.driftwood.domain.WorkflowInstance;
import com.driftwood.domain.WorkflowStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, UUID> {
    long countByStatus(WorkflowStatus status);
    long countByStatusIn(List<WorkflowStatus> statuses);
}
