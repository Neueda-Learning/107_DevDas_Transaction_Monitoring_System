package com.hsbc.tms.transaction;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void clean() {
        jdbcClient.sql("DELETE FROM alert_history").update();
        jdbcClient.sql("DELETE FROM alert_transactions").update();
        jdbcClient.sql("DELETE FROM alerts").update();
        jdbcClient.sql("DELETE FROM rule_execution_history").update();
        jdbcClient.sql("DELETE FROM transactions").update();
        jdbcClient.sql("DELETE FROM monitoring_rules").update();
    }

    @Test
    void shouldCreateAndFetchTransaction() throws Exception {
        String payload = """
                {
                  "accountId": "ACC-1001",
                  "payeeId": "PAY-201",
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
                .andExpect(jsonPath("$.accountId").value("ACC-1001"))
                .andReturn();

        String transactionId = extractTransactionId(result.getResponse().getContentAsString());

        mockMvc.perform(get("/api/v1/transactions/{id}", transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transactionId))
                .andExpect(jsonPath("$.payeeId").value("PAY-201"));
    }

    @Test
    void shouldReturnValidationErrorForInvalidPayload() throws Exception {
        String payload = """
                {
                  "accountId": "",
                  "payeeId": "PAY-201",
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
                  "accountId": "ACC-2001",
                  "payeeId": "PAY-202",
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
                        .param("accountId", "ACC-2001")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].accountId").value("ACC-2001"));
    }

    @Test
    void shouldApproveAndRejectPendingTransaction() throws Exception {
        String pendingPayload = """
                {
                  "accountId": "ACC-3001",
                  "payeeId": "PAY-203",
                  "amount": 80.00,
                  "currency": "USD",
                  "type": "DEBIT",
                  "status": "PENDING_APPROVAL",
                  "transactionTime": "%s",
                  "description": "Needs approval"
                }
                """.formatted(Instant.now().toString());

        MvcResult createResult = mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pendingPayload))
                .andExpect(status().isCreated())
                .andReturn();

        String id = extractTransactionId(createResult.getResponse().getContentAsString());

        mockMvc.perform(patch("/api/v1/transactions/{id}/approve", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operatorId": "ops-1",
                                  "note": "   "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.reviewedBy").value("ops-1"))
                .andExpect(jsonPath("$.reviewNote").value("Approved by operator"));

        MvcResult createRejectResult = mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pendingPayload.replace("ACC-3001", "ACC-3002")))
                .andExpect(status().isCreated())
                .andReturn();

        String rejectId = extractTransactionId(createRejectResult.getResponse().getContentAsString());

        mockMvc.perform(patch("/api/v1/transactions/{id}/reject", rejectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operatorId": "ops-2",
                                  "note": "manual reject"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.reviewedBy").value("ops-2"))
                .andExpect(jsonPath("$.reviewNote").value("manual reject"));
    }

    @Test
    void shouldRequestApproveAndRejectRollbackFlow() throws Exception {
        String payload = """
                {
                  "accountId": "ACC-4001",
                  "payeeId": "PAY-204",
                  "amount": 450.00,
                  "currency": "USD",
                  "type": "DEBIT",
                  "status": "COMPLETED",
                  "transactionTime": "%s",
                  "description": "rollback candidate"
                }
                """.formatted(Instant.now().toString());

        MvcResult createResult = mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andReturn();

        String id = extractTransactionId(createResult.getResponse().getContentAsString());

        mockMvc.perform(patch("/api/v1/transactions/{id}/rollback/request", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reasonCode": "  duplicate ",
                                  "reasonDetail": "  sent twice ",
                                  "requestedBy": " user-1 ",
                                  "supportingReference": "  "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ROLLBACK_REQUESTED"))
                .andExpect(jsonPath("$.rollbackReasonCode").value("DUPLICATE"))
                .andExpect(jsonPath("$.rollbackReasonDetail").value("sent twice"))
                .andExpect(jsonPath("$.rollbackRequestedBy").value("user-1"));

        MvcResult approveRollbackResult = mockMvc.perform(patch("/api/v1/transactions/{id}/rollback/approve", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operatorId": "ops-r1",
                                  "note": ""
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"))
                .andExpect(jsonPath("$.rollbackReviewNote").value("Rollback approved and refunded"))
                .andExpect(jsonPath("$.refundTransactionId").isNotEmpty())
                .andReturn();

        String refundTransactionId = extractUuidField(
                approveRollbackResult.getResponse().getContentAsString(),
                "refundTransactionId");

        mockMvc.perform(get("/api/v1/transactions/{id}", refundTransactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Refund for " + id))
                .andExpect(jsonPath("$.amount").value(-450.00));

        MvcResult createSecond = mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload.replace("ACC-4001", "ACC-4002")))
                .andExpect(status().isCreated())
                .andReturn();

        String rejectRollbackId = extractTransactionId(createSecond.getResponse().getContentAsString());

        mockMvc.perform(patch("/api/v1/transactions/{id}/rollback/request", rejectRollbackId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reasonCode": "err",
                                  "reasonDetail": "mistake",
                                  "requestedBy": "user-2",
                                  "supportingReference": "case-22"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/transactions/{id}/rollback/reject", rejectRollbackId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operatorId": "ops-r2",
                                  "note": "   "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ROLLBACK_REJECTED"))
                .andExpect(jsonPath("$.rollbackReviewNote").value("Rollback rejected"));
    }

    @Test
    void shouldReturnErrorsForInvalidRangeAndMissingResource() throws Exception {
        mockMvc.perform(get("/api/v1/transactions")
                        .param("minAmount", "100")
                        .param("maxAmount", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("minAmount cannot be greater than maxAmount"));

        mockMvc.perform(get("/api/v1/transactions/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("Transaction not found for id")));
    }

    @Test
    void shouldReturnValidationErrorForInvalidDecisionPayload() throws Exception {
        String payload = """
                {
                  "accountId": "ACC-5001",
                  "payeeId": "PAY-205",
                  "amount": 50.00,
                  "currency": "USD",
                  "type": "DEBIT",
                  "status": "PENDING_APPROVAL",
                  "transactionTime": "%s"
                }
                """.formatted(Instant.now().toString());

        MvcResult createResult = mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        String id = extractTransactionId(createResult.getResponse().getContentAsString());

        mockMvc.perform(patch("/api/v1/transactions/{id}/approve", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operatorId": "",
                                  "note": "x"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    private String extractTransactionId(String responseBody) {
        Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F-]{36})\"").matcher(responseBody);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalStateException("Could not extract transaction id from response body");
    }

    private String extractUuidField(String responseBody, String fieldName) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"([0-9a-fA-F-]{36})\"").matcher(responseBody);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalStateException("Could not extract field " + fieldName + " from response body");
    }
}


