#!/bin/bash

echo "🔍 QDRANT DATABASE CONTENT CHECK"
echo "================================="

# Check if Qdrant is running
echo "📡 Checking Qdrant connection..."
if curl -s http://localhost:6333/collections > /dev/null 2>&1; then
    echo "✅ Qdrant is running on localhost:6333"
else
    echo "❌ Qdrant not accessible on localhost:6333"
    echo "💡 Check if Qdrant is running or try port 6334"
    exit 1
fi

# List all collections
echo ""
echo "📊 Available collections:"
curl -s http://localhost:6333/collections | jq -r '.result.collections[].name // empty' 2>/dev/null || echo "   (Unable to parse JSON - check manually)"

# Check specific collection that should contain app.py
COLLECTION="codebase-index-dssi-day3-ollama"
echo ""
echo "🔍 Checking collection: $COLLECTION"

# Get collection info
echo "📊 Collection details:"
curl -s "http://localhost:6333/collections/$COLLECTION" | jq '.result' 2>/dev/null || echo "   Collection not found or error"

# Get points count
echo ""
echo "📊 Points in collection:"
POINTS_COUNT=$(curl -s "http://localhost:6333/collections/$COLLECTION" | jq -r '.result.points_count // 0' 2>/dev/null)
echo "   Total points: $POINTS_COUNT"

if [ "$POINTS_COUNT" = "0" ] || [ "$POINTS_COUNT" = "" ]; then
    echo "❌ ISSUE FOUND: Collection has no points (empty)"
    echo "   This explains why vector search returns 0 results"
    echo "   Solutions:"
    echo "   1. Run the indexing process in the application"
    echo "   2. Make sure indexing directory is set to ./codebase/dssi-day3-ollama"
    echo "   3. Wait for indexing to complete"
else
    echo "✅ Collection has $POINTS_COUNT points"
    echo ""
    echo "📝 Next steps:"
    echo "   1. Run the application: mvn spring-boot:run"
    echo "   2. Try semantic search with query: 'REST API endpoints'"
    echo "   3. Look for debug output showing enhanced queries"
    echo "   4. Check if app.py appears in results"
fi

echo ""
echo "📋 DIAGNOSTIC COMPLETE"
echo "======================"