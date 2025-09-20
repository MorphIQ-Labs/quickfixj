# QuickFIX/J Performance Benchmarks (Java 25)

This catalog documents all JMH benchmarks in `quickfixj-perf-test`, their purpose, parameters, and how to consume results for before/after comparisons.

See also:
- Baseline instructions and template: `docs/perf-baseline.md`
- Modernization plan and targets: `docs/java25-modernization-plan.md`

## How We Run

- Docker (Java 25):
```
docker run --rm -t -v "$PWD":/ws -w /ws openjdk:25 \
  bash -lc './mvnw -P perf -pl quickfixj-perf-test -am verify'
```
- Local macOS (JDK 25):
```
JAVA_HOME=$(/usr/libexec/java_home -v 25) \
  ./mvnw -P perf -pl quickfixj-perf-test -am verify
```
- Output (perf profile):
  - JSON: `quickfixj-perf-test/target/perf/jmh-results-<timestamp>.json`
  - Console summary
- Tuning (override defaults): `-Dperf.wi=5 -Dperf.i=10 -Dperf.f=1 -Dperf.tu=ns -Dperf.bm=thrpt -Dperf.rf=json`
- Optional profilers (manual run): add `-prof gc` (allocs/GC) and/or `-prof stack` (hot stacks), e.g.:
```
java -jar quickfixj-perf-test/target/quickfixj-perf-test.jar \
  -wi 3 -i 5 -f 1 -tu ns -bm thrpt -prof gc -rff quickfixj-perf-test/target/perf/jmh-gc.json -rf json
```

## Metrics & KPIs

- Primary metric: JMH throughput (ops/time) or time/op depending on the benchmark; we default to `-bm thrpt`.
- Secondary: Allocation rate and GC counts (use `-prof gc`).
- KPI definitions per benchmark are listed below.

## Benchmarks Catalog

### MessageParsePerfTest
- Purpose: Parse a realistic FIX.4.4 ExecutionReport string into a Message.
- Method(s): `baseline()`, `parse()`
- KPI: Throughput (ops/ns) for `parse()`; compare across Java 21 vs 25 and post‑optimization.
- Notes: Hot path for tag scanning and numeric/timestamp conversion.

### MessageCrackerPerfTest
- Purpose: Dispatch a parsed message through a sample application’s `fromApp` handlers.
- Method(s): `baseline()`, `crack()`
- KPI: Throughput (ops/ns) for `crack()`; reflects dispatch overhead and handler path.
- Notes: Informs precomputed handler map/MethodHandle optimizations.

### DictionaryValidationPerfTest
- Purpose: Validate a parsed FIX.4.4 message using `DataDictionary.validate`.
- Method(s): `validate()`
- KPI: Throughput (ops/time) for validation; allocation rate with `-prof gc`.
- Notes: Guides DataDictionary hot‑cache effectiveness.

### ParserRepeatingGroupPerfTest
- Purpose: Parse MarketDataSnapshotFullRefresh (W) with repeating groups (MDEntries).
- Params: `snapshot` (3 entries example)
- Method(s): `parseSnapshot()`
- KPI: Throughput; allocation rate with `-prof gc`.
- Notes: Sensitive to group parsing loops and dictionary lookups.

### RawDataParsingPerfTest
- Purpose: Parse messages with large RawData (95/96) payloads.
- Params: `rawSize` in bytes: 1024, 8192, 65536
- Method(s): `parseRaw_withDictionary()`, `parseRaw_withoutDictionary()`
- KPI: Throughput and allocations vs payload size; with/without dictionary.
- Notes: Helps detect quadratic behavior and buffer reallocation issues.

### LargeMessageParsePerfTest
- Purpose: Parse large MD Snapshot with many MDEntries.
- Params: `entries`: 50, 200, 1000
- Method(s): `parseLarge_withDictionary()`, `parseLarge_withoutDictionary()`
- KPI: Throughput and allocation scaling vs entries; validate impact of caches.
- Notes: Stress test for parser scalability.

### TimestampConverterPerfTest
- Purpose: Conversion performance for `UtcTimestampConverter`.
- Params: `ts`: seconds, millis, micro/nanos
- Method(s): `parse_seconds()`, `parse_millis()`, `format_seconds()`, `format_millis()`
- KPI: Throughput for parse/format across precisions; relevant for tight timestamp loops.
- Notes: Informs fast‑path timestamp improvements.

## Before/After Comparison Workflow

1) Produce a baseline on Java 25 (Docker or local) and save JSON artifacts.
2) Implement a focused optimization (e.g., dictionary cache or parser buffer reuse) gated behind a feature flag.
3) Re‑run the same perf command with identical parameters and environment.
4) Compare:
   - Primary: throughput delta (%, higher is better when `-bm thrpt`).
   - Secondary: allocation MB/s (via `-prof gc`), GC count, and P99 latency (if enabled).
5) Record results in `docs/perf-baseline.md` under “Change Log” with commit/PR reference and deltas.

## Reporting Helpers

- Quick table from JSON (requires `jq` and `column`):
```
jq -r '.[] | [.benchmark, .params | tostring, .primaryMetric.score, .primaryMetric.scoreError, .primaryMetric.scoreUnit] | @tsv' \
  quickfixj-perf-test/target/perf/jmh-results-*.json | column -t
```
- Save CSV directly:
```
java -jar quickfixj-perf-test/target/quickfixj-perf-test.jar -rf csv -rff quickfixj-perf-test/target/perf/jmh-results.csv \
  -wi 3 -i 5 -f 1 -tu ns -bm thrpt
```

## Notes & Caveats

- Keep environment stable (container version, host load) to reduce noise.
- Warm JVM sufficiently; the perf profile defaults (wi=3, i=5) are intended for quick runs, increase for more accuracy.
- Use feature flags for experimental paths; do not change defaults unless fully vetted.
