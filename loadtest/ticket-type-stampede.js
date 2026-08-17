import http from 'k6/http';
import { check } from 'k6';

// Fires VUS concurrent requests at the same ticket-type id, all at once
// (shared-iterations, one iteration per VU), to simulate a cache-stampede
// burst against a cold `ticketType:{id}` Redis key.
//
// Usage:
//   k6 run -e TICKET_TYPE_ID=3 -e VUS=50 loadtest/ticket-type-stampede.js
//
// Point TICKET_TYPE_ID at a non-existent id to instead demonstrate cache
// penetration (every request bypasses the cache and hits Postgres).

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TICKET_TYPE_ID = __ENV.TICKET_TYPE_ID || '1';
const VUS = parseInt(__ENV.VUS || '50', 10);

export const options = {
  scenarios: {
    stampede: {
      executor: 'shared-iterations',
      vus: VUS,
      iterations: VUS,
      maxDuration: '30s',
    },
  },
};

export default function () {
  const res = http.get(`${BASE_URL}/api/v1/ticket-types/${TICKET_TYPE_ID}`);
  check(res, {
    'status is 200 or 404': (r) => r.status === 200 || r.status === 404,
  });
}
