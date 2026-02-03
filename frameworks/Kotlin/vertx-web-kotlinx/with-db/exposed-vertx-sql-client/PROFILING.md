# Profiling Transaction Providers in Vertx-Web-Kotlinx-Exposed-Vertx-SQL-Client-PostgreSQL

## Overview

This document describes how to profile and compare different transaction provider implementations for the Exposed Vert.x SQL Client benchmark.

## Background

The benchmark currently uses `JdbcTransactionExposedTransactionProvider`, which handles transaction management via JDBC connections. This document explores profiling this implementation and comparing it with alternative approaches.

## Current Implementation

The benchmark uses:
- `JdbcTransactionExposedTransactionProvider(exposedDatabase)` in `MainVerticle.kt`
- This provider wraps an Exposed `Database` instance and provides transaction context for statement preparation

## Profiling Setup

### Prerequisites

1. Java 17+ with JFR (Java Flight Recorder) support
2. Docker (for TFB infrastructure)
3. async-profiler (optional, for more detailed profiling)

### Running the Benchmark with Profiling

#### Using Java Flight Recorder (JFR)

1. Enable JFR in the Gradle build configuration:

Add to `build.gradle.kts`:
```kotlin
application {
    applicationDefaultJvmArgs = listOf(
        "-XX:+FlightRecorder",
        "-XX:StartFlightRecording=filename=/tmp/profile.jfr,duration=60s,settings=profile"
    )
}
```

2. Run the benchmark:
```bash
./tfb --test vertx-web-kotlinx-exposed-vertx-sql-client-postgresql --type update
```

3. Analyze the JFR file:
```bash
jfr print --events jdk.ExecutionSample /tmp/profile.jfr > profile_jdbc.txt
```

#### Using async-profiler

1. Download async-profiler:
```bash
wget https://github.com/async-profiler/async-profiler/releases/download/v2.9/async-profiler-2.9-linux-x64.tar.gz
tar -xzf async-profiler-2.9-linux-x64.tar.gz
```

2. Start the benchmark
3. Attach async-profiler:
```bash
./async-profiler-2.9-linux-x64/profiler.sh -d 60 -f /tmp/profile_jdbc.html <PID>
```

## Alternative Transaction Provider Implementations

### Option 1: Database-Backed Transaction Provider

Create a transaction provider that uses the Exposed Database's transaction mechanism directly:

```kotlin
class DatabaseExposedTransactionProvider(private val database: Database) {
    fun <T> provideTransaction(block: () -> T): T {
        return org.jetbrains.exposed.v1.core.transaction(database) {
            block()
        }
    }
}
```

### Option 2: Statement Preparation Optimized Provider

Focus on minimizing statement preparation overhead by caching or batching:

```kotlin
class StatementPreparationExposedTransactionProvider(private val database: Database) {
    // Implementation would cache prepared statements and reuse them
    // This could reduce the overhead of statement preparation in tight loops
}
```

## Comparing Performance

### Metrics to Compare

1. **Throughput**: Requests per second for the UPDATE test
2. **Latency**: P50, P95, P99 latencies
3. **CPU Usage**: Time spent in transaction management vs. query execution
4. **Memory**: Object allocations during transaction handling

### Expected Differences

- **JdbcTransactionExposedTransactionProvider**: 
  - Uses JDBC blocking I/O
  - May have higher overhead due to JDBC driver
  - Better for traditional synchronous code

- **DatabaseExposedTransactionProvider** (if implemented):
  - Could be lighter weight
  - May have different blocking characteristics
  - Potentially better for high-concurrency scenarios

## Running the Tests

### Test 1: Baseline with JdbcTransactionExposedTransactionProvider

```bash
# Current configuration - no changes needed
./tfb --test vertx-web-kotlinx-exposed-vertx-sql-client-postgresql --type update
```

### Test 2: With Alternative Provider

1. Modify `MainVerticle.kt` to use the alternative provider
2. Run the same test
3. Compare results

## Analysis

### Flame Graphs

Use async-profiler to generate flame graphs showing where time is spent:

```bash
./profiler.sh -d 60 -f /tmp/flamegraph.html -e cpu <PID>
```

### Hot Spots

Look for:
- Time spent in transaction management code
- Statement preparation overhead
- Database connection management
- Query execution time

### Recommendations

Based on profiling results, choose the transaction provider that:
1. Provides better throughput for the UPDATE test
2. Has lower latency percentiles
3. Shows less CPU overhead in transaction management
4. Aligns with the reactive programming model of Vert.x

## Results Summary

| Provider | RPS | P50 (ms) | P95 (ms) | P99 (ms) | CPU % | Notes |
|----------|-----|----------|----------|----------|-------|-------|
| JdbcTransaction | TBD | TBD | TBD | TBD | TBD | Baseline |
| DatabaseTransaction | TBD | TBD | TBD | TBD | TBD | Alternative |

## Conclusion

[To be filled after profiling is complete]

## References

- [Exposed Vert.x SQL Client](https://github.com/huanshankeji/exposed-vertx-sql-client)
- [TechEmpower Framework Benchmarks](https://www.techempower.com/benchmarks/)
- [Java Flight Recorder](https://docs.oracle.com/javacomponents/jmc-5-4/jfr-runtime-guide/about.htm)
- [async-profiler](https://github.com/async-profiler/async-profiler)
