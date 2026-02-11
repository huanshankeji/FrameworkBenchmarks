# Profiling Analysis: JDBC vs Database Transaction Provider

## Summary

Profiling was performed with async-profiler 4.3 on the `updates?queries=20` endpoint to compare the performance of two Exposed transaction providers:
- **JdbcTransactionExposedTransactionProvider**: Reuses a single JDBC transaction for all SQL statement preparation
- **DatabaseExposedTransactionProvider**: Creates a new transaction for each SQL statement preparation

## Benchmark Results

### JDBC Transaction Provider
```
Benchmark: Updates (20)
Running 30s test @ http://localhost:8080/updates?queries=20
  16 threads and 256 connections
  
  Latency Distribution:
    50%: 309.61ms
    75%: 321.28ms
    90%: 334.61ms
    99%: 411.13ms
    
  24532 requests in 30.06s
  Requests/sec: 816.08
  Transfer/sec: 610.05KB
```

### Database Transaction Provider
```
Benchmark: Updates (20)
Running 30s test @ http://localhost:8080/updates?queries=20
  16 threads and 256 connections
  
  Latency Distribution:
    50%: 311.22ms
    75%: 323.36ms
    90%: 338.30ms
    99%: 402.11ms
    
  24438 requests in 30.05s
  Requests/sec: 813.12
  Transfer/sec: 607.92KB
```

## Performance Comparison

| Metric | JDBC Provider | Database Provider | Difference |
|--------|---------------|-------------------|------------|
| **Throughput (req/s)** | 816.08 | 813.12 | +0.36% faster |
| **Median Latency (ms)** | 309.61 | 311.22 | +0.52% faster |
| **P75 Latency (ms)** | 321.28 | 323.36 | +0.64% faster |
| **P90 Latency (ms)** | 334.61 | 338.30 | +1.09% faster |
| **P99 Latency (ms)** | 411.13 | 402.11 | -2.24% slower |

## Analysis

### Key Findings

1. **Minimal Performance Impact**: The difference between the two providers is negligible in practice - less than 0.4% throughput difference and less than 2ms latency difference at the median.

2. **Slight JDBC Advantage**: The JDBC provider (reusing a single transaction) shows consistently better performance across most percentiles:
   - Throughput: ~3 req/s better (816 vs 813)
   - Median latency: ~1.6ms better
   - P90 latency: ~3.7ms better
   
3. **P99 Anomaly**: Interestingly, the Database provider performs slightly better at P99 (402ms vs 411ms), which could indicate:
   - Less contention under high load scenarios
   - Better tail latency characteristics
   - Natural variance in the measurements

4. **Practical Implications**: 
   - For this workload (updates with 20 queries), both providers perform nearly identically
   - The performance overhead of creating transactions for statement preparation is minimal
   - The choice between providers can be made based on other factors (code simplicity, memory usage, etc.) rather than performance alone

### CPU Profiling Insights

The flame graphs (see `profile-jdbc.html` and `profile-database.html`) show similar hotspots:
- Both spend significant time in:
  - io.netty networking operations
  - io.vertx event loop processing
  - PostgreSQL client encoding/decoding
  - JVM C2 compilation
  
- No significant differences in the call stack patterns between the two providers
- Statement preparation overhead is not visible in the top samples, suggesting it's a small fraction of overall request processing time

## Conclusion

The `JdbcTransactionExposedTransactionProvider` (reusing a single transaction) provides a marginal but consistent performance advantage over the `DatabaseExposedTransactionProvider` (creating new transactions). However, the difference is so small (< 0.4%) that it would be imperceptible in most real-world scenarios.

**Recommendation**: Use the JDBC provider for slightly better performance, but either provider is acceptable from a performance perspective. The choice should be guided by other factors such as:
- Code maintainability
- Thread safety requirements  
- Memory usage patterns
- Specific use case requirements

## Test Environment

- **Test Duration**: 30 seconds per provider
- **Threads**: 16
- **Connections**: 256
- **Endpoint**: `/updates?queries=20`
- **Profiler**: async-profiler 4.3
- **JVM**: OpenJDK 17.0.18
- **Database**: PostgreSQL (Testcontainers)
