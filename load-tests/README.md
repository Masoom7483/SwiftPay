# SwiftPay Load Test

This k6 scenario matches the hackathon target: 250 TPS for 1,000,000 payment
requests. At 250 requests per second, the run duration is 1h 6m 40s.

## Run

Start SwiftPay first:

```bash
docker compose up --build
```

Run the workload:

```bash
RUN_ID="$(date +%Y%m%d%H%M%S)" \
k6 run --summary-export load-tests/results/summary.json load-tests/k6/payments.js
```

Useful overrides:

```bash
RATE=50 DURATION=2m RUN_ID=smoke k6 run load-tests/k6/payments.js
BASE_URL=http://localhost:8080 AMOUNT=0.01 k6 run load-tests/k6/payments.js
```

## PCAP Capture

Capture traffic during the run:

```bash
sudo tcpdump -i any -w load-tests/results/swiftpay-250tps-1m.pcap \
  'tcp port 8080 or tcp port 8081 or tcp port 29092 or tcp port 5432 or tcp port 6379'
```

Generated PCAP and result files are intentionally ignored by git because they can
be large. Store the final `summary.json` and `.pcap` beside the submission when
running the official benchmark.
