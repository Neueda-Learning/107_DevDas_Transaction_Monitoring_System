package com.hsbc.tms.alerts;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AlertControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void clean() {
        jdbcClient.sql("DELETE FROM alert_transactions").update();
        jdbcClient.sql("DELETE FROM alert_history").update();
        jdbcClient.sql("DELETE FROM alerts").update();
    }

    @Test
    void shouldCreateGetAndReadAlertHistory() throws Exception {
        String suffix = String.valueOf(Instant.now().toEpochMilli());
        String payload = """
                {
                  "ruleName": "Velocity Rule %s",
                  "ruleType": "VELOCITY",
                  "severity": "HIGH",
                  "message": "Velocity threshold exceeded",
                  "operatorId": "analyst-1",
                  "note": "created from queue",
                  "triggeringTransactionIds": []
                }
                """.formatted(suffix);

        MvcResult createResult = mockMvc.perform(post("/api/v1/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn();

        Long alertId = extractLongId(createResult.getResponse().getContentAsString());

        mockMvc.perform(get("/api/v1/alerts/{id}", alertId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(alertId))
                .andExpect(jsonPath("$.ruleType").value("VELOCITY"))
                .andExpect(jsonPath("$.severity").value("HIGH"))
                .andExpect(jsonPath("$.history[0].toStatus").value("OPEN"));

        mockMvc.perform(get("/api/v1/alerts/{id}/history", alertId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].toStatus").value("OPEN"))
                .andExpect(jsonPath("$[0].changedBy").value("analyst-1"));
    }

    @Test
    void shouldFilterByStatusAndActiveOnly() throws Exception {
        Long openAlertId = createAlert("Rule Open", "OPEN filter case");
        Long closedAlertId = createAlert("Rule Closed", "CLOSED filter case");

        mockMvc.perform(patch("/api/v1/alerts/{id}/status", closedAlertId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\",\"operatorId\":\"analyst-2\",\"note\":\"resolved\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mockMvc.perform(get("/api/v1/alerts")
                        .param("status", "CLOSED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(closedAlertId))
                .andExpect(jsonPath("$[0].status").value("CLOSED"));

        mockMvc.perform(get("/api/v1/alerts")
                        .param("activeOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(openAlertId))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    void shouldRejectInvalidStatusTransition() throws Exception {
        Long alertId = createAlert("Transition Rule", "transition case");

        mockMvc.perform(patch("/api/v1/alerts/{id}/status", alertId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\",\"operatorId\":\"analyst\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/alerts/{id}/status", alertId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\",\"operatorId\":\"analyst\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid transition from CLOSED to OPEN"));
    }

    @Test
    void shouldValidateCreateRequest() throws Exception {
        String invalidPayload = """
                {
                  "ruleName": "",
                  "ruleType": "VELOCITY",
                  "severity": "HIGH",
                  "message": "",
                  "operatorId": ""
                }
                """;

        mockMvc.perform(post("/api/v1/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    private Long createAlert(String ruleName, String message) throws Exception {
        String payload = """
                {
                  "ruleName": "%s",
                  "ruleType": "NEW_PAYEE",
                  "severity": "MEDIUM",
                  "message": "%s",
                  "operatorId": "analyst-1",
                  "triggeringTransactionIds": []
                }
                """.formatted(ruleName, message);

        MvcResult result = mockMvc.perform(post("/api/v1/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        return extractLongId(result.getResponse().getContentAsString());
    }

    private Long extractLongId(String responseBody) {
        Matcher matcher = Pattern.compile("\\\"id\\\"\\s*:\\s*(\\d+)").matcher(responseBody);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        throw new IllegalStateException("Could not extract numeric id from response body");
    }
}



