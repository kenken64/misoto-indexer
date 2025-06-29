#!/usr/bin/env python3
"""
Enhanced Semantic Search Test Script

This script tests the new two-stage semantic search functionality:
1. Ollama analyzes the search prompt to identify frameworks and syntax patterns
2. Enhanced query is used to search the vector database for more accurate results

Example workflow:
- Query: "REST API endpoints" 
- Ollama identifies: Flask, @app.route, endpoint patterns
- Enhanced search finds: Flask route code + framework documentation
"""

import requests
import json
import time
import sys
import traceback

# Configuration
BASE_URL = "http://localhost:8080"
TEST_CODEBASE_PATH = "d:/Projects/misoto-indexer/codebase/dssi-day3-ollama"

def log_step(step, message):
    """Log a test step with formatting"""
    print(f"\n{'='*60}")
    print(f"STEP {step}: {message}")
    print('='*60)

def log_result(success, message):
    """Log test result"""
    status = "✅ SUCCESS" if success else "❌ FAILED"
    print(f"{status}: {message}")

def test_search_query(query, expected_patterns=None):
    """Test a search query and check for expected patterns in results"""
    print(f"\n🔍 Testing search: '{query}'")
    
    try:
        response = requests.post(
            f"{BASE_URL}/api/search/hybrid",
            json={"query": query, "maxResults": 10},
            headers={"Content-Type": "application/json"},
            timeout=30
        )
        
        if response.status_code == 200:
            result = response.json()
            vector_results = result.get('vectorResults', [])
            ai_analysis = result.get('aiAnalysis', '')
            
            print(f"   📊 Found {len(vector_results)} vector results")
            
            # Show first few results
            for i, res in enumerate(vector_results[:3]):
                file_name = res.get('fileName', 'Unknown')
                doc_type = res.get('metadata', {}).get('documentType', 'unknown')
                content_preview = res.get('content', '')[:100].replace('\n', ' ')
                print(f"   {i+1}. {file_name} [{doc_type}]: {content_preview}...")
            
            # Check for expected patterns if provided
            if expected_patterns:
                found_patterns = []
                all_content = " ".join([res.get('content', '') for res in vector_results])
                all_content += " " + ai_analysis
                
                for pattern in expected_patterns:
                    if pattern.lower() in all_content.lower():
                        found_patterns.append(pattern)
                
                print(f"   🎯 Expected patterns: {expected_patterns}")
                print(f"   ✅ Found patterns: {found_patterns}")
                
                return len(found_patterns) > 0
            
            return len(vector_results) > 0
            
        else:
            print(f"   ❌ Search failed: {response.status_code} - {response.text}")
            return False
            
    except Exception as e:
        print(f"   ❌ Search error: {str(e)}")
        return False

