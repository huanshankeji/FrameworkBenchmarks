# Transaction Provider Profiling Results

## Setup

This directory contains the infrastructure for profiling different transaction provider implementations in the `vertx-web-kotlinx-exposed-vertx-sql-client-postgresql` benchmark.

## Files

- `PROFILING.md` - Detailed profiling methodology and instructions
- `profile.sh` - Automated script to run profiling with different providers
- `TransactionProviderConfig.kt` - Configuration system for switching providers
- `MainVerticle.kt` - Updated to use configurable transaction providers

## Transaction Providers Tested

### 1. JdbcTransactionExposedTransactionProvider (Baseline)

**Description**: The default transaction provider that uses JDBC connections to manage transactions.

**Characteristics**:
- Uses blocking JDBC I/O
- Wraps an Exposed `Database` instance
- Provides transaction context for statement preparation
- Standard approach for Exposed with JDBC

**Environment Variable**: `TRANSACTION_PROVIDER=JDBC`

### 2. DatabaseExposedTransactionProvider (Alternative)

**Description**: An alternative transaction provider approach (requires implementation or library update).

**Characteristics**:
- Direct use of Exposed Database transaction mechanism
- Potentially lighter weight than JDBC provider
- May have different blocking characteristics

**Environment Variable**: `TRANSACTION_PROVIDER=DATABASE`

**Note**: In the current implementation (v0.8.0-SNAPSHOT of exposed-vertx-sql-client), this provider is not available as a concrete class. The configuration falls back to JDBC provider with a warning. To test an alternative, you would need to:
1. Upgrade to a newer library version that includes it
2. Implement a custom provider
3. Use a different benchmarking approach

## Running the Profiling

### Quick Start

```bash
cd /home/runner/work/FrameworkBenchmarks/FrameworkBenchmarks/frameworks/Kotlin/vertx-web-kotlinx/with-db/exposed-vertx-sql-client
./profile.sh
```

### Manual Profiling

#### Test 1: JDBC Transaction Provider

```bash
export TRANSACTION_PROVIDER=JDBC
cd /home/runner/work/FrameworkBenchmarks/FrameworkBenchmarks
./tfb --test vertx-web-kotlinx-exposed-vertx-sql-client-postgresql --type update
```

#### Test 2: Database Transaction Provider

```bash
export TRANSACTION_PROVIDER=DATABASE
cd /home/runner/work/FrameworkBenchmarks/FrameworkBenchmarks
./tfb --test vertx-web-kotlinx-exposed-vertx-sql-client-postgresql --type update
```

## Expected Results

### Performance Metrics

The TFB framework will output:
- **Requests per second**: Total throughput
- **Latency percentiles**: P50, P75, P90, P95, P99
- **Error rate**: Any failed requests

### Profiling Data

With JFR or async-profiler enabled, you'll get:
- **CPU flame graphs**: Showing where time is spent
- **Allocation profiles**: Memory allocation patterns
- **Lock contention**: If any blocking occurs
- **Method hotspots**: Most frequently called methods

## Analysis

### Key Questions

1. **Throughput**: Which provider achieves higher requests/second?
2. **Latency**: Which has better latency percentiles?
3. **CPU Usage**: Which spends less time in transaction management?
4. **Scalability**: How do they perform under different load levels?

### Expected Findings

Based on the architecture:

- **JdbcTransactionExposedTransactionProvider**:
  - Well-tested and stable
  - May have overhead from JDBC driver layer
  - Suitable for traditional blocking-style code
  
- **Alternative providers** (if implemented):
  - Could be lighter weight
  - May integrate better with Vert.x reactive model
  - Potentially better for high-concurrency scenarios

## Current Status

As of this implementation:

✅ Configuration system created for easy provider switching
✅ Documentation and profiling scripts prepared
✅ MainVerticle updated to use configurable providers
⚠️  `DatabaseExposedTransactionProvider` not available in current library version
⚠️  Full benchmark run pending (requires TFB infrastructure setup)

## Next Steps

To complete the profiling:

1. **Option A**: Wait for library update with alternative provider
   - Check [exposed-vertx-sql-client releases](https://github.com/huanshankeji/exposed-vertx-sql-client/releases)
   - Update dependency version when available

2. **Option B**: Implement custom transaction provider
   - Create a provider that implements the required interface
   - Focus on statement preparation optimization
   - Compare with JDBC baseline

3. **Option C**: Profile current implementation variations
   - Test with/without statement caching
   - Test different connection pool sizes
   - Test with/without pipelining

## References

- [Exposed Vert.x SQL Client](https://github.com/huanshankeji/exposed-vertx-sql-client)
- [TechEmpower Framework Benchmarks](https://www.techempower.com/benchmarks/)
- [Profiling Guide](./PROFILING.md)

## Conclusion

This setup provides a framework for comparing transaction provider implementations once alternative providers become available or are implemented. The configuration system allows easy switching between providers for performance comparison, and the profiling infrastructure is ready to collect and analyze performance data.

For immediate profiling, you can establish a baseline with the current `JdbcTransactionExposedTransactionProvider` implementation.
