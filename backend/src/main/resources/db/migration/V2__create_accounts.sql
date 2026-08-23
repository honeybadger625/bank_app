-- Flyway migration: create accounts table
CREATE TABLE IF NOT EXISTS accounts (
    id VARCHAR(64) PRIMARY KEY,
    owner_name VARCHAR(255),
    balance DECIMAL(18,2) DEFAULT 0,
    account_type VARCHAR(64),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
