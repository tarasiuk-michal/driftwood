package com.driftwood.repository;

import com.driftwood.domain.Step;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StepRepository extends JpaRepository<Step, String> {

    Optional<Step> findByWorkflowIdAndStepOrder(String workflowId, int stepOrder);
}
