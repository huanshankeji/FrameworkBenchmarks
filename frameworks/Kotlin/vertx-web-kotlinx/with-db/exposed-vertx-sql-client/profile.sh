#!/bin/bash

# Script to profile the vertx-web-kotlinx-exposed-vertx-sql-client-postgresql benchmark
# with different transaction provider implementations

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
PROFILE_DIR="$SCRIPT_DIR/profiles"

mkdir -p "$PROFILE_DIR"

echo "=== Vertx-Web-Kotlinx-Exposed Transaction Provider Profiling ==="
echo "Profile output directory: $PROFILE_DIR"
echo ""

# Function to run benchmark with profiling
run_with_profiling() {
    local provider_name=$1
    local provider_type=$2
    
    echo "=========================================="
    echo "Profiling with $provider_name"
    echo "=========================================="
    
    export TRANSACTION_PROVIDER="$provider_type"
    
    # Run the TFB benchmark
    echo "Running TFB benchmark..."
    cd "$REPO_ROOT"
    ./tfb --test vertx-web-kotlinx-exposed-vertx-sql-client-postgresql --type update
    
    # Note: In a real scenario, you would:
    # 1. Start the application with JFR enabled
    # 2. Run the benchmark
    # 3. Collect the JFR file
    # 4. Generate reports
    
    echo "Benchmark complete for $provider_name"
    echo ""
}

# Profile with JDBC Transaction Provider (baseline)
run_with_profiling "JdbcTransactionExposedTransactionProvider" "JDBC"

# Profile with Database Transaction Provider (alternative)
run_with_profiling "DatabaseExposedTransactionProvider" "DATABASE"

echo "=========================================="
echo "Profiling Complete"
echo "=========================================="
echo "Profile files saved to: $PROFILE_DIR"
echo ""
echo "To analyze the profiles:"
echo "1. JFR files: jfr print --events jdk.ExecutionSample <file>.jfr"
echo "2. HTML reports: Open the .html files in a browser"
echo "3. Compare the reports to identify performance differences"
echo ""
echo "Key metrics to compare:"
echo "- Requests per second"
echo "- Latency percentiles (P50, P95, P99)"
echo "- CPU time in transaction management"
echo "- Memory allocation patterns"
