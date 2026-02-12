#!/bin/bash
set -e

PROVIDER=$1

if [ -z "$PROVIDER" ]; then
    echo "Usage: $0 <jdbc|database> [profiler_path]"
    echo "  profiler_path: Path to async-profiler bin directory (optional if asprof is in PATH)"
    exit 1
fi

# Check if asprof is available in PATH
if command -v asprof &> /dev/null; then
    ASPROF_CMD="asprof"
    echo "Using asprof from PATH: $(which asprof)"
else
    PROFILER_PATH=$2
    if [ -z "$PROFILER_PATH" ]; then
        echo "ERROR: asprof command not found in PATH"
        echo ""
        echo "Please either:"
        echo ""
        echo "  1. Add async-profiler bin directory to your PATH:"
        echo "     For current session:"
        echo "       export PATH=\"/path/to/async-profiler/bin:\$PATH\""
        echo "     For permanent use, add the above line to your ~/.bashrc or ~/.bash_profile"
        echo ""
        echo "  2. Provide the profiler path as second argument:"
        echo "       $0 $PROVIDER /path/to/async-profiler/bin"
        echo ""
        exit 1
    fi
    ASPROF_CMD="${PROFILER_PATH}/asprof"
    if [ ! -x "$ASPROF_CMD" ]; then
        echo "ERROR: asprof not found at: $ASPROF_CMD"
        exit 1
    fi
    echo "Using asprof from: $ASPROF_CMD"
fi

echo "========================================="
echo "Profiling with ${PROVIDER} provider"
echo "========================================="

# Start the application
echo "Starting application with ${PROVIDER} provider..."
./gradlew :benchmark-runner:run --args="exposed-vertx-sql-client" \
    -Dtransaction.provider=${PROVIDER} \
    --console=plain --no-daemon 2>&1 | tee app-${PROVIDER}.log &

APP_PID=$!

# Wait for the application to start
echo "Waiting for application to start..."
for i in {1..60}; do
    sleep 1
    if ps aux | grep -q "[j]ava.*BenchmarkRunner"; then
        echo "Application process found!"
        break
    fi
    if [ $i -eq 60 ]; then
        echo "ERROR: Timeout waiting for application to start"
        tail -50 app-${PROVIDER}.log
        exit 1
    fi
done

# Wait a bit more for the app to fully initialize
sleep 10

# Check if the application is running
if ! ps -p $APP_PID > /dev/null 2>&1; then
    echo "ERROR: Gradle process failed"
    tail -50 app-${PROVIDER}.log
    exit 1
fi

# Find the Java process running the Vert.x application
echo "Finding Java process..."
JAVA_PID=$(ps aux | grep "[j]ava.*BenchmarkRunner" | head -1 | awk '{print $2}')

if [ -z "$JAVA_PID" ]; then
    echo "ERROR: Could not find Java process"
    ps aux | grep java
    kill $APP_PID 2>/dev/null || true
    exit 1
fi

echo "Found Java process: $JAVA_PID"

# Note: Wall-clock profiling doesn't require perf events
echo "Note: Using wall-clock profiling mode (doesn't require special perf_event settings)"
echo ""

# Start async-profiler 4.3 in wall-clock mode
echo "Starting async-profiler in wall-clock mode..."
echo "Wall-clock profiling captures time spent regardless of CPU usage"
echo "(includes I/O wait, locks, and other blocking operations)"
echo ""
if ! $ASPROF_CMD start -e wall $JAVA_PID; then
    echo "ERROR: Failed to start profiler with wall-clock events"
    echo ""
    echo "Trying alternative profiling method (itimer)..."
    if ! $ASPROF_CMD start -e itimer $JAVA_PID; then
        echo "ERROR: Failed to start profiler with itimer events"
        kill $APP_PID 2>/dev/null || true
        exit 1
    fi
    echo "Using itimer profiling (similar to wall-clock)"
fi

# Wait a bit for profiler to initialize
sleep 2

# Run wrk benchmarks with queries=20
echo "Running wrk benchmarks for update test (queries=20)..."
./run-wrk-benchmarks.sh --type update -d 30 2>&1 | tee benchmark-${PROVIDER}.txt

# Stop profiling
echo "Stopping async-profiler..."
if ! $ASPROF_CMD stop -o flamegraph -f profile-${PROVIDER}.html $JAVA_PID; then
    echo "WARNING: Failed to stop profiler cleanly"
    echo "The profile may be incomplete or empty"
fi

# Check if profile was generated with actual data
if [ -f "profile-${PROVIDER}.html" ]; then
    FILE_SIZE=$(wc -c < "profile-${PROVIDER}.html")
    if [ "$FILE_SIZE" -lt 20000 ]; then
        echo ""
        echo "WARNING: Generated profile is very small ($FILE_SIZE bytes)"
        echo "This usually means no samples were collected."
        echo "Common causes:"
        echo "  - perf_event_paranoid is too restrictive (check with: cat /proc/sys/kernel/perf_event_paranoid)"
        echo "  - Profiler didn't have enough time to collect samples"
        echo "  - Application wasn't under sufficient load"
        echo ""
        echo "See PROFILING-TROUBLESHOOTING.md for solutions"
    fi
fi

echo "Stopping application..."
kill $APP_PID 2>/dev/null || true
sleep 3
kill -9 $APP_PID 2>/dev/null || true
kill $JAVA_PID 2>/dev/null || true
sleep 2
kill -9 $JAVA_PID 2>/dev/null || true

echo ""
echo "Results saved:"
echo "  - Flame graph: profile-${PROVIDER}.html"
echo "  - Benchmark results: benchmark-${PROVIDER}.txt"
echo "  - Application log: app-${PROVIDER}.log"
echo ""
