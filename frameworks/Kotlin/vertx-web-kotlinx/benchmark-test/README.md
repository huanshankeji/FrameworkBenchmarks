# Local Benchmark Testing

This module provides local testing for the vertx-web-kotlinx-postgresql framework without requiring the full TFB Docker orchestration.

## Overview

The test suite uses:
- **Testcontainers** for PostgreSQL - automatically manages database lifecycle
- **JUnit 5** for test execution
- **Built-in HTTP client** for endpoint testing
- **Simple load testing** to replicate wrk-style benchmarking

## Requirements

- JDK 25 (configured in project)
- Docker (for Testcontainers)
- Gradle (wrapper included)

## Running Tests

From the `vertx-web-kotlinx` directory:

```bash
# Run all tests
./gradlew :benchmark-test:test

# Run with more verbose output
./gradlew :benchmark-test:test --info

# Run only functional tests (skip load tests)
./gradlew :benchmark-test:test --tests 'VertxWebKotlinxBenchmarkTest' --exclude-groups 'LoadTests'

# Run only load tests
./gradlew :benchmark-test:test --tests 'VertxWebKotlinxBenchmarkTest$LoadTests'
```

## What Gets Tested

### Functional Tests
- ✅ JSON serialization endpoint (`/json`)
- ✅ Plaintext endpoint (`/plaintext`)
- ✅ Single database query (`/db`)
- ✅ Multiple database queries (`/queries?queries=N`)
- ✅ Database updates (`/updates?queries=N`)
- ✅ Fortunes HTML rendering with XSS protection (`/fortunes`)

### Load Tests
Simple concurrent load tests that measure:
- Throughput (requests/second)
- Average latency
- P50, P95, P99 latencies
- Error rate

## Benefits Over `./tfb --test`

1. **No Docker-in-Docker complexity** - Testcontainers handles database lifecycle
2. **Faster iteration** - No need to rebuild entire TFB toolset
3. **IDE integration** - Run tests directly from IntelliJ IDEA or other IDEs
4. **Consistent results** - Same database schema and data as TFB tests
5. **No SSL certificate issues** - Runs entirely on localhost
6. **Reusable containers** - Testcontainers reuses containers when possible

## How It Works

1. **Setup Phase**:
   - Testcontainers starts a PostgreSQL container
   - Initializes database with TFB schema (`init-postgres.sql`)
   - Starts Vert.x server connected to test database
   
2. **Test Phase**:
   - Functional tests validate response formats and correctness
   - Load tests measure performance under concurrent load
   
3. **Teardown Phase**:
   - Vert.x server is shut down gracefully
   - PostgreSQL container is stopped and removed

## Customization

### Adjust Load Test Parameters

Edit `VertxWebKotlinxBenchmarkTest.kt`:

```kotlin
performLoadTest("/json", requests = 1000, concurrency = 10)
```

### Add Custom Tests

Add new `@Test` methods to the test class:

```kotlin
@Test
fun `test custom endpoint`() {
    val response = get("/custom")
    assertEquals(200, response.statusCode())
    // ... assertions
}
```

## Troubleshooting

### Docker not found
Ensure Docker is running and accessible to your user:
```bash
docker ps
```

### Port 8080 already in use
Stop any services using port 8080:
```bash
lsof -ti:8080 | xargs kill -9
```

### Testcontainers issues
Set environment variable to see more logs:
```bash
export TESTCONTAINERS_RYUK_DISABLED=false
```

## Comparison to TFB `./tfb --test`

| Feature | `./tfb --test` | This Test Suite |
|---------|----------------|-----------------|
| Database setup | Manual Docker | Testcontainers (auto) |
| Benchmarking | wrk/wrk2 | Simple HTTP client |
| SSL issues | May occur | None |
| Speed | Slower (full stack) | Faster (targeted) |
| IDE integration | Limited | Full support |
| CI/CD friendly | Moderate | Excellent |

## Next Steps

For full TFB benchmarking with wrk, you still need to use `./tfb --test vertx-web-kotlinx-postgresql`. This test suite is designed for:
- Development and debugging
- CI/CD validation
- Quick correctness checks
- Local performance profiling
