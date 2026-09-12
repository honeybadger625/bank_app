CREATE TABLE simulation_transactions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    run_id VARCHAR(128),
    ts DATETIME2,
    account_id VARCHAR(128),
    event_type VARCHAR(64),
    amount DECIMAL(18,2),
    status VARCHAR(64),
    raw_json NVARCHAR(MAX)
);
