#!/bin/bash

# Benchmark script using wrk to test vertx-web-kotlinx-postgresql
# This script requires wrk to be installed: https://github.com/wg/wrk
#
# Usage:
# 1. Start the application: ./gradlew :benchmark-runner:run
# 2. In another terminal, run this script: ./run-wrk-benchmarks.sh [OPTIONS]
#
# Options:
#   -d, --duration SECONDS    Duration of test in seconds (default: 15)
#   -t, --threads NUM         Number of threads (default: number of CPU cores)
#   -c, --connections NUM     Number of connections (default: 512)
#   --type TYPE               Test type to run: json, plaintext, db, query, fortune, update, all (default: all)
#   -h, --help                Show this help message

set -e

# Default values
BASE_URL="http://localhost:8080"
DURATION=15
THREADS=$(nproc 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo 4)
CONNECTIONS=512
TEST_TYPE="all"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -d|--duration)
            DURATION="$2"
            shift 2
            ;;
        -t|--threads)
            THREADS="$2"
            shift 2
            ;;
        -c|--connections)
            CONNECTIONS="$2"
            shift 2
            ;;
        --type)
            TEST_TYPE="$2"
            shift 2
            ;;
        -h|--help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  -d, --duration SECONDS    Duration of test in seconds (default: 15)"
            echo "  -t, --threads NUM         Number of threads (default: number of CPU cores)"
            echo "  -c, --connections NUM     Number of connections (default: 512)"
            echo "  --type TYPE               Test type: json, plaintext, db, query, fortune, update, all (default: all)"
            echo "  -h, --help                Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0                                    # Run all tests with defaults"
            echo "  $0 --type db -d 30 -c 256             # Run only db test for 30s with 256 connections"
            echo "  $0 --type query -t 16               # Run query tests with 16 threads"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Run '$0 --help' for usage information"
            exit 1
            ;;
    esac
done

echo "=== Vert.x-Web Kotlinx Benchmark with wrk ==="
echo ""
echo "Configuration:"
echo "  Duration: ${DURATION}s"
echo "  Threads: $THREADS"
echo "  Connections: $CONNECTIONS"
echo "  Test Type: $TEST_TYPE"
echo ""

# Check if wrk is installed
if ! command -v wrk &> /dev/null; then
    echo "ERROR: wrk is not installed"
    echo "Please install wrk from: https://github.com/wg/wrk"
    echo ""
    echo "On Ubuntu/Debian: sudo apt-get install wrk"
    echo "On macOS: brew install wrk"
    exit 1
fi

# Check if server is running (skip for json/plaintext which don't require DB)
if [[ "$TEST_TYPE" != "json" && "$TEST_TYPE" != "plaintext" ]]; then
    if ! curl -s "${BASE_URL}/db" > /dev/null 2>&1; then
        echo "WARNING: Database endpoint not responding at ${BASE_URL}"
        echo "Make sure the server is running:"
        echo "  cd /path/to/vertx-web-kotlinx"
        echo "  ./gradlew :benchmark-runner:run"
        echo ""
    fi
fi

echo "Starting benchmarks..."
echo ""

# Helper function to run a benchmark
run_benchmark() {
    local name="$1"
    local endpoint="$2"
    local accept_header="$3"
    
    echo "========================================="
    echo "Benchmark: $name"
    echo "========================================="
    wrk -H 'Host: localhost' -H "Accept: $accept_header" -H 'Connection: keep-alive' \
        --latency -d ${DURATION}s -c $CONNECTIONS --timeout 8 -t $THREADS \
        "${BASE_URL}${endpoint}"
    echo ""
}

# Run benchmarks based on test type
case $TEST_TYPE in
    json)
        run_benchmark "JSON Serialization" "/json" "application/json"
        ;;
    plaintext)
        run_benchmark "Plaintext" "/plaintext" "text/plain"
        ;;
    db)
        run_benchmark "Single Database Query" "/db" "application/json"
        ;;
    query)
        run_benchmark "Multiple Queries (5)" "/queries?queries=5" "application/json"
        run_benchmark "Multiple Queries (20)" "/queries?queries=20" "application/json"
        ;;
    cached-query)
        echo "ERROR: cached-query test type is not implemented for this framework"
        exit 1
        ;;
    update)
        run_benchmark "Updates (5)" "/updates?queries=5" "application/json"
        run_benchmark "Updates (20)" "/updates?queries=20" "application/json"
        ;;
    fortune)
        run_benchmark "Fortunes" "/fortunes" "text/html"
        ;;
    all)
        run_benchmark "JSON Serialization" "/json" "application/json"
        run_benchmark "Plaintext" "/plaintext" "text/plain"
        run_benchmark "Single Database Query" "/db" "application/json"
        run_benchmark "Multiple Queries (5)" "/queries?queries=5" "application/json"
        run_benchmark "Multiple Queries (20)" "/queries?queries=20" "application/json"
        run_benchmark "Updates (5)" "/updates?queries=5" "application/json"
        run_benchmark "Updates (20)" "/updates?queries=20" "application/json"
        run_benchmark "Fortunes" "/fortunes" "text/html"
        ;;
    *)
        echo "ERROR: Invalid test type: $TEST_TYPE"
        echo "Valid types: json, plaintext, db, query, cached-query, fortune, update, all"
        exit 1
        ;;
esac

echo "========================================="
echo "Benchmarks Complete!"
echo "========================================="
