package com.maniba.eventledger.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void applyCreditTransactionReturnsUpdatedBalance() throws Exception {
        String accountId = "acct-credit";
        String traceId = "trace-credit-123";

        Map<String, Object> request = transactionRequest("evt-credit-1", "CREDIT", new BigDecimal("100.00"), "USD", Instant.parse("2025-01-01T10:00:00Z"));

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Trace-Id", traceId)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(accountId))
                .andExpect(jsonPath("$.balance").value(100.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(header().string("X-Trace-Id", traceId));
    }

    @Test
    void applyDebitTransactionReturnsUpdatedBalanceAfterCredit() throws Exception {
        String accountId = "acct-debit";

        Map<String, Object> creditRequest = transactionRequest("evt-debit-1", "CREDIT", new BigDecimal("200.00"), "USD", Instant.parse("2025-01-01T09:00:00Z"));
        Map<String, Object> debitRequest = transactionRequest("evt-debit-2", "DEBIT", new BigDecimal("50.00"), "USD", Instant.parse("2025-01-01T10:00:00Z"));

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creditRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(200.00));

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(debitRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(150.00));
    }

    @Test
    void duplicateEventIdIsIgnoredAndDoesNotCreateAdditionalTransaction() throws Exception {
        String accountId = "acct-duplicate";
        String eventId = "evt-duplicate-1";

        Map<String, Object> request = transactionRequest(eventId, "CREDIT", new BigDecimal("75.00"), "USD", Instant.parse("2025-01-01T08:00:00Z"));

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(75.00));

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(75.00));

        mockMvc.perform(get("/accounts/{accountId}", accountId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.transactions[0].eventId").value(eventId));
    }

    @Test
    void accountDetailsTransactionsAreSortedByEventTimestamp() throws Exception {
        String accountId = "acct-sorted";

        Map<String, Object> laterTransaction = transactionRequest("evt-later", "CREDIT", new BigDecimal("20.00"), "USD", Instant.parse("2025-01-01T10:00:00Z"));
        Map<String, Object> earlierTransaction = transactionRequest("evt-earlier", "CREDIT", new BigDecimal("10.00"), "USD", Instant.parse("2025-01-01T09:00:00Z"));

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(laterTransaction)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(earlierTransaction)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/accounts/{accountId}", accountId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions[0].eventId").value("evt-earlier"))
                .andExpect(jsonPath("$.transactions[1].eventId").value("evt-later"));
    }

    @Test
    void invalidAmountResultsInBadRequest() throws Exception {
        String accountId = "acct-invalid-amount";

        Map<String, Object> request = transactionRequest("evt-invalid-amount", "CREDIT", new BigDecimal("0.00"), "USD", Instant.parse("2025-01-01T11:00:00Z"));

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Amount must be greater than zero"));
    }

    @Test
    void invalidTypeResultsInBadRequest() throws Exception {
        String accountId = "acct-invalid-type";

        Map<String, Object> request = transactionRequest("evt-invalid-type", "TRANSFER", new BigDecimal("10.00"), "USD", Instant.parse("2025-01-01T11:00:00Z"));

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("Invalid transaction type")));
    }

    @Test
    void missingRequiredFieldsResultsInValidationError() throws Exception {
        String accountId = "acct-missing-fields";

        Map<String, Object> invalidRequest = new HashMap<>();
        invalidRequest.put("type", "CREDIT");
        invalidRequest.put("currency", "USD");

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message", Matchers.anyOf(
                        Matchers.containsString("must not be blank"),
                        Matchers.containsString("must not be null"))));
    }

    @Test
    void missingAccountReturnsNotFoundForBalanceEndpoint() throws Exception {
        mockMvc.perform(get("/accounts/{accountId}/balance", "acct-missing-account")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void requestTraceIdIsReturnedInResponseHeader() throws Exception {
        String accountId = "acct-trace";
        String traceId = "trace-header-xyz";

        Map<String, Object> request = transactionRequest("evt-trace-1", "CREDIT", new BigDecimal("30.00"), "USD", Instant.parse("2025-01-01T08:30:00Z"));

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-Trace-Id", traceId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", traceId));
    }

    private Map<String, Object> transactionRequest(String eventId, String type, BigDecimal amount, String currency, Instant eventTimestamp) {
        Map<String, Object> request = new HashMap<>();
        request.put("eventId", eventId);
        request.put("type", type);
        request.put("amount", amount);
        request.put("currency", currency);
        request.put("eventTimestamp", eventTimestamp.toString());
        return request;
    }
}
