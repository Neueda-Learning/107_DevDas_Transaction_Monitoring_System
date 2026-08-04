package com.hsbc.tms.transaction;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateAndFetchTransaction() throws Exception {
        String payload = """
                {
                  "accountId": "ACC-001",
                  "payeeId": "PAYEE-001",
                  "amount": 1500.25,
                  "currency": "USD",
                  "type": "DEBIT",
                  "status": "COMPLETED",
                  "transactionTime": "%s",
                  "description": "Rent payment"
                }
                """.formatted(Instant.now().toString());

        MvcResult result = mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.accountId").value("ACC-001"))
                .andReturn();

        String transactionId = extractTransactionId(result.getResponse().getContentAsString());

        mockMvc.perform(get("/api/v1/transactions/{id}", transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transactionId))
                .andExpect(jsonPath("$.payeeId").value("PAYEE-001"));
    }

    @Test
    void shouldReturnValidationErrorForInvalidPayload() throws Exception {
        String payload = """
                {
                  "accountId": "",
                  "payeeId": "PAYEE-001",
                  "amount": -20,
                  "currency": "US",
                  "type": "DEBIT",
                  "status": "COMPLETED",
                  "transactionTime": "%s"
                }
                """.formatted(Instant.now().toString());

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void shouldFilterTransactionsByAccountId() throws Exception {
        String payload = """
                {
                  "accountId": "ACC-FILTER",
                  "payeeId": "PAYEE-XYZ",
                  "amount": 320.00,
                  "currency": "USD",
                  "type": "CREDIT",
                  "status": "COMPLETED",
                  "transactionTime": "%s",
                  "description": "Refund"
                }
                """.formatted(Instant.now().toString());

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/transactions")
                        .param("accountId", "ACC-FILTER")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].accountId").value("ACC-FILTER"));
    }

    private String extractTransactionId(String responseBody) {
        Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F-]{36})\"").matcher(responseBody);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalStateException("Could not extract transaction id from response body");
    }
}


