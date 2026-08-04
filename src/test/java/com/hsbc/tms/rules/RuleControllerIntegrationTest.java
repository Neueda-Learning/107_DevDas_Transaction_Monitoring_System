package com.hsbc.tms.rules;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RuleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateUpdateAndFetchRuleHistory() throws Exception {
        String suffix = String.valueOf(Instant.now().toEpochMilli());
        String createPayload = """
                {
                  "name": "Velocity Rule %s",
                  "type": "VELOCITY",
                  "severity": "HIGH",
                  "active": true,
                  "transactionCountThreshold": 4,
                  "timeWindowMinutes": 10
                }
                """.formatted(suffix);

        MvcResult createResult = mockMvc.perform(post("/api/v1/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.type").value("VELOCITY"))
                .andReturn();

        Long ruleId = extractLongId(createResult.getResponse().getContentAsString());

        String updatePayload = """
                {
                  "name": "Velocity Rule Updated %s",
                  "type": "VELOCITY",
                  "severity": "MEDIUM",
                  "active": true,
                  "transactionCountThreshold": 6,
                  "timeWindowMinutes": 15
                }
                """.formatted(suffix);

        mockMvc.perform(put("/api/v1/rules/{id}", ruleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.severity").value("MEDIUM"))
                .andExpect(jsonPath("$.transactionCountThreshold").value(6));

        mockMvc.perform(get("/api/v1/rules/{id}/history", ruleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("CREATED"))
                .andExpect(jsonPath("$[1].action").value("UPDATED"));
    }

    @Test
    void shouldToggleAndSoftDeleteRule() throws Exception {
        String suffix = String.valueOf(Instant.now().toEpochMilli());
        String createPayload = """
                {
                  "name": "Amount Rule %s",
                  "type": "AMOUNT_THRESHOLD",
                  "severity": "LOW",
                  "active": true,
                  "amountThreshold": 1000.00
                }
                """.formatted(suffix);

        MvcResult createResult = mockMvc.perform(post("/api/v1/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn();

        Long ruleId = extractLongId(createResult.getResponse().getContentAsString());

        mockMvc.perform(patch("/api/v1/rules/{id}/status", ruleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(delete("/api/v1/rules/{id}", ruleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void shouldReturnRuleStats() throws Exception {
        mockMvc.perform(get("/api/v1/rules/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRules").isNumber())
                .andExpect(jsonPath("$.activeRules").isNumber())
                .andExpect(jsonPath("$.inactiveRules").isNumber());
    }

    private Long extractLongId(String responseBody) {
        Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(responseBody);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        throw new IllegalStateException("Could not extract numeric id from response body");
    }
}

