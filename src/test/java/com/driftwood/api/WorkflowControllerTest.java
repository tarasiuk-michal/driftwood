package com.driftwood.api;

import com.driftwood.repository.StepExecutionRepository;
import com.driftwood.repository.WorkflowInstanceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
    private WorkflowInstanceRepository instanceRepository;

    @Autowired
    private StepExecutionRepository stepExecutionRepository;

    @AfterEach
    void cleanup() {
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
                result.getResponse().getContentAsString()
                        .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1"));

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
