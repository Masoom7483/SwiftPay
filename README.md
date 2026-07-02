# SwiftPay — Real-Time Payment Ledger

An event-driven P2P payment platform: a REST gateway accepts payments, a ledger
service applies them atomically over Kafka, and an analytics worker tracks volume.

See **[ARCHITECTURE.md](ARCHITECTURE.md)** for the full design.

## Stack
Java 21 · Spring Boot 3.3 · PostgreSQL · Redis · Apache Kafka (KRaft) ·
springdoc/OpenAPI · Docker Compose

## Modules
| Module | Description | Port |
|---|---|---|
| `common` | Shared Kafka event records, enums, topic names | — |
| `transaction-gateway` | Service A — REST API, idempotency, event producer | 8080 |
| `ledger-service` | Service B — Kafka consumer, atomic transfer, reporting | 8081 |
| `analytics-worker` | Service C (bonus) — volume analytics consumer | 8082 |

## PCAP Trace

The full PCAP trace generated during the 250 TPS / 1 million transactions load test is available at

https://drive.google.com/drive/folders/1a-SQ8aBfTJQg5BCdjJq3uE9Mkvb2T9-D?usp=sharing

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
        "transactionId": "a3f1c2d4-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
        "senderId": "acc_1001",
        "receiverId": "acc_2002",
        "amount": 150.75,
        "currency": "USD"
      }'

# Fetch a user's transaction history (from the ledger)
curl http://localhost:8081/v1/users/acc_1001/transactions
```

## Docs & health
- Swagger UI (gateway): http://localhost:8080/swagger-ui.html
- Swagger UI (ledger): http://localhost:8081/swagger-ui.html
- Health: http://localhost:8080/health · http://localhost:8081/health

## Build & test
```bash
mvn clean verify        # compile + unit + integration tests (Testcontainers)
mvn -DskipTests package # build all jars
```
