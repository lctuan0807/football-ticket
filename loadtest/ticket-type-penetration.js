import http from 'k6/http';
import { check } from 'k6';

// Simulates cache penetration: sustained traffic against ticket-type ids
// that don't exist in Postgres. Since TicketTypeCacheService throws
// ResourceNotFoundException before ever writing to Redis on a miss, these
// ids are NEVER cached, so every single request bypasses the cache and
// hits the DB directly — unlike a stampede, this doesn't self-heal after
// the first request.
//
// Usage:
//   k6 run -e VUS=20 -e DURATION=20s loadtest/ticket-type-penetration.js

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const VUS = parseInt(__ENV.VUS || '20', 10);
const DURATION = __ENV.DURATION || '20s';
// Ids picked from this range are assumed to never exist in the DB.
const MIN_INVALID_ID = parseInt(__ENV.MIN_INVALID_ID || '900000000', 10);
const MAX_INVALID_ID = parseInt(__ENV.MAX_INVALID_ID || '999999999', 10);

export const options = {
  scenarios: {
    penetration: {
      executor: 'constant-vus',
      vus: VUS,
      duration: DURATION,
    },
  },
};

export default function () {
  const id = Math.floor(Math.random() * (MAX_INVALID_ID - MIN_INVALID_ID + 1)) + MIN_INVALID_ID;
  const res = http.get(`${BASE_URL}/api/v1/ticket-types/${id}`);
  check(res, {
    'status is 404': (r) => r.status === 404,
  });
}
