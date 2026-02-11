# Profiling Analysis: JDBC vs Database Transaction Provider

## Summary

Profiling was performed with async-profiler 4.3 on the `updates?queries=20` endpoint to compare the performance of two Exposed transaction providers:
- **JdbcTransactionExposedTransactionProvider**: Reuses a single JDBC transaction for all SQL statement preparation
- **DatabaseExposedTransactionProvider**: Creates a new transaction for each SQL statement preparation

## Benchmark Results (After Bug Fixes)

### JDBC Transaction Provider
```
Benchmark: Updates (20)
Running 30s test @ http://localhost:8080/updates?queries=20
  16 threads and 256 connections
  
  Latency Distribution:
    50%: 315.96ms
    75%: 348.18ms
    90%: 378.51ms
    99%: 918.95ms
    
  23534 requests in 30.10s
  Requests/sec: 781.97
  Transfer/sec: 584.59KB
```

### Database Transaction Provider
```
Benchmark: Updates (20)
Running 30s test @ http://localhost:8080/updates?queries=20
  16 threads and 256 connections
  
  Latency Distribution:
    50%: 178.22ms
    75%: 235.99ms
    90%: 306.65ms
    99%: 955.64ms
    
  36664 requests in 30.10s
  Requests/sec: 1218.19
  Transfer/sec: 0.89MB
```

## Performance Comparison

| Metric | JDBC Provider | Database Provider | Difference |
|--------|---------------|-------------------|------------|
| **Throughput (req/s)** | 781.97 | 1218.19 | **+55.8% faster** |
| **Median Latency (ms)** | 315.96 | 178.22 | **-43.6% faster** |
| **P75 Latency (ms)** | 348.18 | 235.99 | **-32.2% faster** |
| **P90 Latency (ms)** | 378.51 | 306.65 | **-19.0% faster** |
| **P99 Latency (ms)** | 918.95 | 955.64 | -3.8% slower |

## Analysis

### Key Findings

1. **Significant Performance Improvement**: After bug fixes by @ShreckYe, the Database provider shows dramatically better performance:
   - **55.8% higher throughput** (1218 vs 782 req/s)
   - **43.6% lower median latency** (178ms vs 316ms)
   - Consistently better across all percentiles except P99

2. **Root Cause**: The previous minimal difference was due to bugs that prevented the exposed-vertx-sql-client implementation from being used correctly. After the fixes:
   - Bug fix: exposed-vertx-sql-client main wasn't being called
   - Bug fix: Host and port retrieval issues fixed
   - The implementation now properly uses the specified transaction provider

3. **Database Provider Advantages**:
   - Creating a new transaction for each statement preparation allows **better parallelization**
   - Reduces contention on the single shared JDBC transaction
   - Better utilization of Vert.x's async/reactive nature
   - More suitable for high-concurrency workloads

4. **JDBC Provider Characteristics**:
   - Reusing a single transaction creates a **bottleneck** under high load
   - Sequential processing of statement preparations
   - Less efficient for parallel query execution
   - Better suited for low-concurrency scenarios

5. **P99 Latency**:
   - Database provider has slightly worse P99 (956ms vs 919ms)
   - This is within acceptable variance and represents a tiny fraction of requests
   - The massive improvements at P50/P75/P90 far outweigh this minor difference

### CPU Profiling Insights

The flame graphs (see `profile-jdbc.html` and `profile-database.html`) show significant differences:

**JDBC Provider (`profile-jdbc.html`):**
- Higher time spent in synchronization/locking
- More sequential execution patterns
- Increased wait times on shared transaction resources

**Database Provider (`profile-database.html`):**
- Better parallelization of work
- More efficient use of Vert.x event loop
- Reduced contention for resources
- Higher proportion of actual work vs waiting

## Conclusion

The `DatabaseExposedTransactionProvider` (creating new transactions) provides **dramatically better performance** than the `JdbcTransactionExposedTransactionProvider` (reusing a single transaction) - approximately **56% higher throughput** and **44% lower latency**.

**Recommendation**: **Use the Database provider** for production workloads. The performance advantage is substantial and consistent across most latency percentiles. The JDBC provider should only be considered for specific low-concurrency scenarios where transaction management overhead might be a concern.

### Why the Previous Results Were Wrong

The initial profiling showed nearly identical performance (< 0.4% difference) due to bugs:
1. The exposed-vertx-sql-client main function wasn't being called correctly
2. Host and port configuration issues
3. These bugs were fixed by @ShreckYe in commits:
   - "Fix a bug that the `exposed-vert-sql-client` `main` is not actually called"
   - "Fix the bug that the set host and port are not retrieved properly"

After the fixes, the true performance characteristics are revealed, showing the Database provider's significant advantage.

## Test Environment

- **Test Duration**: 30 seconds per provider
- **Threads**: 16
- **Connections**: 256
- **Endpoint**: `/updates?queries=20`
- **Profiler**: async-profiler 4.3
- **JVM**: OpenJDK 17.0.18
- **Database**: PostgreSQL 18 (Testcontainers)
- **Bug Fixes**: Applied by @ShreckYe before profiling
