package com.driftwood.repository;

import com.driftwood.domain.StepExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StepExecutionRepository extends JpaRepository<StepExecution, UUID> {

    Optional<StepExecution> findByWorkflowInstanceIdAndStepId(UUID workflowInstanceId, String stepId);
}
