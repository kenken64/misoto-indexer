#!/usr/bin/env python3
"""
Test script for the enhanced line-aware semantic search strategy
"""

import subprocess
import sys
import time
import os

def run_command(cmd, description, timeout=60):
    """Run a command and return its success status"""
    print(f"\n🔧 {description}")
    print(f"Running: {cmd}")
    
    try:
        result = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=timeout)
        
        if result.stdout:
            print("STDOUT:", result.stdout)
        if result.stderr:
            print("STDERR:", result.stderr)
        
        print(f"Exit code: {result.returncode}")
        return result.returncode == 0
        
    except subprocess.TimeoutExpired:
        print(f"❌ Command timed out after {timeout} seconds")
        return False
    except Exception as e:
        print(f"❌ Error: {e}")
        return False

def main():
    print("🧪 Testing Enhanced Line-Aware Semantic Search")
    print("=" * 60)
    
    # Change to project directory
    os.chdir(r"d:\Projects\misoto-indexer")
    
    # Step 1: Stop any running processes
    print("\n1. Stopping any running indexer...")
    subprocess.run(["taskkill", "/f", "/im", "java.exe"], 
                  capture_output=True, text=True)
    time.sleep(2)
    
    # Step 2: Clear and reindex with our enhanced approach
    print("\n" + "=" * 60)
    print("🚀 CLEAR AND REINDEX WITH ENHANCED STRATEGY")
    print("=" * 60)
    
    # Step 3: Start the indexer and test the enhanced search
    print("\n2. Creating test automation script...")
    
    # Create a batch script to automate the full workflow
    batch_content = '''@echo off
echo =================================================
echo ENHANCED SEMANTIC SEARCH TEST
echo =================================================
echo.
echo Starting indexer...
echo 6
echo 2
echo d:\\Projects\\misoto-indexer\\codebase\\dssi-day3-ollama
echo 3
echo y
timeout /t 35 /nobreak
echo.
echo =================================================
echo TESTING SEMANTIC SEARCH
echo =================================================
echo 3
echo REST API endpoint
echo.
echo =================================================
echo TESTING ALTERNATIVE QUERIES
echo =================================================
echo 3
echo Flask route
echo.
echo 3
echo @app.route
echo.
echo 3
echo HTTP endpoint
echo.
echo 0
'''
    
    with open("test_enhanced_search.bat", "w", encoding='utf-8') as f:
        f.write(batch_content)
    
    print("✅ Created test automation script: test_enhanced_search.bat")
    print("\n" + "=" * 60)
    print("📋 TEST PLAN:")
    print("=" * 60)
    print("  1. Clear and reindex dssi-day3-ollama with enhanced analysis")
    print("  2. Test semantic search for 'REST API endpoint'")
    print("  3. Test alternative queries: 'Flask route', '@app.route', 'HTTP endpoint'")
    print("  4. Verify that results show:")
    print("     - app.py with correct line numbers")
    print("     - Line 31: @app.route('/')")
    print("     - Line 36: @app.route('/api/generate-sql', methods=['POST'])")
    print("     - Line 126: @app.route('/api/validate-sql', methods=['POST'])")
    print("     - Line 153: @app.route('/api/status')")
    print("     - Line 181: @app.route('/examples')")
    print("\n💡 Expected improvements:")
    print("   - File-level summaries with line number mappings")
    print("   - Individual documents for each REST API endpoint")
    print("   - Prioritized results for REST API queries")
    print("   - No irrelevant files like docker_compose.yml")
    print("   - Accurate line numbers in search results")
    
    print("\n" + "=" * 60)
    print("🎯 STARTING ENHANCED SEARCH TEST")
    print("=" * 60)
    
    print("\n💻 Run the following command:")
    print(f"java -jar target\\indexer-0.0.1-SNAPSHOT.jar < test_enhanced_search.bat")
    print("\n⚡ Or run interactively:")
    print("java -jar target\\indexer-0.0.1-SNAPSHOT.jar")
    print("Then follow the prompts to index dssi-day3-ollama and test searches.")

if __name__ == "__main__":
    main()
