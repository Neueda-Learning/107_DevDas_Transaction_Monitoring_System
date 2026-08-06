package com.hsbc.tms.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hsbc.tms.alerts.repository.AlertRepository;
import com.hsbc.tms.alerts.service.AlertService;
import com.hsbc.tms.common.dto.PagedResponse;
import com.hsbc.tms.common.exception.BadRequestException;
import com.hsbc.tms.common.exception.ResourceNotFoundException;
import com.hsbc.tms.rules.service.RuleEngineService;
import com.hsbc.tms.transaction.dto.CreateTransactionRequest;
import com.hsbc.tms.transaction.dto.TransactionDecisionRequest;
import com.hsbc.tms.transaction.dto.TransactionFilterRequest;
import com.hsbc.tms.transaction.dto.TransactionResponse;
import com.hsbc.tms.transaction.dto.TransactionRollbackDecisionRequest;
import com.hsbc.tms.transaction.dto.TransactionRollbackRequest;
import com.hsbc.tms.transaction.model.Transaction;
import com.hsbc.tms.transaction.model.TransactionStatus;
import com.hsbc.tms.transaction.model.TransactionType;
import com.hsbc.tms.transaction.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private RuleEngineService ruleEngineService;

    @Mock
    private AlertService alertService;

    @Mock
    private AlertRepository alertRepository;

    private TransactionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TransactionServiceImpl(transactionRepository, ruleEngineService, alertService, alertRepository);
    }

    @Test
    void createTransaction_savesAndReturnsResponse() {
        CreateTransactionRequest request = createRequest(TransactionStatus.PENDING);

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ruleEngineService.evaluate(any(Transaction.class))).thenReturn(false);

        TransactionResponse response = service.createTransaction(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getAccountId()).isEqualTo("ACC-1");
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.PENDING);
        verify(transactionRepository, never()).update(any(Transaction.class));
    }

    @Test
    void createTransaction_setsPendingApprovalWhenRulesViolatedForCompletedStatus() {
        CreateTransactionRequest request = createRequest(TransactionStatus.COMPLETED);

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ruleEngineService.evaluate(any(Transaction.class))).thenReturn(true);
        when(transactionRepository.update(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = service.createTransaction(request);

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.PENDING_APPROVAL);
        assertThat(response.getReviewNote()).isEqualTo("Rule violation detected. Operator approval required.");
        verify(transactionRepository).update(any(Transaction.class));
    }

    @Test
    void getTransactionById_throwsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(transactionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTransactionById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Transaction not found for id: " + id);
    }

    @Test
    void findTransactions_returnsPagedResponse() {
        TransactionFilterRequest filter = new TransactionFilterRequest();

        Transaction t1 = sampleTransaction(TransactionStatus.COMPLETED);
        t1.setAmount(new BigDecimal("100.00"));
        Transaction t2 = sampleTransaction(TransactionStatus.PENDING);
        t2.setId(UUID.randomUUID());
        t2.setAmount(new BigDecimal("200.00"));

        Page<Transaction> page = new PageImpl<>(
                List.of(t1, t2),
                PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "transactionTime")),
                7);

        when(transactionRepository.findByFilter(any(TransactionFilterRequest.class), any())).thenReturn(page);

        PagedResponse<TransactionResponse> response = service.findTransactions(filter, 0, 2, "transactionTime", "desc");

        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getTotalElements()).isEqualTo(7);
        assertThat(response.getTotalPages()).isEqualTo(4);
        assertThat(response.isFirst()).isTrue();
        assertThat(response.isLast()).isFalse();
    }

    @Test
    void findTransactions_throwsWhenAmountRangeInvalid() {
        TransactionFilterRequest filter = new TransactionFilterRequest();
        filter.setMinAmount(new BigDecimal("100.00"));
        filter.setMaxAmount(new BigDecimal("10.00"));

        assertThatThrownBy(() -> service.findTransactions(filter, 0, 10, "amount", "asc"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("minAmount cannot be greater than maxAmount");
    }

    @Test
    void findTransactions_throwsWhenTimeRangeInvalid() {
        TransactionFilterRequest filter = new TransactionFilterRequest();
        filter.setFromTime(Instant.parse("2026-08-06T10:00:00Z"));
        filter.setToTime(Instant.parse("2026-08-06T09:00:00Z"));

        assertThatThrownBy(() -> service.findTransactions(filter, 0, 10, "amount", "asc"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("fromTime cannot be after toTime");
    }

    @Test
    void findTransactions_throwsWhenSortByUnsupported() {
        assertThatThrownBy(() -> service.findTransactions(new TransactionFilterRequest(), 0, 10, "bad", "asc"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Unsupported sortBy field: bad");
    }

    @Test
    void findTransactions_throwsWhenSortDirUnsupported() {
        assertThatThrownBy(() -> service.findTransactions(new TransactionFilterRequest(), 0, 10, "amount", "sideways"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("sortDir must be either 'asc' or 'desc'");
    }

    @Test
    void approve_completesPendingTransactionAndResolvesAlerts() {
        UUID id = UUID.randomUUID();
        Transaction tx = sampleTransaction(TransactionStatus.PENDING_APPROVAL);
        tx.setId(id);

        when(transactionRepository.findById(id)).thenReturn(Optional.of(tx));
        when(transactionRepository.update(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = service.approve(id, new TransactionDecisionRequest("operator-1", "   "));

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(response.getReviewedBy()).isEqualTo("operator-1");
        assertThat(response.getReviewNote()).isEqualTo("Approved by operator");
        verify(alertService).resolveAlertsForTransactionDecision(id, "operator-1", true, "   ");
    }

    @Test
    void approve_throwsWhenStatusNotPendingApproval() {
        UUID id = UUID.randomUUID();
        when(transactionRepository.findById(id)).thenReturn(Optional.of(sampleTransaction(TransactionStatus.COMPLETED)));

        assertThatThrownBy(() -> service.approve(id, new TransactionDecisionRequest("ops", null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only pending transactions can be approved");

        verify(transactionRepository, never()).update(any(Transaction.class));
    }

    @Test
    void reject_marksRejectedAndResolvesAlerts() {
        UUID id = UUID.randomUUID();
        Transaction tx = sampleTransaction(TransactionStatus.PENDING_APPROVAL);
        tx.setId(id);

        when(transactionRepository.findById(id)).thenReturn(Optional.of(tx));
        when(transactionRepository.update(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = service.reject(id, new TransactionDecisionRequest("operator-2", "manual reject"));

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.REJECTED);
        assertThat(response.getReviewNote()).isEqualTo("manual reject");
        verify(alertService).resolveAlertsForTransactionDecision(id, "operator-2", false, "manual reject");
    }

    @Test
    void requestRollback_normalizesInputsAndMarksRequested() {
        UUID id = UUID.randomUUID();
        Transaction tx = sampleTransaction(TransactionStatus.COMPLETED);
        tx.setId(id);

        when(transactionRepository.findById(id)).thenReturn(Optional.of(tx));
        when(transactionRepository.update(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = service.requestRollback(id,
                new TransactionRollbackRequest("  duplicate  ", "  wrong payee  ", "  user-1  ", "   "));

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.ROLLBACK_REQUESTED);
        assertThat(response.getRollbackReasonCode()).isEqualTo("DUPLICATE");
        assertThat(response.getRollbackReasonDetail()).isEqualTo("wrong payee");
        assertThat(response.getRollbackRequestedBy()).isEqualTo("user-1");
        assertThat(response.getRollbackSupportingReference()).isNull();
    }

    @Test
    void requestRollback_throwsWhenStatusNotCompleted() {
        UUID id = UUID.randomUUID();
        when(transactionRepository.findById(id)).thenReturn(Optional.of(sampleTransaction(TransactionStatus.PENDING_APPROVAL)));

        assertThatThrownBy(() -> service.requestRollback(id,
                new TransactionRollbackRequest("R", "D", "U", null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Rollback can only be requested for completed transactions");
    }

    @Test
    void approveRollback_createsRefundAndLinksItToOriginalTransaction() {
        UUID id = UUID.randomUUID();
        Transaction original = sampleTransaction(TransactionStatus.ROLLBACK_REQUESTED);
        original.setId(id);
        original.setAmount(new BigDecimal("120.50"));

        when(transactionRepository.findById(id)).thenReturn(Optional.of(original));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.update(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = service.approveRollback(id, new TransactionRollbackDecisionRequest("  op-7  ", null));

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.REFUNDED);
        assertThat(response.getRollbackReviewedBy()).isEqualTo("op-7");
        assertThat(response.getRollbackReviewNote()).isEqualTo("Rollback approved and refunded");
        assertThat(response.getRefundTransactionId()).isNotNull();

        ArgumentCaptor<Transaction> refundCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(refundCaptor.capture());
        Transaction refund = refundCaptor.getValue();
        assertThat(refund.getAmount()).isEqualByComparingTo("-120.50");
        assertThat(refund.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(refund.getRefundedForTransactionId()).isEqualTo(id);
        assertThat(refund.getDescription()).contains(id.toString());
    }

    @Test
    void rejectRollback_marksRollbackRejectedWithDefaultNote() {
        UUID id = UUID.randomUUID();
        Transaction tx = sampleTransaction(TransactionStatus.ROLLBACK_REQUESTED);
        tx.setId(id);

        when(transactionRepository.findById(id)).thenReturn(Optional.of(tx));
        when(transactionRepository.update(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = service.rejectRollback(id, new TransactionRollbackDecisionRequest(" ops-9 ", ""));

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.ROLLBACK_REJECTED);
        assertThat(response.getRollbackReviewedBy()).isEqualTo("ops-9");
        assertThat(response.getRollbackReviewNote()).isEqualTo("Rollback rejected");
    }

    @Test
    void approveRollback_throwsWhenStatusNotRollbackRequested() {
        UUID id = UUID.randomUUID();
        when(transactionRepository.findById(id)).thenReturn(Optional.of(sampleTransaction(TransactionStatus.COMPLETED)));

        assertThatThrownBy(() -> service.approveRollback(id, new TransactionRollbackDecisionRequest("ops", null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only rollback-requested transactions can be approved for refund");
    }

    private CreateTransactionRequest createRequest(TransactionStatus status) {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAccountId("ACC-1");
        request.setPayeeId("PAYEE-1");
        request.setAmount(new BigDecimal("45.67"));
        request.setCurrency("USD");
        request.setType(TransactionType.DEBIT);
        request.setStatus(status);
        request.setTransactionTime(Instant.parse("2026-08-06T08:00:00Z"));
        request.setDescription("Sample");
        return request;
    }

    private Transaction sampleTransaction(TransactionStatus status) {
        Transaction tx = new Transaction();
        tx.setId(UUID.randomUUID());
        tx.setAccountId("ACC-1");
        tx.setPayeeId("PAYEE-1");
        tx.setAmount(new BigDecimal("45.67"));
        tx.setCurrency("USD");
        tx.setType(TransactionType.DEBIT);
        tx.setStatus(status);
        tx.setTransactionTime(Instant.parse("2026-08-06T08:00:00Z"));
        tx.setDescription("Sample");
        tx.setCreatedAt(Instant.parse("2026-08-06T08:00:00Z"));
        tx.setUpdatedAt(Instant.parse("2026-08-06T08:00:00Z"));
        return tx;
    }
}

