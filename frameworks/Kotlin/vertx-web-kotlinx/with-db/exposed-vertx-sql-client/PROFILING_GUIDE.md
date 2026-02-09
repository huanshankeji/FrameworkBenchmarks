# Profiling Guide: Transaction Provider Comparison

This guide explains how to profile the vertx-web-kotlinx-exposed-vertx-sql-client-postgresql benchmark with different transaction providers.

## Overview

The benchmark supports two transaction provider implementations:
1. **JdbcTransactionExposedTransactionProvider** (default) - Uses JDBC for transaction management
2. **DatabaseExposedTransactionProvider** - Uses direct database transaction management

## Prerequisites

Before profiling, ensure you have:
1. Built and published exposed-vertx-sql-client@703dc89 to Maven Local
2. Copied Maven dependencies to project directory: `cp -r ~/.m2 ./`
3. Docker installed and running
4. TFB (TechEmpower Framework Benchmarks) toolset available

## Quick Start - Local Profiling

### Step 1: Profile with JDBC Provider (Default)

```bash
cd /path/to/FrameworkBenchmarks

# Run the benchmark with JDBC transaction provider
./tfb --test vertx-web-kotlinx-exposed-vertx-sql-client-postgresql --type update

# The benchmark will use JdbcTransactionExposedTransactionProvider by default
```

### Step 2: Profile with Database Provider

```bash
cd /path/to/FrameworkBenchmarks

# Set environment variable to use DatabaseExposedTransactionProvider
export TRANSACTION_PROVIDER=database

# Run the benchmark
./tfb --test vertx-web-kotlinx-exposed-vertx-sql-client-postgresql --type update
```

## Finding Profiling Reports

After running the `tfb` command, profiling reports and results can be found in:

### 1. Profiling Results (async-profiler flame graphs)

The profiling results are embedded in the container logs. To extract them:

```bash
# Navigate to the results directory
cd FrameworkBenchmarks

# Find your results directory (has a timestamp in the name)
ls -lt results/ | head -5

# Extract profiling results using the extraction script
./frameworks/Kotlin/vertx-web-kotlinx/with-db/exposed-vertx-sql-client/extract_profiling_results.sh results/<timestamp>
```

This will create `profile.html` in:
```
results/<timestamp>/vertx-web-kotlinx-exposed-vertx-sql-client-postgresql/run/profile.html
```

**Alternative manual extraction:**
```bash
# The profiling HTML is embedded in the log file between markers
# You can manually extract it:
cd results/<timestamp>/vertx-web-kotlinx-exposed-vertx-sql-client-postgresql/run/
sed -n '/===PROFILING_RESULTS_START===/,/===PROFILING_RESULTS_END===/p' \
    vertx-web-kotlinx-exposed-vertx-sql-client-postgresql.log | \
    grep -v "===PROFILING_RESULTS" > profile.html
```

### 2. TFB Results Directory
```
FrameworkBenchmarks/results/<timestamp>/
```

This directory contains:
- `results.json` - Complete benchmark results with throughput, latency percentiles
- Individual test logs and metrics

### 3. Benchmark Output
The console output will show:
- Requests per second
- Average latency
- Latency percentiles (P50, P75, P90, P95, P99)
- Error rates

### 4. Docker Container Logs
The container logs are automatically saved to:
```
FrameworkBenchmarks/results/<timestamp>/vertx-web-kotlinx-exposed-vertx-sql-client-postgresql/run/vertx-web-kotlinx-exposed-vertx-sql-client-postgresql.log
```

The profiling HTML is embedded in this log file between `===PROFILING_RESULTS_START===` and `===PROFILING_RESULTS_END===` markers.

## Detailed Profiling with async-profiler

The dockerfile automatically enables async-profiler for all benchmark runs. The profiling results are captured in the container logs and can be extracted as described above.

### Automatic Profiling

Profiling is enabled by default with these settings:
- Event: CPU sampling
- Output format: HTML flame graph
- Interval: 1,000,000 nanoseconds (1ms)
- Output location: `/tmp/profile.html` (captured in logs at container exit)

### Viewing Flame Graphs

After extracting the profile.html file:

```bash
# Open the flame graph in a browser
cd results/<timestamp>/vertx-web-kotlinx-exposed-vertx-sql-client-postgresql/run/
open profile.html  # macOS
# or
xdg-open profile.html  # Linux
# or just open it with your browser
```

The flame graphs show:
- Hot code paths (wider sections = more time spent)
- Call stacks (bottom to top)
- CPU time distribution across methods
- Transaction management overhead

### Comparing Providers

