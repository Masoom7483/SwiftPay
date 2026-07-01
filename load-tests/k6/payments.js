import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const runId = __ENV.RUN_ID || `${Date.now()}`;
const amount = __ENV.AMOUNT || '0.01';
const senderId = __ENV.SENDER_ID || 'acc_1001';
const receiverId = __ENV.RECEIVER_ID || 'acc_2002';
const currency = __ENV.CURRENCY || 'USD';

export const options = {
  scenarios: {
    payments_250_tps: {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.RATE || 250),
      timeUnit: '1s',
      duration: __ENV.DURATION || '1h6m40s',
      preAllocatedVUs: Number(__ENV.PREALLOCATED_VUS || 400),
      maxVUs: Number(__ENV.MAX_VUS || 1000),
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

export default function () {
  const sequence = exec.scenario.iterationInTest;
  const payload = JSON.stringify({
    transaction_id: `${runId}-${sequence}`,
    sender_id: senderId,
    receiver_id: receiverId,
    amount,
    currency,
  });

  const response = http.post(`${baseUrl}/v1/payments`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { endpoint: 'POST /v1/payments' },
  });

  check(response, {
    accepted: (r) => r.status === 202,
  });
}
