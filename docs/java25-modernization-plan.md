# QuickFIX/J Modernization Plan for Java 25

## Executive Summary

- Objective: Modernize QuickFIX/J for Java 25 to increase throughput, reduce latency/allocations, and strengthen TLS/network defaults without breaking core APIs.
- Strategy: Apply high‑value Java 21–25 features (virtual threads, structured concurrency, pattern‑matching switch), optimize hot paths (parsing, validation, dispatch, logging, file I/O), and expand JMH coverage to guide changes.
- Delivery: Stage work in phases with clear acceptance criteria and continuous perf baselines.


## Current State & Observations

- Baseline perf (JDK 21):
  - MessageParsePerfTest.parse ≈ 0.892 ops/ns
  - MessageCrackerPerfTest.crack ≈ 0.099 ops/ns
- JDK 24/25 TLS: stricter defaults; legacy ciphers/keys fail (SSL tests require modern suites/keystores and endpoint ID).
- Build: Parallel reactor races fixed; CI is Linux‑only on JDK 25 (Docker). Perf profile added for one‑command JMH.


## Java 25 Features to Leverage

- Virtual Threads (since 21): scale per‑session blocking flows with low overhead.
- Structured Concurrency (21) & Scoped Values: cleaner cancellation/context propagation vs ThreadLocals.
- Pattern Matching (switch/instanceof) & switch expressions: reduce branches/boilerplate in crackers/converters.
- Modern TLS (1.3): prefer TLS_AES_* suites; enable endpoint identification by default where applicable.
- (Optional) Vector API (Incubator): SIMD‑assisted delimiter scanning in parser (behind a feature flag).


## High‑Impact Opportunities

### 1) Virtual Threads for Session Executors
- Add `SessionThreadModel` (platform | virtual) with virtual default disabled.
- Use `Executors.newVirtualThreadPerTaskExecutor()` for session/event tasks; bound pools if needed.
- Expected: better scalability for many sessions; safer blocking I/O; simplified concurrency.
- Risks: Apache MINA blocking internals; keep opt‑in, validate with AT.

### 2) Parser Hot‑Paths
- Reusable parser context (byte[]/ByteBuffer, reusable char[] scratch, per‑thread state via ThreadLocal or ScopedValue).
- Numeric fast paths (int/long/decimal) avoiding BigDecimal unless required; pre‑validated scale for price/qty.
- Avoid constructing Strings unless needed; pass slices to validation; profile hotspots.

### 3) DataDictionary Fast‑Cache
- On load, precompute immutable structures:
  - tag→type (primitive array or compact map)
  - tag→validator lookups
  - required‑fields bitsets per msgType
  - repeating group definitions cache
- Expected: fewer hash lookups/allocations on validation; stable perf under load.

### 4) Cracker Dispatch Fast Path
- Precompute handler map: (beginString or applVerId, msgType) → handler method reference/MethodHandle.
- Fallback to reflection for unknown handlers; no API break.

### 5) Logging & File I/O
- Guard debug logging (no concatenation without `isDebugEnabled()`); parameterized logging.
- Optional: queue + writer thread for bursts; batched appends; reuse encoders; consider NIO `Files.write` with pre‑sized buffers.

### 6) TLS/SSL Test & Runtime Defaults
- Update test keystores to ECDSA/RSA‑PSS; prefer TLS_AES_* suites; enable endpoint identification where expected.
- In runtime, default to TLS 1.3 + endpoint identification if unset; warn on deprecated ciphers.

### 7) Modern Java Language Features
- pattern‑matching switch/instanceof in converters/crackers
- switch expressions for compact, branch‑friendly code

### 8) JMH Coverage Expansion
- Parsing: short/long messages, repeating groups, rawData, mixed tags, dictionary on/off
- Validation: classic vs compiled dictionary
- Cracker dispatch: reflection vs precomputed handlers
- Logging/I/O: overhead with debug on/off; file writer batching

### 9) Structured Concurrency in Tests/Tools
- Use structured concurrency for coordinated tasks (tools, long ATs), to reduce flakiness and simplify cancellation.


## Benchmarks & Triage

### Baselines
- Establish Linux/JDK 25 baselines:
  - Parser throughput (ns/op, ops/sec), allocation rate, GC counts
  - Cracker dispatch throughput
  - Validation cost with/without compiled dictionary

### Triage Rules
- Priority 1: ≥10% parser throughput gain or ≥15% allocation reduction
- Priority 2: ≥5% cracker/validation gain
- Priority 3: Logging/I/O wins that reduce tail latencies

### Perf Matrix
- Dimensions: message size (1–2KB, 5–10KB, 20–50KB), tag density, dictionary on/off, logging (off/debug), store/log (off/on)
- Outputs: ops/sec, ns/op, allocations MB/s, GC, P99 latency


