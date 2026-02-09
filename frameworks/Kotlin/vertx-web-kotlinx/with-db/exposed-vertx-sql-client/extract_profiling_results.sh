#!/bin/bash
# Extract profiling results from TFB log files
# Usage: ./extract_profiling_results.sh <results_directory>

if [ -z "$1" ]; then
    echo "Usage: $0 <results_directory>"
    echo "Example: $0 results/20260209123456"
    exit 1
fi

RESULTS_DIR="$1"
LOG_FILE="$RESULTS_DIR/vertx-web-kotlinx-exposed-vertx-sql-client-postgresql/run/vertx-web-kotlinx-exposed-vertx-sql-client-postgresql.log"

if [ ! -f "$LOG_FILE" ]; then
    echo "Error: Log file not found: $LOG_FILE"
    exit 1
fi

echo "Extracting profiling results from $LOG_FILE..."

# Extract the profiling results between the markers
sed -n '/===PROFILING_RESULTS_START===/,/===PROFILING_RESULTS_END===/p' "$LOG_FILE" | \
    grep -v "===PROFILING_RESULTS" > "$RESULTS_DIR/vertx-web-kotlinx-exposed-vertx-sql-client-postgresql/run/profile.html"

if [ -s "$RESULTS_DIR/vertx-web-kotlinx-exposed-vertx-sql-client-postgresql/run/profile.html" ]; then
    echo "Profiling results extracted to: $RESULTS_DIR/vertx-web-kotlinx-exposed-vertx-sql-client-postgresql/run/profile.html"
else
    echo "No profiling results found in log file"
    rm -f "$RESULTS_DIR/vertx-web-kotlinx-exposed-vertx-sql-client-postgresql/run/profile.html"
    exit 1
fi
