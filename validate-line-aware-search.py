#!/usr/bin/env python3
"""
Script to validate the enhanced line-aware semantic search
"""

import subprocess
import sys
import time
import os

def main():
    print("🔍 VALIDATING LINE-AWARE SEMANTIC SEARCH")
    print("=" * 60)
    
    # Change to project directory
    os.chdir(r"d:\Projects\misoto-indexer")
    
    # Stop any running processes
    subprocess.run(["taskkill", "/f", "/im", "java.exe"], 
                  capture_output=True, text=True)
    time.sleep(2)
    
    print("\n📋 TESTING PLAN:")
    print("=" * 40)
    print("1. Index the dssi-day3-ollama directory with enhanced analysis")
    print("2. Search for 'REST API endpoint'")
    print("3. Verify results show app.py with these exact line numbers:")
    print("   - Line 31: @app.route('/')")
    print("   - Line 36: @app.route('/api/generate-sql', methods=['POST'])")
    print("   - Line 126: @app.route('/api/validate-sql', methods=['POST'])")
    print("   - Line 153: @app.route('/api/status')")
    print("   - Line 181: @app.route('/examples')")
    
    print("\n🚀 EXPECTED IMPROVEMENTS:")
    print("=" * 40)
    print("✅ Individual documents for each REST API endpoint")
    print("✅ Exact line numbers in search results")
    print("✅ Context showing @app.route decorators")
    print("✅ No irrelevant files (docker_compose.yml, etc.)")
    print("✅ Summary document with all endpoints listed")
    
    print("\n💻 MANUAL TEST STEPS:")
    print("=" * 40)
    print("1. Run: java -jar target\\indexer-0.0.1-SNAPSHOT.jar")
    print("2. Choose option 6 (Index Codebase)")
    print("3. Choose option 2 (Change indexing directory)")
    print("4. Enter: d:\\Projects\\misoto-indexer\\codebase\\dssi-day3-ollama")
    print("5. Choose option 3 (Clear cache and reindex)")
    print("6. Wait for enhanced analysis to complete")
    print("7. Choose option 3 (Semantic Code Search)")
    print("8. Search for: REST API endpoint")
    print("9. Verify line numbers match the expected results above")
    
    print("\n🔧 DEBUGGING INFO:")
    print("=" * 40)
    print("Look for these log messages during indexing:")
    print("📄 Analyzed app.py - Created X documents (5 endpoints, Y functions, Z classes)")
    print("Each endpoint should create a separate document with:")
    print("- documentType: restApiEndpoint")
    print("- lineNumber: [31|36|126|153|181]")
    print("- endpointName: [route path]")
    
    print(f"\n⚡ Starting indexer now...")
    print("=" * 60)

if __name__ == "__main__":
    main()
