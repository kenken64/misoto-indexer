#!/usr/bin/env python3
"""
Debug script to investigate why semantic search returns wrong results
"""

import subprocess
import sys
import time
import os

def debug_search_issue():
    """Debug the semantic search issue"""
    print("🔍 DEBUGGING SEMANTIC SEARCH ISSUE")
    print("=" * 60)
    
    # Check what's in the app.py file
    app_py_path = "./codebase/dssi-day3-ollama/app.py"
    if os.path.exists(app_py_path):
        print("✅ app.py exists")
        with open(app_py_path, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()
            route_count = content.count('@app.route')
            print(f"📊 Found {route_count} @app.route decorators in app.py")
            
            # Show the routes
            lines = content.split('\n')
            for i, line in enumerate(lines, 1):
                if '@app.route' in line:
                    print(f"   Line {i}: {line.strip()}")
    else:
        print("❌ app.py not found")
        return
    
    # Check what's actually indexed
    cache_file = ".indexed_dssi-day3-ollama_files_cache.txt"
    if os.path.exists(cache_file):
        print(f"\n📁 Checking indexed files in {cache_file}")
        with open(cache_file, 'r') as f:
            lines = f.readlines()
            app_py_indexed = any('app.py' in line for line in lines)
            start_webapp_indexed = any('start_webapp.py' in line for line in lines)
            
            print(f"   app.py indexed: {'✅' if app_py_indexed else '❌'}")
            print(f"   start_webapp.py indexed: {'✅' if start_webapp_indexed else '❌'}")
            
            if app_py_indexed:
                print("   📝 app.py should contain the Flask routes")
            if start_webapp_indexed:
                print("   📝 start_webapp.py contains Flask setup code")
    
    print("\n🔍 POSSIBLE ISSUES:")
    print("1. Project analysis not being triggered")
    print("2. Search not using correct collection")
    print("3. Vector embeddings not capturing Flask route patterns")
    print("4. Search enhancement not working")
    
    print("\n🔧 DEBUGGING STEPS:")
    print("1. Run semantic search manually and look for:")
    print("   - 'Analyzing project context' message")
    print("   - 'Project Type: Python' or 'Flask'")
    print("   - 'Frameworks: Flask'")
    print("   - Collection: 'codebase-index-dssi-day3-ollama'")
    print("2. If project analysis is not triggered, check directory setting")
    print("3. If wrong collection is used, check getCurrentCollectionName()")

def create_test_search_query():
    """Create a test to verify the search behavior"""
    print("\n🧪 SUGGESTED TEST SEARCH QUERIES:")
    print("=" * 50)
    
    test_queries = [
        "Flask REST API endpoints @app.route",
        "Flask routes app.route decorator",
        "API endpoints generate-sql validate-sql",
        "Flask jsonify response",
        "app.route methods POST"
    ]
    
    for i, query in enumerate(test_queries, 1):
        print(f"{i}. '{query}'")
    
    print("\nExpected results for any of these queries:")
    print("✅ app.py with @app.route decorators")
    print("✅ Line numbers pointing to route definitions")
    print("✅ Content showing Flask route functions")
    print("❌ NOT start_webapp.py with Flask setup code")

def main():
    print("Semantic Search Debug Tool")
    print("Investigating why search returns wrong results")
    print()
    
    debug_search_issue()
    create_test_search_query()
    
    print("\n" + "=" * 60)
    print("📋 NEXT STEPS:")
    print("1. Run manual semantic search with debug output")
    print("2. Check if project analysis is triggered")
    print("3. Verify correct collection is being used")
    print("4. If needed, debug the project-aware search logic")

if __name__ == "__main__":
    main()
