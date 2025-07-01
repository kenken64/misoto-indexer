#!/bin/bash

# Script to test multi-query expansion via CLI
echo "🧪 Testing Multi-Query Expansion via CLI"
echo "========================================"

echo "Starting application and automatically testing 'REST API endpoints' search..."

# Use expect to automate CLI interaction
expect << 'EOF'
set timeout 30
spawn mvn spring-boot:run

# Wait for the menu
expect "Enter your choice:"

# Select option 3 (Semantic Code Search)
send "3\r"

# Wait for query prompt
expect "Enter your search query:"

# Enter the test query that should trigger multi-query expansion
send "REST API endpoints\r"

# Wait for results or error
expect {
    "Multi-query expansion activated" {
        puts "\n✅ SUCCESS: Multi-query expansion triggered!"
        exp_continue
    }
    "📋 Executing 3 targeted queries" {
        puts "\n✅ SUCCESS: Three targeted queries executed!"
        exp_continue
    }
    "Multi-query results:" {
        puts "\n✅ SUCCESS: Multi-query results combined!"
        exp_continue
    }
    "Press Enter to continue" {
        puts "\n✅ Search completed successfully!"
        send "\r"
    }
    timeout {
        puts "\n❌ TIMEOUT: Search took too long"
        exit 1
    }
}

# Exit the application
expect "Enter your choice:"
send "0\r"
expect eof

puts "\n🎉 Multi-query expansion test completed!"
EOF

echo ""
echo "✅ CLI Test completed! Check output above for multi-query expansion messages."