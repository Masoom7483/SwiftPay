CREATE TABLE IF NOT EXISTS outbox_events (
    id              UUID PRIMARY KEY,
    service_name    VARCHAR(64)   NOT NULL,
    aggregate_id    VARCHAR(64)   NOT NULL,
    topic           VARCHAR(128)  NOT NULL,
    event_type      VARCHAR(256)  NOT NULL,
    payload         TEXT          NOT NULL,
    status          VARCHAR(16)   NOT NULL,
    attempts        INTEGER       NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ   NOT NULL,
    last_error      VARCHAR(1024),
    created_at      TIMESTAMPTZ   NOT NULL,
    sent_at         TIMESTAMPTZ,
    version         BIGINT        NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_outbox_ready
    ON outbox_events (service_name, status, next_attempt_at, created_at);

CREATE INDEX IF NOT EXISTS idx_outbox_aggregate
    ON outbox_events (aggregate_id);
