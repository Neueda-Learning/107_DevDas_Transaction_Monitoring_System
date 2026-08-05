package com.hsbc.tms.simulation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SimulationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldGenerateTransactionsAndReturnIds() throws Exception {
        mockMvc.perform(post("/api/v1/simulator/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"count\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5))
                .andExpect(jsonPath("$.createdCount").value(5))
                .andExpect(jsonPath("$.createdTransactionIds").isArray())
                .andExpect(jsonPath("$.createdTransactionIds.length()").value(5));
    }

    @Test
    void shouldRejectInvalidSimulationCount() throws Exception {
        mockMvc.perform(post("/api/v1/simulator/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"count\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }
}

