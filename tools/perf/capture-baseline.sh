#!/usr/bin/env bash
set -euo pipefail

# Capture a JMH baseline via Docker (Java 25), then append a summary
# to docs/perf-baseline.md and reference the saved JSON artifact.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

echo "[perf] Running JMH perf profile inside openjdk:25 container..."
docker run --rm -t -v "$PWD":/ws -w /ws openjdk:25 \
  bash -lc './mvnw -P perf -pl quickfixj-perf-test -am verify -B -V'

LATEST_JSON=$(ls -1t quickfixj-perf-test/target/perf/jmh-results-*.json 2>/dev/null | head -n 1 || true)
if [[ -z "${LATEST_JSON}" ]]; then
  echo "[perf] ERROR: No JMH JSON result found under quickfixj-perf-test/target/perf" >&2
  exit 1
fi

DATE_UTC=$(date -u +"%Y-%m-%dT%H:%M:%SZ" || echo "unknown")
GIT_SHA=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")

echo "[perf] Latest JMH result: ${LATEST_JSON}"

SUMMARY_TS=""
if command -v jq >/dev/null 2>&1; then
  if command -v column >/dev/null 2>&1; then
    SUMMARY_TS=$(jq -r '.[] | [.benchmark, (.params // {} | tostring), .primaryMetric.score, .primaryMetric.scoreError, .primaryMetric.scoreUnit] | @tsv' "${LATEST_JSON}" | column -t)
  else
    SUMMARY_TS=$(jq -r '.[] | [.benchmark, (.params // {} | tostring), .primaryMetric.score, .primaryMetric.scoreError, .primaryMetric.scoreUnit] | @tsv' "${LATEST_JSON}")
  fi
else
  SUMMARY_TS="Install jq to generate a formatted summary; see ${LATEST_JSON}"
fi

# Write the baseline entry (avoid any accidental execution by using printf)
{
  printf '## Baseline – %s\n\n' "${DATE_UTC}"
  printf -- '- Run ID: %s\n' "${GIT_SHA}"
  printf -- '- Container: openjdk:25\n'
  printf -- '- Artifact: %s\n\n' "${LATEST_JSON}"
  printf '### Summary\n```\n'
  printf '%s\n' "${SUMMARY_TS}"
  printf '```\n\n'
  printf '### Raw Artifact\n- %s\n\n' "${LATEST_JSON}"
  printf '---\n\n'
} >> docs/perf-baseline.md

echo "[perf] Baseline appended to docs/perf-baseline.md"
