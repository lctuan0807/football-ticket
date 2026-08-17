#!/usr/bin/env bash
# Simulates cache penetration against GET /api/v1/ticket-types/{id}:
# sustained traffic against ids that never exist, so they never get
# cached and every request round-trips to Postgres. No Redis flush is
# needed here (there's nothing to flush) — that's the point: this never
# self-heals like a stampede does.
#
# Prerequisite: run the app with stdout captured, e.g.:
#   mvn spring-boot:run | tee /tmp/footballticket-app.log
#
# Usage:
#   ./loadtest/run-penetration-test.sh
#   VUS=50 DURATION=30s ./loadtest/run-penetration-test.sh

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
VUS="${VUS:-20}"
DURATION="${DURATION:-20s}"
LOG_FILE="${LOG_FILE:-/tmp/footballticket-app.log}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

command -v k6 >/dev/null || { echo "k6 not found on PATH (brew install k6)" >&2; exit 1; }
[ -f "$LOG_FILE" ] || { echo "LOG_FILE '$LOG_FILE' does not exist. Run the app with: mvn spring-boot:run | tee $LOG_FILE" >&2; exit 1; }

BASELINE_LINES=$(wc -l < "$LOG_FILE")

echo "Running $VUS VUs for $DURATION against random non-existent ticket-type ids..."
k6 run \
  -e BASE_URL="$BASE_URL" \
  -e VUS="$VUS" \
  -e DURATION="$DURATION" \
  "$SCRIPT_DIR/ticket-type-penetration.js"

MISS_COUNT=$(tail -n "+$((BASELINE_LINES + 1))" "$LOG_FILE" | grep -c "Ticket type not found in cache:" || true)

echo ""
echo "Penetration check: $MISS_COUNT requests hit Postgres directly (cache never absorbed any of this traffic, since misses on non-existent ids are never cached)."
echo "Compare with the k6 'http_reqs' count above — they should be roughly equal, confirming a 0% cache-hit rate."
