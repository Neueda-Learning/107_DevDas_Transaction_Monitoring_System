package com.hsbc.tms.transaction.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hsbc.tms.common.dto.PagedResponse;
import com.hsbc.tms.transaction.dto.CreateTransactionRequest;
import com.hsbc.tms.transaction.dto.TransactionDecisionRequest;
import com.hsbc.tms.transaction.dto.TransactionFilterRequest;
import com.hsbc.tms.transaction.dto.TransactionResponse;
import com.hsbc.tms.transaction.dto.TransactionRollbackDecisionRequest;
import com.hsbc.tms.transaction.dto.TransactionRollbackRequest;
import com.hsbc.tms.transaction.model.TransactionStatus;
import com.hsbc.tms.transaction.model.TransactionType;
import com.hsbc.tms.transaction.service.TransactionService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    private TransactionController controller;

    @BeforeEach
    void setUp() {
        controller = new TransactionController(transactionService);
    }

    @Test
    void createTransaction_delegatesToServiceAndReturnsCreated() {
        CreateTransactionRequest request = new CreateTransactionRequest();
        TransactionResponse response = sampleResponse();
        when(transactionService.createTransaction(request)).thenReturn(response);

        var result = controller.createTransaction(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(response);
        verify(transactionService).createTransaction(request);
    }

    @Test
    void getTransactionById_delegatesToService() {
        UUID id = UUID.randomUUID();
        TransactionResponse response = sampleResponse();
        when(transactionService.getTransactionById(id)).thenReturn(response);

        var result = controller.getTransactionById(id);

        assertThat(result.getBody()).isEqualTo(response);
        verify(transactionService).getTransactionById(id);
    }

    @Test
    void findTransactions_mapsRequestParamsToFilterAndDelegates() {
        PagedResponse<TransactionResponse> paged = new PagedResponse<>();
        paged.setContent(List.of(sampleResponse()));
        when(transactionService.findTransactions(org.mockito.ArgumentMatchers.any(TransactionFilterRequest.class), org.mockito.ArgumentMatchers.eq(2), org.mockito.ArgumentMatchers.eq(5), org.mockito.ArgumentMatchers.eq("amount"), org.mockito.ArgumentMatchers.eq("asc")))
                .thenReturn(paged);

        var result = controller.findTransactions(
                "ACC-7",
                "PAYEE-3",
                TransactionStatus.PENDING_APPROVAL,
                TransactionType.DEBIT,
                new BigDecimal("10.00"),
                new BigDecimal("99.00"),
                Instant.parse("2026-08-06T08:00:00Z"),
                Instant.parse("2026-08-06T09:00:00Z"),
                2,
                5,
                "amount",
                "asc");

        assertThat(result.getBody()).isEqualTo(paged);

        ArgumentCaptor<TransactionFilterRequest> filterCaptor = ArgumentCaptor.forClass(TransactionFilterRequest.class);
        verify(transactionService).findTransactions(filterCaptor.capture(), org.mockito.ArgumentMatchers.eq(2), org.mockito.ArgumentMatchers.eq(5), org.mockito.ArgumentMatchers.eq("amount"), org.mockito.ArgumentMatchers.eq("asc"));

        TransactionFilterRequest filter = filterCaptor.getValue();
        assertThat(filter.getAccountId()).isEqualTo("ACC-7");
        assertThat(filter.getPayeeId()).isEqualTo("PAYEE-3");
        assertThat(filter.getStatus()).isEqualTo(TransactionStatus.PENDING_APPROVAL);
        assertThat(filter.getType()).isEqualTo(TransactionType.DEBIT);
        assertThat(filter.getMinAmount()).isEqualByComparingTo("10.00");
        assertThat(filter.getMaxAmount()).isEqualByComparingTo("99.00");
        assertThat(filter.getFromTime()).isEqualTo(Instant.parse("2026-08-06T08:00:00Z"));
        assertThat(filter.getToTime()).isEqualTo(Instant.parse("2026-08-06T09:00:00Z"));
    }

    @Test
    void approve_delegatesToService() {
        UUID id = UUID.randomUUID();
        TransactionDecisionRequest request = new TransactionDecisionRequest("ops-1", "ok");
        TransactionResponse response = sampleResponse();
        when(transactionService.approve(id, request)).thenReturn(response);

        var result = controller.approve(id, request);

        assertThat(result.getBody()).isEqualTo(response);
        verify(transactionService).approve(id, request);
    }

    @Test
    void reject_delegatesToService() {
        UUID id = UUID.randomUUID();
        TransactionDecisionRequest request = new TransactionDecisionRequest("ops-2", "no");
        TransactionResponse response = sampleResponse();
        when(transactionService.reject(id, request)).thenReturn(response);

        var result = controller.reject(id, request);

        assertThat(result.getBody()).isEqualTo(response);
        verify(transactionService).reject(id, request);
    }

    @Test
    void requestRollback_delegatesToService() {
        UUID id = UUID.randomUUID();
        TransactionRollbackRequest request = new TransactionRollbackRequest("CODE", "detail", "user", "ref");
        TransactionResponse response = sampleResponse();
        when(transactionService.requestRollback(id, request)).thenReturn(response);

        var result = controller.requestRollback(id, request);

        assertThat(result.getBody()).isEqualTo(response);
        verify(transactionService).requestRollback(id, request);
    }

    @Test
    void approveRollback_delegatesToService() {
        UUID id = UUID.randomUUID();
        TransactionRollbackDecisionRequest request = new TransactionRollbackDecisionRequest("ops", "approve");
        TransactionResponse response = sampleResponse();
        when(transactionService.approveRollback(id, request)).thenReturn(response);

        var result = controller.approveRollback(id, request);

        assertThat(result.getBody()).isEqualTo(response);
        verify(transactionService).approveRollback(id, request);
    }

    @Test
    void rejectRollback_delegatesToService() {
        UUID id = UUID.randomUUID();
        TransactionRollbackDecisionRequest request = new TransactionRollbackDecisionRequest("ops", "reject");
        TransactionResponse response = sampleResponse();
        when(transactionService.rejectRollback(id, request)).thenReturn(response);

        var result = controller.rejectRollback(id, request);

        assertThat(result.getBody()).isEqualTo(response);
        verify(transactionService).rejectRollback(id, request);
    }

    private TransactionResponse sampleResponse() {
        TransactionResponse response = new TransactionResponse();
        response.setId(UUID.randomUUID());
        response.setAccountId("ACC-1");
        response.setPayeeId("PAYEE-1");
        response.setAmount(new BigDecimal("10.00"));
        response.setCurrency("USD");
        response.setType(TransactionType.DEBIT);
        response.setStatus(TransactionStatus.COMPLETED);
        response.setTransactionTime(Instant.parse("2026-08-06T08:00:00Z"));
        return response;
    }
}

