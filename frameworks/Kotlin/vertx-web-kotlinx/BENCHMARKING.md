# Local Benchmarking with wrk and Testcontainers

This setup allows you to run the vertx-web-kotlinx application locally with Testcontainers-managed PostgreSQL and benchmark it using the `wrk` command-line tool, replicating the TFB benchmarking approach without requiring the full Docker-in-Docker setup.

## Overview

- **Testcontainers** automatically manages PostgreSQL lifecycle
- **No Docker scripts needed** - dependencies are downloaded once and containers are reused
- **Uses actual `wrk` tool** - same benchmarking tool as TFB
- **Works for all implementations** - default, exposed-vertx-sql-client, r2dbc, etc.

## Prerequisites

1. **Java 25** (configured in project)
2. **Docker** (for Testcontainers)
3. **wrk** - HTTP benchmarking tool
   - Ubuntu/Debian: `sudo apt-get install wrk`
   - macOS: `brew install wrk`
   - From source: https://github.com/wg/wrk

## Quick Start

### 1. Start the Application

```bash
cd frameworks/Kotlin/vertx-web-kotlinx
./gradlew :benchmark-runner:run
```

This will:
- Start a PostgreSQL container using Testcontainers
- Initialize the database with TFB schema
- Start the Vert.x application on port 8080
- Keep running until you press Ctrl+C

The PostgreSQL container is configured with network alias `tfb-database` so the application connects seamlessly without code modifications.

### 2. Run wrk Benchmarks

In another terminal:

```bash
cd frameworks/Kotlin/vertx-web-kotlinx
./run-wrk-benchmarks.sh
```

This script runs wrk benchmarks against all endpoints:
- JSON serialization
- Plaintext
- Single database query
- Multiple queries (5 and 20)
- Database updates (5 and 20)
- Fortunes HTML rendering

### 3. Stop the Application

Press `Ctrl+C` in the terminal running the application. The PostgreSQL container will be automatically stopped.

## Manual wrk Usage

You can also run wrk manually with custom parameters:

```bash
# Basic JSON test
wrk -t 4 -c 100 -d 30s http://localhost:8080/json

# With detailed latency statistics
wrk -t 32 -c 512 -d 15s --latency http://localhost:8080/json

# Database queries
wrk -t 32 -c 512 -d 15s --latency http://localhost:8080/queries?queries=20

# Updates
wrk -t 32 -c 512 -d 15s --latency http://localhost:8080/updates?queries=20
```

### wrk Parameters

- `-t` : Number of threads (typically set to number of CPU cores)
- `-c` : Number of concurrent connections
- `-d` : Duration of test (e.g., `15s`, `1m`)
- `--latency` : Print detailed latency statistics
- `--timeout` : Socket timeout (default: 2s)

## Example Output

```
Running 15s test @ http://localhost:8080/updates?queries=20
  32 threads and 512 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   226.14ms   21.28ms 331.64ms   75.24%
    Req/Sec    70.42     24.90   160.00     70.97%
  Latency Distribution
     50%  224.30ms
     75%  237.84ms
     90%  252.31ms
     99%  282.09ms
  33810 requests in 15.11s, 24.65MB read
Requests/sec:   2236.94
Transfer/sec:      1.63MB
```

## Testing Different Implementations

The benchmark runner currently uses the default implementation (`with-db/default`). To test other implementations:

### exposed-vertx-sql-client

1. Modify `benchmark-runner/build.gradle.kts`:
```kotlin
dependencies {
    implementation(project(":with-db:exposed-vertx-sql-client"))
    // ... other dependencies
}
```

2. Modify `benchmark-runner/src/main/kotlin/BenchmarkRunner.kt`:
```kotlin
// Change from:
MainKt.main()

// To:
// Import and call the appropriate main function for exposed-vertx-sql-client
```

### r2dbc

Similarly, change the project dependency and main function call.

## Advantages over `./tfb --test`

| Feature | `./tfb --test` | This Setup |
|---------|----------------|------------|
| Database setup | Manual Docker | Testcontainers (automatic) |
| Benchmarking tool | wrk (nested Docker) | wrk (direct) |
| Container reuse | Limited | Yes (via Testcontainers) |
| SSL issues | May occur | None |
| Setup complexity | High | Low |
| IDE integration | None | Full support |
| Iteration speed | Slow (full rebuild) | Fast (Gradle incremental) |

## Troubleshooting

### Port 8080 already in use

```bash
# Find and kill process using port 8080
lsof -ti:8080 | xargs kill -9
```

### Docker not running

```bash
# Check Docker status
docker ps

# Start Docker if needed (varies by OS)
```

### wrk not found

Install wrk as described in Prerequisites section.

### Database connection issues

The application connects to PostgreSQL using hostname `tfb-database`. Testcontainers automatically sets up networking with this alias. If you see connection errors, ensure:
1. Docker is running
2. You have permissions to create Docker networks
3. No firewall is blocking Docker networking

## CI/CD Integration

This setup works well in CI/CD environments:

```yaml
# Example GitHub Actions workflow
- name: Install wrk
  run: sudo apt-get update && sudo apt-get install -y wrk

- name: Start application
  run: ./gradlew :benchmark-runner:run &
  
- name: Wait for application
  run: sleep 10

- name: Run benchmarks
  run: ./run-wrk-benchmarks.sh

- name: Stop application
  run: pkill -f benchmark-runner
```

## Performance Tuning

For production-like performance testing, consider:

1. **Increase JVM heap**: Add to `benchmark-runner/build.gradle.kts`:
```kotlin
tasks.named<JavaExec>("run") {
    jvmArgs = listOf("-Xmx4g", "-Xms4g")
}
```

2. **Use more instances**: The application deploys one verticle instance per CPU core by default

3. **Tune PostgreSQL**: Modify `init-postgres.sql` to add PostgreSQL configuration

4. **Use longer test duration**: Modify `DURATION` in `run-wrk-benchmarks.sh` to `30s` or `1m` for more stable results

## Development Workflow

1. Make code changes to the application
2. Restart the benchmark runner: `./gradlew :benchmark-runner:run`
3. Run benchmarks: `./run-wrk-benchmarks.sh`
4. Analyze results and iterate

Testcontainers reuses the PostgreSQL container between runs, so subsequent starts are much faster.
