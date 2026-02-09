#!/bin/bash

# Benchmark script using wrk to test vertx-web-kotlinx-postgresql
# This script requires wrk to be installed: https://github.com/wg/wrk
#
# Usage:
# 1. Start the application: ./gradlew :benchmark-runner:run
# 2. In another terminal, run this script: ./run-wrk-benchmarks.sh

set -e

BASE_URL="http://localhost:8080"
DURATION="15s"
THREADS=32
CONNECTIONS=512

echo "=== Vert.x-Web Kotlinx Benchmark with wrk ==="
echo ""
echo "Configuration:"
echo "  Duration: $DURATION"
echo "  Threads: $THREADS"
echo "  Connections: $CONNECTIONS"
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

# Check if server is running
if ! curl -s "${BASE_URL}/json" > /dev/null 2>&1; then
    echo "ERROR: Server is not running at ${BASE_URL}"
    echo "Please start the server first:"
    echo "  cd /path/to/vertx-web-kotlinx"
    echo "  ./gradlew :benchmark-runner:run"
    exit 1
fi

echo "Server is running. Starting benchmarks..."
echo ""

# JSON endpoint
echo "========================================="
echo "Benchmark: JSON Serialization"
echo "========================================="
wrk -H 'Host: localhost' -H 'Accept: application/json' -H 'Connection: keep-alive' \
    --latency -d $DURATION -c $CONNECTIONS --timeout 8 -t $THREADS \
    "${BASE_URL}/json"
echo ""

# Plaintext endpoint
echo "========================================="
echo "Benchmark: Plaintext"
echo "========================================="
wrk -H 'Host: localhost' -H 'Accept: text/plain' -H 'Connection: keep-alive' \
    --latency -d $DURATION -c $CONNECTIONS --timeout 8 -t $THREADS \
    "${BASE_URL}/plaintext"
echo ""

# DB single query
echo "========================================="
echo "Benchmark: Single Database Query"
echo "========================================="
wrk -H 'Host: localhost' -H 'Accept: application/json' -H 'Connection: keep-alive' \
    --latency -d $DURATION -c $CONNECTIONS --timeout 8 -t $THREADS \
    "${BASE_URL}/db"
echo ""

# Multiple queries (5)
echo "========================================="
echo "Benchmark: Multiple Queries (5)"
echo "========================================="
wrk -H 'Host: localhost' -H 'Accept: application/json' -H 'Connection: keep-alive' \
    --latency -d $DURATION -c $CONNECTIONS --timeout 8 -t $THREADS \
    "${BASE_URL}/queries?queries=5"
echo ""

# Multiple queries (20)
echo "========================================="
echo "Benchmark: Multiple Queries (20)"
echo "========================================="
wrk -H 'Host: localhost' -H 'Accept: application/json' -H 'Connection: keep-alive' \
    --latency -d $DURATION -c $CONNECTIONS --timeout 8 -t $THREADS \
    "${BASE_URL}/queries?queries=20"
echo ""

# Updates (5)
echo "========================================="
echo "Benchmark: Updates (5)"
echo "========================================="
wrk -H 'Host: localhost' -H 'Accept: application/json' -H 'Connection: keep-alive' \
    --latency -d $DURATION -c $CONNECTIONS --timeout 8 -t $THREADS \
    "${BASE_URL}/updates?queries=5"
echo ""

# Updates (20)
echo "========================================="
echo "Benchmark: Updates (20)"
echo "========================================="
wrk -H 'Host: localhost' -H 'Accept: application/json' -H 'Connection: keep-alive' \
    --latency -d $DURATION -c $CONNECTIONS --timeout 8 -t $THREADS \
    "${BASE_URL}/updates?queries=20"
echo ""

# Fortunes
echo "========================================="
echo "Benchmark: Fortunes"
echo "========================================="
wrk -H 'Host: localhost' -H 'Accept: text/html' -H 'Connection: keep-alive' \
    --latency -d $DURATION -c $CONNECTIONS --timeout 8 -t $THREADS \
    "${BASE_URL}/fortunes"
echo ""

echo "========================================="
echo "Benchmarks Complete!"
echo "========================================="
