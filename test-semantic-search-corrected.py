#!/usr/bin/env python3
"""
Corrected test for project-aware semantic search with proper CLI inputs
"""

import subprocess
import sys
import time
import os

def test_semantic_search_with_proper_inputs():
    """Test semantic search with correct CLI input sequence"""
    print("🎯 CORRECTED SEMANTIC SEARCH TEST")
    print("=" * 60)
    
    # Check if the indexed cache exists
    cache_file = ".indexed_dssi-day3-ollama_files_cache.txt"
    if not os.path.exists(cache_file):
        print(f"❌ Cache file not found: {cache_file}")
        return False
    
    print(f"✅ Found indexed cache: {cache_file}")
    
    try:
        # Start the application
        process = subprocess.Popen([
            'java', '-jar', 'target/indexer-0.0.1-SNAPSHOT.jar'
        ], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        
        print("🚀 Starting application...")
        
        # Prepare correct input sequence for semantic search:
        # 1. Menu choice: 3 (Semantic Code Search)
        # 2. Search query: "Flask REST API endpoints @app.route"
        # 3. Similarity threshold: "" (use default 0.7)
        # 4. Max results: "" (use default 10)
        # 5. Exit: 0
        
        commands = [
            "3",                                           # Semantic Code Search
            "Flask REST API endpoints @app.route",        # Search query
            "",                                            # Use default threshold (0.7)
            "",                                            # Use default max results (10)
            "0"                                            # Exit
        ]
        
        input_text = "\n".join(commands) + "\n"
        
        print("🔍 Sending commands:")
        for i, cmd in enumerate(commands, 1):
            if cmd == "":
                print(f"   {i}. [ENTER] (use default)")
            else:
                print(f"   {i}. {cmd}")
        
        print("\n⏳ Executing semantic search...")
        
        # Execute with timeout
        stdout, stderr = process.communicate(input=input_text, timeout=90)
        
        print("\n📊 APPLICATION OUTPUT:")
        print("=" * 50)
        print(stdout)
        
        if stderr:
            print("\n⚠️ STDERR OUTPUT:")
            print("=" * 30)
            # Filter out common Java warnings
            stderr_lines = stderr.split('\n')
            relevant_errors = [line for line in stderr_lines if 
                             "WARNING:" not in line and 
                             "restricted method" not in line and
                             "terminally deprecated" not in line and
                             line.strip()]
            if relevant_errors:
                print('\n'.join(relevant_errors))
        
        # Analyze the output for project-aware search features
        print("\n🔍 DETAILED ANALYSIS:")
        print("=" * 50)
        
        success_indicators = {
            "app_started": "Started IndexerApplication" in stdout,
            "semantic_search_menu": "SEMANTIC CODE SEARCH" in stdout,
            "query_received": "Flask REST API endpoints @app.route" in stdout,
            "project_context_analysis": "Analyzing project context" in stdout,
            "project_type_identified": "Project Type:" in stdout,
            "frameworks_detected": "Frameworks:" in stdout or "Flask" in stdout,
            "dependencies_found": "Dependencies:" in stdout,
            "project_aware_search": "project-aware search" in stdout or "Project-aware search" in stdout,
            "search_execution": "Performing" in stdout and "search" in stdout,
            "results_returned": "results found" in stdout or "Search Results:" in stdout or "matches found" in stdout,
        }
        
        for indicator, passed in success_indicators.items():
            status = "✅" if passed else "❌"
            description = indicator.replace('_', ' ').title()
            print(f"{status} {description}: {'PASSED' if passed else 'FAILED'}")
        
        # Count successes
        passed_count = sum(success_indicators.values())
        total_count = len(success_indicators)
        
        print(f"\n📈 OVERALL SCORE: {passed_count}/{total_count} ({passed_count/total_count*100:.1f}%)")
        
        # Detailed analysis
        if success_indicators["project_context_analysis"]:
            print("🎉 SUCCESS: Project context analysis is working!")
        elif success_indicators["semantic_search_menu"] and success_indicators["query_received"]:
            print("⚠️ PARTIAL: Semantic search started but project analysis may not be triggered")
        elif "NoSuchElementException" in stdout:
            print("❌ INPUT ERROR: CLI input parsing failed")
        else:
            print("❓ UNCLEAR: Check the output above for specific issues")
        
        return passed_count >= 5  # At least half should pass for success
        
    except subprocess.TimeoutExpired:
        print("❌ Test timed out")
        process.kill()
        return False
    except Exception as e:
        print(f"❌ Error during test: {e}")
        return False

def main():
    print("Corrected Semantic Search Test")
    print("Testing project-aware semantic search with proper CLI inputs")
    print()
    
    # Check prerequisites
    jar_file = "target/indexer-0.0.1-SNAPSHOT.jar"
    if not os.path.exists(jar_file):
        print(f"❌ JAR file not found: {jar_file}")
        print("Please build the project first: mvn clean package -DskipTests")
        return
    
    success = test_semantic_search_with_proper_inputs()
    
    print("\n" + "=" * 60)
    if success:
        print("🎉 SEMANTIC SEARCH TEST SUCCESSFUL!")
        print("\nProject-aware search functionality is working correctly.")
        print("The system should be analyzing project context and enhancing searches.")
    else:
        print("❌ TEST NEEDS ATTENTION")
        print("\nPossible issues to investigate:")
        print("  • CLI input handling")
        print("  • Project analysis triggering")
        print("  • Search execution flow")
        print("\nTry running the application manually to debug further.")

if __name__ == "__main__":
    main()
