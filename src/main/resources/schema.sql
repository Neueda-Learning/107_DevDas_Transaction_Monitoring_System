CREATE TABLE IF NOT EXISTS transactions (
    id CHAR(36) PRIMARY KEY,
    account_id VARCHAR(50) NOT NULL,
    payee_id VARCHAR(50) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    transaction_time TIMESTAMP(6) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    INDEX idx_transactions_account_id (account_id),
    INDEX idx_transactions_payee_id (payee_id),
    INDEX idx_transactions_status (status),
    INDEX idx_transactions_transaction_time (transaction_time)
);

