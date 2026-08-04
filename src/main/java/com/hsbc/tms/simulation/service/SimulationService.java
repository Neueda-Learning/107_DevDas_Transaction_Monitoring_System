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

    private static final List<String> ACCOUNTS = List.of("ACC-001", "ACC-002", "ACC-003");
    private static final List<String> PAYEES = List.of("PAYEE-A", "PAYEE-B", "PAYEE-C", "PAYEE-NEW");
    private static final List<TransactionType> TYPES = List.of(TransactionType.DEBIT, TransactionType.CREDIT);

    private final TransactionService transactionService;
    private final Random random = new Random();

    public SimulationService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public List<UUID> generate(int count) {
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            CreateTransactionRequest request = new CreateTransactionRequest();
            request.setAccountId(randomChoice(ACCOUNTS));
            request.setPayeeId(randomChoice(PAYEES));
            request.setAmount(randomAmount());
            request.setCurrency("USD");
            request.setType(randomChoice(TYPES));
            request.setStatus(TransactionStatus.COMPLETED);
            request.setTransactionTime(Instant.now());
            request.setDescription("Simulated transaction " + (i + 1));
            ids.add(transactionService.createTransaction(request).getId());
        }
        return ids;
    }

    private <T> T randomChoice(List<T> values) {
        return values.get(random.nextInt(values.size()));
    }

    private BigDecimal randomAmount() {
        BigDecimal value = BigDecimal.valueOf(10 + (20000 - 10) * random.nextDouble());
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