## Concrete Code Changes (Sketch)

- Virtual threads
  - New config; wrap session handlers with virtual‑thread executor; keep platform default
- Parser context
  - `ParserState` (byte[] buffer, char[] scratch) with fast parseInt/Long/Decimal
  - Avoid String creation on hot path; pass slices to validators
- DataDictionary cache
  - Build immutable arrays/maps at load time; remove repeated hash lookups
- Cracker fast path
  - Precompute handler map; direct call via method references/MethodHandles
- Logging & I/O
  - Guard debug; parameterized logs; batch writes; reuse encoders/buffers
- TLS defaults
  - Prefer TLSv1.3/TLS_AES_*; endpoint identification default; warn on insecure configs


## Risk & Compatibility

- Virtual threads: opt‑in; keep acceptance tests as authority; add bounded executor fallback
- Parser/validator: ensure numeric semantics identical; put fast numeric under config if needed (e.g., `qfj.parser.fastNumeric=true`)
- TLS: default secure, allow override; tests updated to modern suites
- Transport: Apache MINA remains default; optional new backend considered later


## CI & Tooling

- Linux‑only CI on JDK 25 (Docker image `openjdk:25`). macOS/Windows jobs are parked until setup‑java supports 25.
- Perf profile: one‑liner to build and run JMH
  - Docker: `docker run --rm -t -v "$PWD":/ws -w /ws openjdk:25 bash -lc './mvnw -P perf -pl quickfixj-perf-test -am verify'`
  - Local macOS (if JDK 25 installed): `JAVA_HOME=$(/usr/libexec/java_home -v 25) ./mvnw -P perf -pl quickfixj-perf-test -am verify`
- Benchmark results: saved to `quickfixj-perf-test/target/perf/jmh-results-<timestamp>.json` and printed to stdout.
- Tuning perf runs: override properties, e.g. `-Dperf.wi=5 -Dperf.i=10 -Dperf.f=2 -Dperf.tu=us -Dperf.rf=csv`.
- Optional nightly perf CI: run select benchmarks and archive `jmh-results-*.json` for trend analysis.


## Work Plan & Timeline

### Phase 0: Infra & Tests (1–2 weeks)
- Stabilize SSL tests for JDK 25 (TLS 1.3 suites, endpoint ID, keystores)
- Expand JMH coverage (parser, validation, cracker, logging/I/O)
- Add nightly perf job (optional)

### Phase 1: Quick Wins (2–3 weeks)
- DataDictionary fast‑cache tables
- Parser scratch buffers; numeric fast paths; String creation avoidance
- Logging guards; batched file I/O where safe
- Cracker dispatch precomputation (no API change)

### Phase 2: Virtual Threads & Structured Concurrency (2–4 weeks)
- Add session thread model option (virtual)
- Integrate vthreads; acceptance tests on
- Use structured concurrency in long‑running tools/tests

### Phase 3: SIMD/Vector API (Exploratory)
- Optional vectorized delimiter scanning behind feature flag; fallback path

### Phase 4: Transport Abstraction (Long‑term)
- Define SPI for alternate I/O backend; prototype modern channel transport; keep MINA default


## Acceptance Criteria

- Parser throughput +15–25% and ≥15% allocation reduction on common messages
- Cracker dispatch +5–10%
- TLS test suite green on JDK 25 with secure defaults
- Virtual threads: stable opt‑in; neutral or better perf for many sessions
- No acceptance test regressions with defaults


## Task Tracker (Initial)

- [ ] Phase 0 – SSL test updates for JDK 25 (TLS 1.3 suites, endpoint ID)
- [ ] Phase 0 – Add JMH: validation on/off, repeating groups, rawData; logging/I/O
- [ ] Phase 1 – DataDictionary hot caches (tag→type, validators, bitsets)
- [ ] Phase 1 – Parser context & numeric fast paths; avoid Strings on hot path
- [ ] Phase 1 – Logging guards; optional batched writer
- [ ] Phase 1 – Cracker handler map & MethodHandles
- [ ] Phase 2 – Virtual thread executor (opt‑in); bound fallback
- [ ] Phase 2 – Structured concurrency in tools/tests
- [ ] Phase 3 – Vector API parser scanning (flagged)
- [ ] Phase 4 – Transport SPI and prototype backend


## References

- JDK 21–25 features: Virtual Threads, Structured Concurrency, Pattern Matching, TLS 1.3 updates
- JMH: https://openjdk.org/projects/code-tools/jmh/
- SLF4J Logging Best Practices: parameterized logging; guards for expensive messages
