package com.driftwood.api;

import com.driftwood.domain.StepStatus;
import com.driftwood.domain.WorkflowStatus;
import com.driftwood.repository.DeadLetterRepository;
import com.driftwood.repository.StepExecutionRepository;
import com.driftwood.repository.WorkflowInstanceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Exercises the stepFailureRates (random %) path in WorkerService.shouldSucceed(),
// separate from the stepFailures (forced counter) path tested in RetryAndDeadLetterTest.
@SpringBootTest(properties = {
        "driftwood.worker.step-failure-rates.poison-workflow-step-2=1.0"
})
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {"driftwood.step.dispatch", "driftwood.step.result"})
class StepFailureRateTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private WorkflowInstanceRepository instanceRepository;
    @Autowired private StepExecutionRepository stepExecutionRepository;
    @Autowired private DeadLetterRepository deadLetterRepository;

    @AfterEach
    void cleanup() {
        deadLetterRepository.deleteAll();
        stepExecutionRepository.deleteAll();
        instanceRepository.deleteAll();
    }

    @Test
    void stepFailureRate_100percent_deadLettersWithErrorMessage() throws Exception {
        // poison-workflow step-2 has maxAttempts=3 (V7 migration).
        // rate=1.0 → always fails via the random-rate branch → dead-letters in ~12s.
        MvcResult result = mockMvc.perform(post("/workflows/poison-workflow/instances"))
                .andExpect(status().isAccepted())
                .andReturn();

        UUID instanceId = UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString())
                        .get("id").asText());

        await().atMost(30, SECONDS).untilAsserted(() ->
                mockMvc.perform(get("/workflows/instances/" + instanceId))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("DEAD_LETTERED"))
        );

        var instance = instanceRepository.findById(instanceId).orElseThrow();
        assertThat(instance.getStatus()).isEqualTo(WorkflowStatus.DEAD_LETTERED);

        var entry = deadLetterRepository.findAll().stream().findFirst().orElseThrow();
        assertThat(entry.getErrorMessage()).isNotBlank();
        assertThat(entry.getStepId()).isEqualTo("poison-workflow-step-2");

        var stepExecution = stepExecutionRepository.findAll().stream()
                .filter(e -> e.getStep().getId().equals("poison-workflow-step-2"))
                .findFirst().orElseThrow();
        assertThat(stepExecution.getStatus()).isEqualTo(StepStatus.DEAD_LETTERED);
        assertThat(stepExecution.getAttemptCount()).isEqualTo(2); // 3 attempts exhausted
    }
}
