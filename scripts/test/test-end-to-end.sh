#!/bin/bash

echo "=== Comprehensive End-to-End Test ==="
echo "Testing dynamic cache file naming and collection handling"
echo "========================================================"

# Start the application in background and capture PID
cd /d/Projects/misoto-indexer
echo "Starting application..."

# Create a simple test directory with a few files
TEST_DIR="./test-codebase"
mkdir -p "$TEST_DIR"
echo "public class TestClass { }" > "$TEST_DIR/TestClass.java"
echo "# Test README" > "$TEST_DIR/README.md"
echo "console.log('test');" > "$TEST_DIR/test.js"

echo "Created test directory: $TEST_DIR"
echo "Test files:"
ls -la "$TEST_DIR"

echo ""
echo "Expected behaviors:"
echo "1. Collection name: codebase-index-test-codebase"
echo "2. Cache file: .indexed_test-codebase_files_cache.txt"
echo ""

# Cleanup function
cleanup() {
    echo "Cleaning up..."
    rm -rf "$TEST_DIR"
    rm -f .indexed_test-codebase_files_cache.txt
    pkill -f "sg.edu.nus.iss.codebase.indexer.IndexerApplication" 2>/dev/null || true
}

# Set trap to cleanup on exit
trap cleanup EXIT

echo "Test directory created successfully at: $(pwd)/$TEST_DIR"
echo "You can now:"
echo "1. Start the application: mvn compile exec:java -Dexec.mainClass=sg.edu.nus.iss.codebase.indexer.IndexerApplication"
echo "2. Choose option 6 (Index Codebase)"
echo "3. Enter path: $(pwd)/$TEST_DIR"
echo "4. Verify that cache file .indexed_test-codebase_files_cache.txt is created"
echo ""
echo "Press Enter to cleanup and exit..."
read