To compare both transaction providers:

1. Run with JDBC provider (default):
```bash
./tfb --test vertx-web-kotlinx-exposed-vertx-sql-client-postgresql --type update
./frameworks/Kotlin/vertx-web-kotlinx/with-db/exposed-vertx-sql-client/extract_profiling_results.sh results/<timestamp>
mv results/<timestamp>/vertx-web-kotlinx-exposed-vertx-sql-client-postgresql/run/profile.html profile-jdbc.html
```

2. Run with Database provider:
```bash
export TRANSACTION_PROVIDER=database
./tfb --test vertx-web-kotlinx-exposed-vertx-sql-client-postgresql --type update
./frameworks/Kotlin/vertx-web-kotlinx/with-db/exposed-vertx-sql-client/extract_profiling_results.sh results/<timestamp>
mv results/<timestamp>/vertx-web-kotlinx-exposed-vertx-sql-client-postgresql/run/profile.html profile-database.html
```

3. Compare the two flame graphs side by side

### Manual Profiling (Optional)

If you need additional profiling beyond the automatic flame graphs:

```bash
# While benchmark is running, attach to container
docker ps | grep vertx-web-kotlinx-exposed
docker exec -it <container_id> bash

# Inside container, find Java PID
ps aux | grep java

# Check async-profiler status (automatic profiling is already running)
/opt/async-profiler-4.3-linux-x64/profiler.sh status <PID>
```

### Alternative: Java Flight Recorder

Modify the dockerfile to add JFR options:

```dockerfile
CMD export JAVA_OPTS=" \
    -XX:+FlightRecorder \
    -XX:StartFlightRecording=filename=/tmp/profile.jfr,duration=60s,settings=profile \
    ... (existing options) \
    " && \
    with-db/exposed-vertx-sql-client/build/install/exposed-vertx-sql-client/bin/exposed-vertx-sql-client
```

Then retrieve the JFR file:
```bash
docker cp <container_id>:/tmp/profile.jfr ./profile-jdbc.jfr

# Analyze with jfr tool
jfr print --events jdk.ExecutionSample profile-jdbc.jfr > profile-jdbc.txt
```

## Comparing Results

After profiling both providers:

1. **Throughput Comparison**
   - Compare RPS (Requests Per Second) from results.json
   - Higher is better

2. **Latency Comparison**
   - Compare P50, P95, P99 latencies
   - Lower is better

3. **CPU Profiling**
   - Compare flame graphs from async-profiler
   - Look for hot spots in transaction management code

4. **Memory Usage**
   - Check Docker stats during benchmark: `docker stats <container_id>`

## Example Results Format

```
=== JDBC Transaction Provider ===
Requests/sec: 45,000
Latency P50: 2.1ms
Latency P95: 5.3ms
Latency P99: 8.7ms

=== Database Transaction Provider ===
Requests/sec: 48,500
Latency P50: 1.9ms
Latency P95: 4.8ms
Latency P99: 7.2ms
```

## Automated Profiling Script

For convenience, here's a script to run both profiles:

```bash
#!/bin/bash
# profile-both.sh

echo "=== Profiling with JDBC Provider ==="
export TRANSACTION_PROVIDER=jdbc
./tfb --test vertx-web-kotlinx-exposed-vertx-sql-client-postgresql --type update
cp results/results.json results-jdbc.json

echo "=== Profiling with Database Provider ==="
export TRANSACTION_PROVIDER=database
./tfb --test vertx-web-kotlinx-exposed-vertx-sql-client-postgresql --type update
cp results/results.json results-database.json

echo "=== Comparison ==="
echo "JDBC results: results-jdbc.json"
echo "Database results: results-database.json"
```

## Troubleshooting

### Container Won't Start
- Check Docker logs: `docker logs <container_id>`
- Verify .m2 dependencies are present in project directory
- Ensure TRANSACTION_PROVIDER env var is set correctly

### Low Performance
- Ensure database is properly configured
- Check connection pool settings
- Verify pipelining is enabled (should be by default)

### Profiling Not Working
- Verify async-profiler is installed in container
- Check Java process permissions for profiling
- Try using JFR as an alternative

## Additional Resources

- [TFB Documentation](https://github.com/TechEmpower/FrameworkBenchmarks/wiki)
- [async-profiler Documentation](https://github.com/async-profiler/async-profiler)
- [JFR Documentation](https://docs.oracle.com/javacomponents/jmc-5-4/jfr-runtime-guide/)
- [exposed-vertx-sql-client Repository](https://github.com/huanshankeji/exposed-vertx-sql-client)
