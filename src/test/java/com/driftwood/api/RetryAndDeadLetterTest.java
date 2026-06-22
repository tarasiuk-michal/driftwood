package com.driftwood.api;

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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "driftwood.worker.step-failures.flaky-workflow-step-2=2"
})
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {"driftwood.step.dispatch", "driftwood.step.result"})
class RetryAndDeadLetterTest {

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

    @AfterEach
    void cleanup() {
        deadLetterRepository.deleteAll();
        stepExecutionRepository.deleteAll();
        instanceRepository.deleteAll();
    }

    @Test
    void flakeyWorkflow_retriesTwiceThenCompletes() throws Exception {
        MvcResult result = mockMvc.perform(post("/workflows/flaky-workflow/instances"))
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
    }
}
