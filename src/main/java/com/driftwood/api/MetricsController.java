package com.driftwood.api;

import com.driftwood.api.dto.MetricsSummary;
import com.driftwood.domain.StepExecution;
import com.driftwood.domain.StepStatus;
import com.driftwood.domain.WorkflowStatus;
import com.driftwood.repository.StepExecutionRepository;
import com.driftwood.repository.WorkflowInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final WorkflowInstanceRepository instanceRepository;
    private final StepExecutionRepository stepExecutionRepository;

    @GetMapping("/summary")
    @Transactional(readOnly = true)
    public MetricsSummary summary() {
        long inFlight    = instanceRepository.countByStatusIn(List.of(WorkflowStatus.RUNNING, WorkflowStatus.RETRYING));
        long completed   = instanceRepository.countByStatus(WorkflowStatus.COMPLETED);
        long deadLettered = instanceRepository.countByStatus(WorkflowStatus.DEAD_LETTERED);
        long retrying    = stepExecutionRepository.countByStatus(StepStatus.RETRYING);

        List<StepExecution> done = stepExecutionRepository.findByStatus(StepStatus.COMPLETED);
        double avgLatencyMs = done.stream()
                .filter(se -> se.getStartedAt() != null && se.getCompletedAt() != null)
                .mapToLong(se -> se.getCompletedAt().toEpochMilli() - se.getStartedAt().toEpochMilli())
                .average()
                .orElse(0.0);

        return new MetricsSummary(inFlight, completed, deadLettered, retrying, avgLatencyMs);
    }
}
