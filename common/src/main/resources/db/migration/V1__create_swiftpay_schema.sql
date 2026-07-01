CREATE TABLE IF NOT EXISTS accounts (
    account_id VARCHAR(64) PRIMARY KEY,
    balance    NUMERIC(19, 2) NOT NULL CHECK (balance >= 0),
    currency   VARCHAR(3)     NOT NULL,
    version    BIGINT         NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS payment_transactions (
    transaction_id VARCHAR(64) PRIMARY KEY,
    sender_id      VARCHAR(64)    NOT NULL,
    receiver_id    VARCHAR(64)    NOT NULL,
    amount         NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    currency       VARCHAR(3)     NOT NULL,
    status         VARCHAR(16)    NOT NULL,
    created_at     TIMESTAMPTZ    NOT NULL,
    updated_at     TIMESTAMPTZ    NOT NULL,
    version        BIGINT         NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_payment_sender
    ON payment_transactions (sender_id);

CREATE INDEX IF NOT EXISTS idx_payment_receiver
    ON payment_transactions (receiver_id);

CREATE TABLE IF NOT EXISTS ledger_entries (
    transaction_id VARCHAR(64) PRIMARY KEY,
    sender_id      VARCHAR(64)    NOT NULL,
    receiver_id    VARCHAR(64)    NOT NULL,
    amount         NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    currency       VARCHAR(3)     NOT NULL,
    status         VARCHAR(16)    NOT NULL,
    failure_reason VARCHAR(128),
    applied_at     TIMESTAMPTZ    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ledger_sender
    ON ledger_entries (sender_id);

CREATE INDEX IF NOT EXISTS idx_ledger_receiver
    ON ledger_entries (receiver_id);

CREATE TABLE IF NOT EXISTS payment_facts (
    transaction_id VARCHAR(64) PRIMARY KEY,
    amount         NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    currency       VARCHAR(3)     NOT NULL,
    completed_at   TIMESTAMPTZ    NOT NULL
);

INSERT INTO accounts (account_id, balance, currency, version) VALUES
    ('acc_1001', 100000.00, 'USD', 0),
    ('acc_2002',  50000.00, 'USD', 0),
    ('acc_3003',      0.00, 'USD', 0)
ON CONFLICT (account_id) DO NOTHING;
