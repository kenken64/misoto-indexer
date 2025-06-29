#!/usr/bin/env python3
"""
Quick test script to verify that project analysis documents are now being returned in semantic search.
"""

import subprocess
import sys
import time

def test_semantic_search(query):
    """Test semantic search with a specific query"""
    print(f"\n{'='*60}")
    print(f"🧪 TESTING: {query}")
    print(f"{'='*60}")
    
    try:
        # Use PowerShell-compatible command syntax
        process = subprocess.Popen([
            'powershell', '-Command', 
            f'cd "d:\\Projects\\misoto-indexer"; echo "3`n{query}`n8" | java -jar target/indexer-0.0.1-SNAPSHOT.jar'
        ], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        
        stdout, stderr = process.communicate(timeout=30)
        
        print("📊 OUTPUT:")
        print(stdout)
        
        if stderr and "error" in stderr.lower():
            print("\n⚠️ ERRORS:")
            print(stderr)
            
        # Analyze results
        analyze_results(stdout, query)
        
        return stdout, stderr
        
    except subprocess.TimeoutExpired:
        print("❌ Test timed out")
        process.kill()
        return None, "Timeout"
    except Exception as e:
        print(f"❌ Error running test: {e}")
        return None, str(e)

def analyze_results(output, query):
    """Analyze the search output for project analysis documents"""
    print(f"\n📈 ANALYSIS FOR: {query}")
    print("-" * 40)
    
    if not output:
        print("❌ No output to analyze")
        return
    
    # Check for project analysis indicators
    project_analysis_found = any(indicator in output.lower() for indicator in [
        "project analysis", "project type", "projectanalysis", "documenttype"
    ])
    
    dependencies_found = any(indicator in output.lower() for indicator in [
        "dependencies", "dependency", "libraries", "packages"
    ])
    
    framework_docs_found = any(indicator in output.lower() for indicator in [
        "framework", "flask", "spring", "frameworkdocumentation"
    ])
    
    # Count results
    result_count = output.count("📄") + output.count("1.") + output.count("2.") + output.count("3.")
    
    print(f"🔍 Project Analysis docs: {'✅ YES' if project_analysis_found else '❌ NO'}")
    print(f"📦 Dependencies docs: {'✅ YES' if dependencies_found else '❌ NO'}")
    print(f"📚 Framework docs: {'✅ YES' if framework_docs_found else '❌ NO'}")
    print(f"📊 Total results: {result_count}")
    
    # Check if threshold is being applied
    if "similarity threshold" in output.lower():
        print("✅ Threshold being applied")
    
    # Check for new debug messages
    if "detected project analysis query" in output.lower():
        print("✅ Project analysis query detection working")
    
    if "applied threshold" in output.lower():
        print("✅ Enhanced threshold filtering working")

def main():
    print("🚀 TESTING ENHANCED SEMANTIC SEARCH")
    print("Testing if project analysis documents are now included...")
    
    # Test queries that should return project analysis documents
    test_queries = [
        "@app.route('/api/status')",  # Flask-specific query
        "flask dependencies",         # Framework dependency query  
        "project type framework",     # Project analysis query
        "python dependencies"         # Language dependency query
    ]
    
    for query in test_queries:
        test_semantic_search(query)
        time.sleep(3)  # Brief pause between tests
    
    print(f"\n{'='*60}")
    print("🏁 TEST SUMMARY")
    print(f"{'='*60}")
    print("If the tests show project analysis documents in the results,")
    print("then the fix is working correctly!")

if __name__ == "__main__":
    main()
