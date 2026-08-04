package com.hsbc.tms.alerts.service;

import com.hsbc.tms.alerts.dto.AlertHistoryResponse;
import com.hsbc.tms.alerts.dto.AlertResponse;
import com.hsbc.tms.alerts.entity.Alert;
import com.hsbc.tms.alerts.entity.AlertHistory;
import com.hsbc.tms.transaction.dto.TransactionResponse;
import com.hsbc.tms.transaction.model.Transaction;
import java.util.List;

public final class AlertApiMapper {

    private AlertApiMapper() {
    }

    public static AlertHistoryResponse toAlertHistoryResponse(AlertHistory history) {
        return new AlertHistoryResponse(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getNote(),
                history.getChangedBy(),
                history.getCreatedAt());
    }

    public static AlertResponse toAlertResponse(
            Alert alert,
            List<TransactionResponse> triggeringTransactions,
            List<AlertHistoryResponse> history) {
        return new AlertResponse(
                alert.getId(),
                alert.getRuleName(),
                alert.getRuleType(),
                alert.getSeverity(),
                alert.getStatus(),
                alert.getMessage(),
                alert.getCreatedAt(),
                alert.getUpdatedAt(),
                triggeringTransactions,
                history);
    }

    public static TransactionResponse toTransactionResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setAccountId(transaction.getAccountId());
        response.setPayeeId(transaction.getPayeeId());
        response.setAmount(transaction.getAmount());
        response.setCurrency(transaction.getCurrency());
        response.setType(transaction.getType());
        response.setStatus(transaction.getStatus());
        response.setTransactionTime(transaction.getTransactionTime());
        response.setDescription(transaction.getDescription());
        response.setCreatedAt(transaction.getCreatedAt());
        response.setUpdatedAt(transaction.getUpdatedAt());
        return response;
    }
}


