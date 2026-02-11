#!/bin/bash
set -e

PROFILER_DIR=/opt/async-profiler-4.3-linux-x64
APP_LOG=/tmp/app_output.log

# Start app in background, capturing stdout/stderr to both console and a log file.
with-db/exposed-vertx-sql-client/build/install/exposed-vertx-sql-client/bin/exposed-vertx-sql-client > >(tee "$APP_LOG") 2>&1 &
APP_PID=$!

# Background profiling manager
(
  # Wait for "benchmark server started" in the app log output
  echo "=== async-profiler: waiting for server to start ==="
  while ! grep -q "benchmark server started" "$APP_LOG" 2>/dev/null; do
    sleep 0.5
  done

  # Find the actual Java PID
  JAVA_PID=$(pgrep -f "java.*exposed-vertx-sql-client" | head -1)
  if [ -z "$JAVA_PID" ]; then
    JAVA_PID=$APP_PID
  fi
  echo "=== async-profiler: server started detected (APP_PID=$APP_PID, JAVA_PID=$JAVA_PID) ==="

  # Use "dump" (not "stop") to take a flamegraph snapshot WITHOUT stopping
  # profiling. This avoids interfering with the running JVM.
  # Dump every 30s so that whichever dumps complete before TFB kills the
  # container will be captured in the logs.
  for i in 30 60 90 120 150 180 210 240; do
    sleep 30
    echo "=== async-profiler: dumping flame graph at ${i}s after server start ==="
    "$PROFILER_DIR/bin/asprof" dump -o flamegraph -f /tmp/profile.html "$JAVA_PID" 2>&1 || { echo "asprof dump FAILED at ${i}s"; continue; }

    if [ -f /tmp/profile.html ]; then
      SIZE=$(wc -c < /tmp/profile.html)
      echo "=== profile.html size: $SIZE bytes ==="
      echo "===PROFILING_RESULTS_START==="
      cat /tmp/profile.html
      echo ""
      echo "===PROFILING_RESULTS_END==="
      echo "=== async-profiler: flame graph dumped at ${i}s ==="
    else
      echo "=== async-profiler: /tmp/profile.html NOT FOUND ==="
    fi
  done
) &

# Wait for the application (killed by Docker/TFB when finished)
wait "$APP_PID" 2>/dev/null || true