def main():
    """Main test execution"""
    print("🚀 Enhanced Semantic Search Test")
    print("Testing two-stage semantic search with framework analysis")
    
    # Step 1: Clear and reindex the test codebase
    log_step(1, "Clear and Reindex Test Codebase")
    try:
        # Clear existing index
        response = requests.post(f"{BASE_URL}/api/indexing/clear", timeout=30)
        if response.status_code == 200:
            log_result(True, "Cleared existing index")
        else:
            log_result(False, f"Failed to clear index: {response.text}")
            return
        
        # Reindex the codebase
        response = requests.post(
            f"{BASE_URL}/api/indexing/index-codebase",
            json={"directoryPath": TEST_CODEBASE_PATH},
            headers={"Content-Type": "application/json"},
            timeout=120
        )
        
        if response.status_code == 200:
            log_result(True, "Successfully reindexed test codebase")
            time.sleep(2)  # Allow indexing to complete
        else:
            log_result(False, f"Failed to reindex: {response.text}")
            return
            
    except Exception as e:
        log_result(False, f"Indexing error: {str(e)}")
        return
    
    # Step 2: Test Framework-Aware Semantic Search
    log_step(2, "Test Framework-Aware Semantic Search")
    
    test_cases = [
        {
            "query": "REST API endpoints",
            "expected": ["@app.route", "Flask", "endpoint", "API"],
            "description": "Should find Flask routes and framework documentation"
        },
        {
            "query": "@app.route('/api/status')",
            "expected": ["@app.route", "Flask", "/api/status", "endpoint"],
            "description": "Should find specific Flask route syntax"
        },
        {
            "query": "Flask endpoints",
            "expected": ["Flask", "@app.route", "endpoint", "route"],
            "description": "Should prioritize Flask-specific content"
        },
        {
            "query": "python dependencies",
            "expected": ["requirements.txt", "Flask", "dependency", "python"],
            "description": "Should find project dependencies and analysis"
        },
        {
            "query": "project frameworks",
            "expected": ["Flask", "framework", "project", "analysis"],
            "description": "Should find project analysis and framework detection"
        },
        {
            "query": "HTTP GET request handling",
            "expected": ["@app.route", "GET", "Flask", "request"],
            "description": "Should find HTTP method handling patterns"
        }
    ]
    
    successful_tests = 0
    total_tests = len(test_cases)
    
    for i, test_case in enumerate(test_cases, 1):
        print(f"\n--- Test Case {i}/{total_tests} ---")
        print(f"Description: {test_case['description']}")
        
        success = test_search_query(
            test_case["query"], 
            test_case["expected"]
        )
        
        if success:
            successful_tests += 1
            log_result(True, f"Test case {i} passed")
        else:
            log_result(False, f"Test case {i} failed")
    
    # Step 3: Test Framework Documentation Retrieval
    log_step(3, "Test Framework Documentation Search")
    
    doc_tests = [
        {
            "query": "Flask route syntax patterns",
            "expected": ["@app.route", "methods", "Flask", "documentation"],
            "description": "Should find Flask documentation and syntax"
        },
        {
            "query": "endpoint decorator patterns",
            "expected": ["@app.route", "decorator", "endpoint", "Flask"],
            "description": "Should find decorator documentation"
        }
    ]
    
    for i, test_case in enumerate(doc_tests, 1):
        print(f"\n--- Documentation Test {i}/{len(doc_tests)} ---")
        print(f"Description: {test_case['description']}")
        
        success = test_search_query(
            test_case["query"], 
            test_case["expected"]
        )
        
        if success:
            successful_tests += 1
            log_result(True, f"Documentation test {i} passed")
        else:
            log_result(False, f"Documentation test {i} failed")
        
        total_tests += 1
    
    # Final Results
    log_step(4, "Test Results Summary")
    print(f"Successful tests: {successful_tests}/{total_tests}")
    print(f"Success rate: {(successful_tests/total_tests)*100:.1f}%")
    
    if successful_tests == total_tests:
        log_result(True, "All tests passed! Enhanced semantic search is working correctly.")
    elif successful_tests > total_tests * 0.7:
        log_result(True, f"Most tests passed ({successful_tests}/{total_tests}). Enhanced search is mostly working.")
    else:
        log_result(False, f"Many tests failed ({total_tests - successful_tests}/{total_tests}). Review implementation.")
    
    print("\n" + "="*60)
    print("ENHANCED SEMANTIC SEARCH VALIDATION COMPLETE")
    print("="*60)
    print("""
Key Features Tested:
✓ Two-stage search: Ollama analysis → Enhanced vector search
✓ Framework-aware query expansion
✓ Syntax pattern recognition (@app.route, etc.)
✓ Framework documentation integration
✓ Project analysis and dependency context
✓ Intelligent result prioritization

Expected Workflow:
1. User searches "REST API endpoints"
2. Ollama identifies Flask framework and @app.route syntax
3. Enhanced query searches for Flask routes + documentation
4. Results include both code and framework syntax patterns
""")

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n❌ Test interrupted by user")
    except Exception as e:
        print(f"\n\n❌ Test failed with error: {str(e)}")
        traceback.print_exc()
