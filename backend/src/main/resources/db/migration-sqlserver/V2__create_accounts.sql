CREATE TABLE accounts (
    id VARCHAR(64) PRIMARY KEY,
    owner_name VARCHAR(255),
    balance DECIMAL(18,2) DEFAULT 0,
    account_type VARCHAR(64),
    created_at DATETIME2 DEFAULT SYSDATETIME()
);
