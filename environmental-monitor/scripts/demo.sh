#!/usr/bin/env bash
set -euo pipefail

BASE="${1:-http://localhost:8000}"

echo "Clear simulator"
curl -sS -X POST "$BASE/api/v1/simulation/scenarios/clear" | python -m json.tool

echo "Storm approaching"
curl -sS -X POST "$BASE/api/v1/simulation/scenarios/storm-approaching" | python -m json.tool

echo "Hold conditions"
curl -sS -X POST "$BASE/api/v1/simulation/scenarios/hold-conditions" | python -m json.tool

echo "Recommendations"
curl -sS "$BASE/api/v1/weather/recommendations" | python -m json.tool

echo "Clearance period"
curl -sS -X POST "$BASE/api/v1/simulation/scenarios/clearance-period" | python -m json.tool

echo "Recommendations after clearance"
curl -sS "$BASE/api/v1/weather/recommendations" | python -m json.tool

echo "Restore clear"
curl -sS -X POST "$BASE/api/v1/simulation/scenarios/clear" | python -m json.tool
