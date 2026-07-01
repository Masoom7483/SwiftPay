# SwiftPay — Real-Time Payment Ledger

An event-driven P2P payment platform: a REST gateway accepts payments, a ledger
service applies them atomically over Kafka, and an analytics worker tracks volume.

See **[ARCHITECTURE.md](ARCHITECTURE.md)** for the full design.

## Stack
Java 21 · Spring Boot 3.3 · PostgreSQL · Redis · Apache Kafka (KRaft) ·
Flyway · springdoc/OpenAPI · Docker Compose · GitHub Actions

## Modules
| Module | Description | Port |
|---|---|---|
| `common` | Shared Kafka event records, topic names, Flyway migrations | — |
| `transaction-gateway` | Service A — REST API, Redis idempotency/cache, transactional outbox | 8080 |
| `ledger-service` | Service B — Kafka consumer, atomic transfer, reporting, outcome outbox | 8081 |
| `analytics-worker` | Service C (bonus) — volume analytics consumer | 8082 |

## Run everything (Docker Compose)
```bash
docker compose up --build
```
This starts Postgres, Redis, Kafka and all three services. Two demo accounts
(`acc_1001` with $100k, `acc_2002` with $50k) are seeded automatically.

## Run locally (without Docker)
Start infra (Postgres/Redis/Kafka) however you like, then:
```bash
mvn -pl transaction-gateway -am spring-boot:run
mvn -pl ledger-service -am spring-boot:run
mvn -pl analytics-worker -am spring-boot:run
```

## Try it
```bash
# Initiate a payment
curl -X POST http://localhost:8080/v1/payments \
  -H 'Content-Type: application/json' \
  -d '{
        "transaction_id": "a3f1c2d4-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
        "sender_id": "acc_1001",
        "receiver_id": "acc_2002",
        "amount": 150.75,
        "currency": "USD"
      }'

# Fetch a user's transaction history (from the ledger)
curl http://localhost:8081/v1/users/acc_1001/transactions
```

## Docs & health
- Swagger UI (gateway): http://localhost:8080/swagger-ui.html
- Swagger UI (ledger): http://localhost:8081/swagger-ui.html
- Health: http://localhost:8080/health · http://localhost:8081/health · http://localhost:8082/health

## Build & test
```bash
mvn clean verify        # compile + tests
mvn -DskipTests package # build all jars
```

CI is defined in `.github/workflows/ci.yml`; it runs Maven verification, validates
Docker Compose, and builds all three service images.

## Load test
The k6 script for the required 250 TPS / 1,000,000 transaction run lives in
`load-tests/k6/payments.js`.

```bash
RUN_ID="$(date +%Y%m%d%H%M%S)" \
k6 run --summary-export load-tests/results/summary.json load-tests/k6/payments.js
```

See `load-tests/README.md` for the PCAP capture command and shorter smoke-test
overrides.
