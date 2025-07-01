#!/usr/bin/env python3
"""
Test script to validate REST API endpoint search and scoring
"""

import subprocess
import json
import sys
import time

def run_search_test(query, expected_file=None):
    """Run a search test and analyze results"""
    print(f"\n🔍 Testing search query: '{query}'")
    print("=" * 60)
    
    try:
        # Build the Java command
        cmd = [
            "java", "-cp", 
            "target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)",
            "sg.edu.nus.iss.codebase.indexer.IndexerApplication",
            "search",
            "--query", query,
            "--max-results", "5"
        ]
        
        # Run the search
        result = subprocess.run(cmd, capture_output=True, text=True, shell=True, cwd="/Users/kennethphang/Projects/misoto-indexer")
        
        if result.returncode != 0:
            print(f"❌ Search failed with error: {result.stderr}")
            return False
            
        output = result.stdout
        print("Search Results:")
        print(output)
        
        # Check if expected file appears in results
        if expected_file:
            if expected_file in output:
                print(f"✅ Expected file '{expected_file}' found in results")
                return True
            else:
                print(f"❌ Expected file '{expected_file}' NOT found in results")
                return False
                
        return True
        
    except Exception as e:
        print(f"❌ Error running search test: {e}")
        return False

def main():
    """Run comprehensive REST API search tests"""
    print("🧪 REST API Search Validation Tests")
    print("="*60)
    
    # First ensure the test codebase is indexed
    print("📋 Step 1: Ensuring test codebase is indexed...")
    
    # Test queries that should find REST API endpoints
    test_cases = [
        ("REST API endpoints", "app.py"),
        ("Flask API", "app.py"),
        ("API generate SQL", "app.py"),
        ("validate SQL endpoint", "app.py"),
        ("status endpoint", "app.py"),
        ("JSON API", "app.py"),
        ("POST endpoints", "app.py")
    ]
    
    success_count = 0
    total_tests = len(test_cases)
    
    for query, expected_file in test_cases:
        if run_search_test(query, expected_file):
            success_count += 1
        time.sleep(2)  # Brief pause between tests
    
    # Summary
    print("\n" + "="*60)
    print(f"🏆 Test Summary: {success_count}/{total_tests} tests passed")
    
    if success_count == total_tests:
        print("✅ All tests passed! REST API search is working correctly.")
    else:
        print("❌ Some tests failed. REST API search needs improvement.")
        
    return success_count == total_tests

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)
