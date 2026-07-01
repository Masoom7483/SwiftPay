-- Seed data for local development / demos.
-- Schema itself is created by Hibernate (ddl-auto=update) on first boot; this
-- file only seeds a couple of demo accounts so payments can be applied.
--
-- NOTE: because Hibernate creates tables lazily, we guard the seed with a
-- DO block that creates the accounts table if it doesn't exist yet, so this
-- init script is safe to run before the ledger service has started.

CREATE TABLE IF NOT EXISTS accounts (
    account_id VARCHAR(64) PRIMARY KEY,
    balance    NUMERIC(19, 2) NOT NULL,
    currency   VARCHAR(3)     NOT NULL,
    version    BIGINT         NOT NULL DEFAULT 0
);

INSERT INTO accounts (account_id, balance, currency, version) VALUES
    ('acc_1001', 100000.00, 'USD', 0),
    ('acc_2002',  50000.00, 'USD', 0),
    ('acc_3003',      0.00, 'USD', 0)
ON CONFLICT (account_id) DO NOTHING;
