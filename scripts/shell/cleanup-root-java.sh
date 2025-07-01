#!/bin/bash

# Script to clean up Java files from the root directory
# These files should be properly organized in the src/ folder structure

echo "🧹 Cleaning up Java files from root directory..."

# List of Java files to remove from root
java_files_to_remove=(
    "TestSemanticSearch.java"
    "TestQdrantDirect.java"
    "RESTEndpointScoring.java"
    "test-search.java"
    "FixQdrantContent.java"
    "TestTextFieldFix.java"
    "CheckDocContent.java"
    "VerifyTextFieldFix.java"
    "ThresholdTest.java"
    "FactoryTest.java"
    "SimpleQdrantTest.java"
    "TestAlternativeRanking.java"
    "FixQdrantViaSpring.java"
    "AlternativeRankingSystem.java"
    "QuickTest.java"
    "DirectVectorTest.java"
    "TestQdrantWithAPI.java"
    "IndexingTest.java"
    "TestQdrantCloud.java"
    "AlternativeRankingTest.java"
    "DirectSearchTest.java"
    "TestRESTScoring.java"
)

# Remove the files
for file in "${java_files_to_remove[@]}"; do
    if [ -f "$file" ]; then
        echo "🗑️  Removing $file"
        rm "$file"
    else
        echo "⚠️  File $file not found (already removed?)"
    fi
done

# Also remove any compiled .class files that might be in the root
echo "🧹 Removing any .class files from root..."
rm -f *.class

echo "✅ Cleanup complete!"
echo ""
echo "📁 Java files should now be properly organized in:"
echo "   - src/main/java/ (for source code)"
echo "   - src/test/java/ (for test code)"
echo ""
echo "💡 Remember to:"
echo "   1. Add proper package declarations to moved files"
echo "   2. Update import statements if needed"
echo "   3. Run 'mvn compile' to ensure everything builds correctly"
