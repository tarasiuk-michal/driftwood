package com.driftwood.repository;

import com.driftwood.domain.StepExecution;
import com.driftwood.domain.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StepExecutionRepository extends JpaRepository<StepExecution, UUID> {

    Optional<StepExecution> findByWorkflowInstanceIdAndStepId(UUID workflowInstanceId, String stepId);

    @Query("SELECT se FROM StepExecution se JOIN FETCH se.workflowInstance JOIN FETCH se.step WHERE se.status = :status AND se.nextRetryAt <= :now")
    List<StepExecution> findDueForRetry(StepStatus status, Instant now);
}
