package com.driftwood.api;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {"driftwood.step.dispatch", "driftwood.step.result"})
class WorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WorkflowInstanceRepository instanceRepository;

    @Autowired
    private StepExecutionRepository stepExecutionRepository;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @AfterEach
    void cleanup() {
        idempotencyKeyRepository.deleteAll();
        stepExecutionRepository.deleteAll();
        instanceRepository.deleteAll();
    }

    @Test
    void createInstance_happyPath_completesAllSteps() throws Exception {
        MvcResult result = mockMvc.perform(post("/workflows/trivial-workflow/instances"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.workflowId").value("trivial-workflow"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();

        UUID instanceId = UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString())
                        .get("id").asText());

        await().atMost(10, SECONDS).untilAsserted(() ->
                mockMvc.perform(get("/workflows/instances/" + instanceId))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("COMPLETED"))
                        .andExpect(jsonPath("$.steps.length()").value(2))
                        .andExpect(jsonPath("$.steps[0].status").value("COMPLETED"))
                        .andExpect(jsonPath("$.steps[1].status").value("COMPLETED"))
        );
    }

    @Test
    void createInstance_duplicateIdempotencyKey_returnsSameInstance() throws Exception {
        String key = "test-idem-key-" + UUID.randomUUID();

        MvcResult first = mockMvc.perform(post("/workflows/trivial-workflow/instances")
                        .header("Idempotency-Key", key))
                .andExpect(status().isAccepted())
                .andReturn();

        String firstId = objectMapper.readTree(first.getResponse().getContentAsString())
                .get("id").asText();

        MvcResult second = mockMvc.perform(post("/workflows/trivial-workflow/instances")
                        .header("Idempotency-Key", key))
                .andExpect(status().isAccepted())
                .andReturn();

        String secondId = objectMapper.readTree(second.getResponse().getContentAsString())
                .get("id").asText();

        assert firstId.equals(secondId) : "Duplicate idempotency key must return same instance";
    }

    @Test
    void createInstance_unknownWorkflow_returns404() throws Exception {
        mockMvc.perform(post("/workflows/nonexistent/instances"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getInstance_unknownInstance_returns404() throws Exception {
        mockMvc.perform(get("/workflows/instances/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
