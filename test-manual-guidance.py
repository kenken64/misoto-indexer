#!/usr/bin/env python3
"""
Simple test to verify project-aware search functionality
Run this AFTER the dssi-day3-ollama directory has been indexed
"""

import subprocess
import os
import time

def check_current_status():
    """Check what's currently indexed"""
    print("🔍 CHECKING CURRENT STATUS")
    print("=" * 50)
    
    # Check for cache files
    cache_files = [f for f in os.listdir('.') if f.startswith('.indexed_') and f.endswith('_files_cache.txt')]
    
    if cache_files:
        print("✅ Found cache files:")
        for cache_file in cache_files:
            print(f"   📁 {cache_file}")
            # Count lines in cache file
            try:
                with open(cache_file, 'r') as f:
                    indexed_count = len([line for line in f if line.startswith("INDEXED:")])
                print(f"      📊 {indexed_count} files indexed")
            except:
                pass
    else:
        print("❌ No cache files found - no directories have been indexed")
        return False
    
    # Check specifically for dssi-day3-ollama
    dssi_cache = ".indexed_dssi-day3-ollama_files_cache.txt"
    if os.path.exists(dssi_cache):
        print(f"\n✅ dssi-day3-ollama is indexed ({dssi_cache})")
        return True
    else:
        print(f"\n❌ dssi-day3-ollama not indexed - cache file {dssi_cache} missing")
        return False

def manual_test_instructions():
    """Provide manual testing instructions"""
    print("\n🔧 MANUAL TESTING INSTRUCTIONS")
    print("=" * 50)
    print("Since automated testing has input issues, please test manually:")
    print()
    print("1. Run the application:")
    print("   java -jar target/indexer-0.0.1-SNAPSHOT.jar")
    print()
    print("2. Choose option 3 (Semantic Code Search)")
    print()
    print("3. Enter this search query:")
    print("   Flask REST API endpoints @app.route")
    print()
    print("4. Use default threshold (just press ENTER)")
    print()
    print("5. Use default max results (just press ENTER)")
    print()
    print("Expected results if project-aware search is working:")
    print("   ✅ Should see: 'Analyzing project context for: ./codebase/dssi-day3-ollama'")
    print("   ✅ Should see: 'Project Type: Python' or 'Project Type: Flask'")
    print("   ✅ Should see: 'Frameworks: Flask' or similar")
    print("   ✅ Should see: 'Dependencies: X found'")
    print("   ✅ Should see: 'Performing project-aware search...'")
    print("   ✅ Should use collection: 'codebase-index-dssi-day3-ollama'")
    print("   ✅ Should return relevant Flask code results")
    print()

def attempt_simple_test():
    """Try a very simple automated test"""
    print("🎯 ATTEMPTING SIMPLE AUTOMATED TEST")
    print("=" * 50)
    
    try:
        # Very simple test - just start app and exit
        process = subprocess.Popen([
            'java', '-jar', 'target/indexer-0.0.1-SNAPSHOT.jar'
        ], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        
        # Just send exit command to see startup messages
        input_text = "0\n"
        
        stdout, stderr = process.communicate(input=input_text, timeout=45)
        
        print("📊 STARTUP ANALYSIS:")
        print("-" * 30)
        
        # Check what directory/collection is currently active
        lines = stdout.split('\n')
        for line in lines:
            if 'Current directory:' in line:
                print(f"🏠 {line.strip()}")
            elif 'collection' in line.lower() and ('current' in line.lower() or 'using' in line.lower()):
                print(f"📦 {line.strip()}")
            elif 'indexed files:' in line.lower():
                print(f"📊 {line.strip()}")
        
        # Check if dssi-day3-ollama is mentioned
        if 'dssi-day3-ollama' in stdout:
            print("✅ dssi-day3-ollama directory detected in output")
        else:
            print("❌ dssi-day3-ollama directory not mentioned")
            
        return True
        
    except Exception as e:
        print(f"❌ Simple test failed: {e}")
        return False

def main():
    print("Project-Aware Search Status Check")
    print("Checking if the system is ready for project-aware semantic search")
    print()
    
    # Check current indexing status
    has_indexed_data = check_current_status()
    
    if not has_indexed_data:
        print("\n❌ PREREQUISITE MISSING")
        print("The dssi-day3-ollama directory needs to be indexed first.")
        print("Please run the application and use option 6 to index the codebase.")
        return
    
    # Try simple automated test
    print()
    attempt_simple_test()
    
    # Provide manual instructions
    manual_test_instructions()
    
    print("\n" + "=" * 50)
    print("📋 SUMMARY")
    print("=" * 50)
    print("The dssi-day3-ollama codebase appears to be indexed.")
    print("For best results, test the project-aware search manually")  
    print("using the instructions above.")
    print()
    print("Key things to look for:")
    print("  • Project context analysis messages")
    print("  • Correct collection being used")
    print("  • Flask framework detection")
    print("  • Enhanced search results")

if __name__ == "__main__":
    main()
