#!/bin/bash
set -e

PROFILER_DIR=/opt/async-profiler-4.3-linux-x64

# Start the application in background
with-db/exposed-vertx-sql-client/build/install/exposed-vertx-sql-client/bin/exposed-vertx-sql-client &
APP_PID=$!

# Background timer: stop profiler and dump flame graph during benchmark run.
# Timing: ~3s startup + ~140s verification + ~60s into benchmark = ~200s.
# The container lives for ~256s total, so 200s is safely before shutdown.
(
  sleep 10
  echo "=== async-profiler: TEST DUMP at 10s ==="
  "$PROFILER_DIR/bin/asprof" stop -o flamegraph -f /tmp/profile.html "$APP_PID" 2>&1 || echo "asprof stop FAILED"
  if [ -f /tmp/profile.html ]; then
    echo "=== profile.html exists, size: $(wc -c < /tmp/profile.html) bytes ==="
    echo "===PROFILING_RESULTS_START==="
    cat /tmp/profile.html
    echo ""
    echo "===PROFILING_RESULTS_END==="
  else
    echo "=== /tmp/profile.html NOT FOUND ==="
  fi
  echo "=== async-profiler: flame graph dumped ==="
) &

# Wait for the application (killed by Docker when TFB finishes)
wait "$APP_PID" 2>/dev/null || true
