package com.driftwood.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createInstance_happyPath_completesWorkflow() throws Exception {
        mockMvc.perform(post("/workflows/trivial-workflow/instances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.workflowId").value("trivial-workflow"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.steps.length()").value(2))
                .andExpect(jsonPath("$.steps[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.steps[1].status").value("COMPLETED"));
    }

    @Test
    void createInstance_unknownWorkflow_returns404() throws Exception {
        mockMvc.perform(post("/workflows/nonexistent/instances"))
                .andExpect(status().isNotFound());
    }
}
