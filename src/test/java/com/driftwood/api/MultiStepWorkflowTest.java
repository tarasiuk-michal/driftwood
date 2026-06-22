package com.driftwood.api;

import com.driftwood.domain.StepStatus;
import com.driftwood.repository.DeadLetterRepository;
import com.driftwood.repository.IdempotencyKeyRepository;
import com.driftwood.repository.StepExecutionRepository;
import com.driftwood.repository.WorkflowInstanceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
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

@SpringBootTest(properties = {
        "driftwood.worker.step-failures.three-step-workflow-step-2=2"
})
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {"driftwood.step.dispatch", "driftwood.step.result"})
class MultiStepWorkflowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WorkflowInstanceRepository instanceRepository;

    @Autowired
    private StepExecutionRepository stepExecutionRepository;

    @Autowired
    private DeadLetterRepository deadLetterRepository;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @AfterEach
    void cleanup() {
        idempotencyKeyRepository.deleteAll();
        deadLetterRepository.deleteAll();
        stepExecutionRepository.deleteAll();
        instanceRepository.deleteAll();
    }

    @Test
    void threeStepWorkflow_step2RetriesTwice_step1And3RunExactlyOnce() throws Exception {
        MvcResult result = mockMvc.perform(post("/workflows/three-step-workflow/instances"))
                .andExpect(status().isAccepted())
                .andReturn();

        UUID instanceId = UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString())
                        .get("id").asText());

        await().atMost(30, SECONDS).untilAsserted(() ->
                mockMvc.perform(get("/workflows/instances/" + instanceId))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("COMPLETED"))
        );

        var executions = stepExecutionRepository.findAll();

        assertThat(executions).hasSize(3);

        var step1 = executions.stream()
                .filter(e -> e.getStep().getId().equals("three-step-workflow-step-1"))
                .toList();
        var step2 = executions.stream()
                .filter(e -> e.getStep().getId().equals("three-step-workflow-step-2"))
                .toList();
        var step3 = executions.stream()
                .filter(e -> e.getStep().getId().equals("three-step-workflow-step-3"))
                .toList();

        assertThat(step1).hasSize(1);
        assertThat(step1.get(0).getStatus()).isEqualTo(StepStatus.COMPLETED);
        assertThat(step1.get(0).getAttemptCount()).isEqualTo(0);

        assertThat(step2).hasSize(1);
        assertThat(step2.get(0).getStatus()).isEqualTo(StepStatus.COMPLETED);
        assertThat(step2.get(0).getAttemptCount()).isEqualTo(2);

        assertThat(step3).hasSize(1);
        assertThat(step3.get(0).getStatus()).isEqualTo(StepStatus.COMPLETED);
        assertThat(step3.get(0).getAttemptCount()).isEqualTo(0);
    }
}
