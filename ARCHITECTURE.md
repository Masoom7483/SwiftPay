# SwiftPay — Architecture Design

> Real-time, event-driven P2P payment ledger. This document is the single
> source of truth for how the system is structured and why.

---

## 1. High-level overview

SwiftPay accepts payments synchronously (fast ACK) but applies them
asynchronously through an event pipeline. This keeps the public API responsive
while the money movement happens reliably in the background, with an audit
trail at every step.

```
                        POST /v1/payments
                              │
                              ▼
        ┌───────────────────────────────────────────┐
        │  Service A — Transaction Gateway (REST)     │
        │  • validate request                         │
        │  • idempotency (Redis SETNX, 24h)           │
        │  • persist PENDING (Postgres)               │
        │  • emit PaymentInitiated                    │
        └───────────────────┬─────────────────────────┘
                            │ Kafka: payment.initiated
                            ▼
        ┌───────────────────────────────────────────┐
        │  Service B — Ledger Service (Consumer)      │
        │  • lock sender & receiver (FOR UPDATE)      │
        │  • debit / credit in ONE db transaction     │
        │  • write immutable LedgerEntry              │
        │  • emit PaymentCompleted / PaymentFailed    │
        │  • GET /v1/users/{id}/transactions          │
        └──────────┬─────────────────────┬────────────┘
                   │ payment.completed    │ payment.failed
                   ▼                      ▼
        ┌────────────────────┐   (gateway updates status)
        │ Service C —        │
        │ Analytics Worker    │
        │ • write PaymentFact │
        │   (OLAP / mock)     │
        └────────────────────┘

Infrastructure: PostgreSQL · Redis · Apache Kafka (KRaft)
```

---

## 2. Services & responsibilities

| Service | Port | Responsibility | Reads/Writes |
|---|---|---|---|
| **transaction-gateway** (A) | 8080 | Public REST API, validation, idempotency, PENDING persistence, event emission | Postgres (`payment_transactions`), Redis, Kafka (produce) |
| **ledger-service** (B) | 8081 | Atomic debit/credit, audit trail, reporting API | Postgres (`accounts`, `ledger_entries`), Kafka (consume + produce) |
| **analytics-worker** (C, bonus) | 8082 | Real-time volume monitoring | Postgres (`payment_facts`) / ClickHouse, Kafka (consume) |
| **common** | — | Shared Kafka event records, enums, topic names | — |

Each service is an independently deployable Spring Boot app; `common` is a plain
library JAR they all depend on so the event contract stays in one place.

---

## 3. Layered structure (per service)

Clean separation of layers — dependencies point inward only:

```
controller / listener   ← entry points (HTTP or Kafka)
        │
     service            ← business logic, transactions, orchestration
        │
  repository            ← Spring Data JPA persistence
        │
     entity             ← JPA domain model
```

`dto` (request/response) and `config` (OpenAPI, Kafka) sit alongside. DTOs never
leak into the persistence layer and entities never leak out of the API — mapping
happens in the service/controller boundary.

---

## 4. Key design decisions

### 4.1 Idempotency — two layers
1. **Redis `SETNX` with 24h TTL** (`IdempotencyService`) — the fast first line;
   the first request for a `transaction_id` wins, repeats get `409 Conflict`.
2. **Postgres primary key** on `transaction_id` — the durable backstop. If Redis
   and the DB ever disagree, a duplicate insert throws and we still return `409`.

On downstream failure the Redis key is **released** so a legitimate retry isn't
wrongly blocked.

### 4.2 Atomic money movement
The ledger performs debit + credit + audit-write inside **one `@Transactional`
method**. Both accounts are fetched with a **pessimistic write lock**
(`SELECT ... FOR UPDATE`), always locking the lower account id first to avoid
deadlocks. If anything fails, the whole transfer rolls back — no partial debits.

### 4.3 Consistency model
- The API is **eventually consistent**: a `202 Accepted` means *accepted*, not
  *settled*. Final state is COMPLETED/FAILED once the ledger processes the event.
- The ledger is the **system of record** for balances; the gateway's
  `payment_transactions` is a request log whose status is updated from events.

### 4.4 Event ordering & keys
All events use `transaction_id` as the Kafka message key, so events for one
payment land on the same partition and are processed in order.

### 4.5 Resilience
- **Consumer retries**: `DefaultErrorHandler` with exponential back-off
  (1s→2s→4s…, ~30s), so a brief DB outage self-heals. Exhausted records go to a
  `<topic>.DLT` dead-letter topic instead of blocking the partition.
- **Producer**: `acks=all` + idempotent producer to avoid lost/duplicate writes.
- **Startup ordering**: compose health checks gate app start on Postgres/Redis/Kafka.

---

## 5. Data model

```
accounts                       payment_transactions (gateway)     ledger_entries (ledger)
──────────                     ─────────────────────────────     ────────────────────────
account_id  (PK)               transaction_id (PK)                transaction_id (PK)
balance                        sender_id                          sender_id
currency                       receiver_id                        receiver_id
version (optimistic lock)      amount                             amount
                               currency                           currency
                               status (PENDING/…)                 status (COMPLETED/FAILED)
                               created_at / updated_at            failure_reason
                               version                            applied_at
```

`transaction_id` is the correlation id threaded through every table and event.

---

## 6. API surface

| Method | Path | Service | Purpose |
|---|---|---|---|
| POST | `/v1/payments` | A | Initiate a payment (→ 202 PENDING) |
| GET | `/v1/users/{userId}/transactions` | B | User transaction history |
| GET | `/health` | all | Health check (Actuator) |
| GET | `/swagger-ui.html` | A, B | Interactive API docs |
| GET | `/v3/api-docs` | A, B | OpenAPI JSON |

Standard HTTP codes: `202` accepted, `400` validation, `404` unknown account,
`409` duplicate, `422` business rejection such as insufficient funds or
currency mismatch, `500` unexpected. All errors use the shared `ErrorResponse`
envelope.

---

## 7. Technology choices

| Concern | Choice | Why |
|---|---|---|
| Framework | Spring Boot 3.3 (Java 21) | Mature ecosystem, records, virtual-thread ready |
| DB | PostgreSQL 16 | ACID transactions for money movement |
| Cache | Redis 7 | Atomic `SETNX` idempotency, balance cache |
| Messaging | Kafka 3.8 (KRaft, no ZooKeeper) | Durable, partitioned, ordered event log |
| Docs | springdoc-openapi 2.6 | Auto-generated Swagger UI from annotations |
| Build | Maven multi-module | One reactor, shared `common` contract |
| Tests | JUnit 5 + Testcontainers | Real Postgres/Kafka in integration tests |

---

## 8. Future hardening (beyond hackathon scope)

- **Transactional outbox** in the gateway so the DB write and event publish are
  truly atomic (today there's a small window if the broker is down mid-commit).
- **Schema migrations** via Flyway/Liquibase instead of `ddl-auto=update`.
- **Saga / compensation** for multi-leg transfers.
- **ClickHouse** for Service C instead of the Postgres mock table.
- **Rate limiting & authN/Z** on the public API.
