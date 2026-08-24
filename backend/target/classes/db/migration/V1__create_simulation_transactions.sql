-- Flyway migration: create minimal simulation_transactions table
CREATE TABLE IF NOT EXISTS simulation_transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    run_id VARCHAR(128),
    ts DATETIME,
    account_id VARCHAR(128),
    event_type VARCHAR(64),
    amount DECIMAL(18,2),
    status VARCHAR(64),
    raw_json TEXT
);
