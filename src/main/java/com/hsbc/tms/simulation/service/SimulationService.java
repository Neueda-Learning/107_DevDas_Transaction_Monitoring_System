package com.hsbc.tms.simulation.service;

import com.hsbc.tms.transaction.dto.CreateTransactionRequest;
import com.hsbc.tms.transaction.model.TransactionStatus;
import com.hsbc.tms.transaction.model.TransactionType;
import com.hsbc.tms.transaction.service.TransactionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SimulationService {

    private static final List<String> ACCOUNTS = List.of(
            "ACC-1001", "ACC-1002", "ACC-1003", "ACC-1004", "ACC-1005", "ACC-1006", "ACC-1007");
    private static final List<String> PAYEES = List.of(
            "PAY-200", "PAY-201", "PAY-202", "PAY-203", "PAY-204", "PAY-205", "PAY-206", "PAY-207");
    private static final List<TransactionType> TYPES = List.of(TransactionType.DEBIT, TransactionType.CREDIT);
    private static final List<TransactionStatus> BASE_STATUSES = List.of(
            TransactionStatus.COMPLETED,
            TransactionStatus.COMPLETED,
            TransactionStatus.COMPLETED,
            TransactionStatus.FAILED);

    private final TransactionService transactionService;
    private final Random random = new Random();

    public SimulationService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public List<UUID> generate(int count) {
        List<UUID> ids = new ArrayList<>();

        String burstAccount = randomChoice(ACCOUNTS);
        String burstPayee = "PAY-NEW-" + (100 + random.nextInt(900));
        int burstCount = Math.max(1, count / 4);

        for (int i = 0; i < count; i++) {
            CreateTransactionRequest request = (i < burstCount)
                    ? buildBurstTransaction(i, burstAccount, burstPayee)
                    : buildRandomTransaction(i);
            ids.add(transactionService.createTransaction(request).getId());
        }
        return ids;
    }

    private CreateTransactionRequest buildBurstTransaction(int index, String accountId, String payeeId) {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAccountId(accountId);
        request.setPayeeId(payeeId);
        request.setAmount(randomHighRiskAmount());
        request.setCurrency("USD");
        request.setType(TransactionType.DEBIT);
        request.setStatus(TransactionStatus.COMPLETED);
        // Keep burst transactions in a short time window to exercise velocity rules.
        request.setTransactionTime(Instant.now().minusSeconds(random.nextInt(15 * 60)));
        request.setDescription("Simulation burst transaction " + (index + 1));
        return request;
    }

    private CreateTransactionRequest buildRandomTransaction(int index) {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAccountId(randomChoice(ACCOUNTS));
        request.setPayeeId(randomChoice(PAYEES));
        request.setAmount(randomAmount());
        request.setCurrency("USD");
        request.setType(randomChoice(TYPES));
        request.setStatus(randomChoice(BASE_STATUSES));
        request.setTransactionTime(Instant.now().minusSeconds(random.nextInt(24 * 60 * 60)));
        request.setDescription("Simulated transaction " + (index + 1));
        return request;
    }

    private <T> T randomChoice(List<T> values) {
        return values.get(random.nextInt(values.size()));
    }

    private BigDecimal randomAmount() {
        BigDecimal value = BigDecimal.valueOf(10 + (20000 - 10) * random.nextDouble());
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal randomHighRiskAmount() {
        BigDecimal value = BigDecimal.valueOf(12000 + (40000 - 12000) * random.nextDouble());
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
