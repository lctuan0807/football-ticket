#!/usr/bin/env bash
# Simulates a cache stampede against GET /api/v1/matches/{matchId}/ticket-types/{id}:
#   1. Deletes the Redis key so the next requests find a cold cache
#      (like right after the 10-minute TTL expires).
#   2. Fires VUS concurrent requests at that id via k6.
#   3. Counts cache-miss log lines in LOG_FILE to show how many requests
#      independently fell through to Postgres, instead of just one.
#
# Prerequisite: run the app with stdout captured, e.g.:
#   mvn spring-boot:run | tee /tmp/footballticket-app.log
#
# Usage:
#   TICKET_TYPE_ID=3 MATCH_ID=1 ./loadtest/run-stampede-test.sh
#   TICKET_TYPE_ID=3 MATCH_ID=1 VUS=200 ./loadtest/run-stampede-test.sh

set -euo pipefail

: "${TICKET_TYPE_ID:?Set TICKET_TYPE_ID to an existing ticket type id}"
BASE_URL="${BASE_URL:-http://localhost:8080}"
MATCH_ID="${MATCH_ID:-1}"
VUS="${VUS:-50}"
REDIS_HOST="${REDIS_HOST:-localhost}"
REDIS_PORT="${REDIS_PORT:-6389}"
LOG_FILE="${LOG_FILE:-/tmp/footballticket-app.log}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

command -v redis-cli >/dev/null || { echo "redis-cli not found on PATH" >&2; exit 1; }
command -v k6 >/dev/null || { echo "k6 not found on PATH (brew install k6)" >&2; exit 1; }
[ -f "$LOG_FILE" ] || { echo "LOG_FILE '$LOG_FILE' does not exist. Run the app with: mvn spring-boot:run | tee $LOG_FILE" >&2; exit 1; }

echo "Flushing ticketType:$TICKET_TYPE_ID from Redis ($REDIS_HOST:$REDIS_PORT) to force a cold cache..."
redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" DEL "ticketType:$TICKET_TYPE_ID" >/dev/null

BASELINE_LINES=$(wc -l < "$LOG_FILE")

echo "Firing $VUS concurrent requests at $BASE_URL/api/v1/matches/$MATCH_ID/ticket-types/$TICKET_TYPE_ID..."
k6 run \
  -e BASE_URL="$BASE_URL" \
  -e MATCH_ID="$MATCH_ID" \
  -e TICKET_TYPE_ID="$TICKET_TYPE_ID" \
  -e VUS="$VUS" \
  "$SCRIPT_DIR/ticket-type-stampede.js"

MISS_COUNT=$(tail -n "+$((BASELINE_LINES + 1))" "$LOG_FILE" | grep -c "Ticket type not found in cache: $TICKET_TYPE_ID" || true)

echo ""
echo "Stampede check: $MISS_COUNT/$VUS concurrent requests missed the cache and hit Postgres directly for ticketType:$TICKET_TYPE_ID (expected 1 with proper stampede protection)."
