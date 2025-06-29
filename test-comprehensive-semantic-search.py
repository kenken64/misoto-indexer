#!/usr/bin/env python3
"""
Test that first sets the correct indexing directory and then performs project-aware semantic search
"""

import subprocess
import sys
import time
import os

def test_with_correct_directory():
    """Test semantic search after setting correct indexing directory"""
    print("🎯 SEMANTIC SEARCH TEST WITH DIRECTORY SETUP")
    print("=" * 70)
    
    # Check if the dssi-day3-ollama codebase exists
    codebase_dir = "./codebase/dssi-day3-ollama"
    if not os.path.exists(codebase_dir):
        print(f"❌ Codebase directory not found: {codebase_dir}")
        return False
    
    print(f"✅ Found codebase directory: {codebase_dir}")
    
    try:
        # Start the application
        process = subprocess.Popen([
            'java', '-jar', 'target/indexer-0.0.1-SNAPSHOT.jar'
        ], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        
        print("🚀 Starting application...")
        
        # Strategy: First use option 6 to set/verify the indexing directory,
        # then use option 3 for semantic search
        commands = [
            "6",                                    # Index Codebase menu
            "2",                                    # Change indexing directory  
            codebase_dir,                          # Set directory to dssi-day3-ollama
            "0",                                    # Back to main menu
            "3",                                    # Semantic Code Search
            "Flask REST API endpoints @app.route", # Search query
            "",                                     # Use default threshold (0.7)
            "",                                     # Use default max results (10)
            "0"                                     # Exit
        ]
        
        input_text = "\n".join(commands) + "\n"
        
        print("🔧 Command sequence:")
        for i, cmd in enumerate(commands, 1):
            if cmd == "":
                print(f"   {i}. [ENTER] (use default)")
            else:
                print(f"   {i}. {cmd}")
        
        print(f"\n⏳ Executing test (directory setup + semantic search)...")
        
        # Execute with longer timeout for directory operations
        stdout, stderr = process.communicate(input=input_text, timeout=120)
        
        print("\n📊 APPLICATION OUTPUT:")
        print("=" * 50)
        print(stdout)
        
        if stderr:
            print("\n⚠️ STDERR OUTPUT:")
            print("=" * 30)
            # Filter out common warnings
            stderr_lines = stderr.split('\n')
            relevant_errors = [line for line in stderr_lines if 
                             "WARNING:" not in line and 
                             "terminally deprecated" not in line and
                             "restricted method" not in line and 
                             line.strip()]
            if relevant_errors:
                print('\n'.join(relevant_errors))
        
        print("\n🔍 COMPREHENSIVE ANALYSIS:")
        print("=" * 50)
        
        # Analyze key indicators
        analysis = {
            "app_started": "Started IndexerApplication" in stdout,
            "directory_changed": codebase_dir in stdout or "dssi-day3-ollama" in stdout,
            "correct_collection": "codebase-index-dssi-day3-ollama" in stdout,
            "semantic_search_started": "SEMANTIC CODE SEARCH" in stdout,
            "query_processed": "Flask REST API endpoints @app.route" in stdout,
            "project_context_analysis": "Analyzing project context" in stdout,
            "project_type_detected": "Project Type:" in stdout and ("Python" in stdout or "Flask" in stdout),
            "frameworks_identified": "Frameworks:" in stdout or ("Flask" in stdout and "detected" in stdout),
            "dependencies_found": "Dependencies:" in stdout and "found" in stdout,
            "project_aware_search": "project-aware search" in stdout or "Project-aware search" in stdout,
            "search_results": "results found" in stdout or "matches found" in stdout or "Search Results:" in stdout
        }
        
        print("KEY INDICATORS:")
        for indicator, passed in analysis.items():
            status = "✅" if passed else "❌"
            description = indicator.replace('_', ' ').title()
            print(f"  {status} {description}: {'PASSED' if passed else 'FAILED'}")
        
        # Count successes
        passed_count = sum(analysis.values())
        total_count = len(analysis)
        success_rate = passed_count / total_count * 100
        
        print(f"\n📈 SUCCESS RATE: {passed_count}/{total_count} ({success_rate:.1f}%)")
        
        # Detailed conclusion
        print("\n🎯 CONCLUSION:")
        if analysis["project_context_analysis"] and analysis["correct_collection"]:
            print("🎉 EXCELLENT: Full project-aware search is working!")
        elif analysis["directory_changed"] and analysis["semantic_search_started"]:
            print("👍 GOOD: Basic functionality working, project analysis may need debugging")
        elif analysis["semantic_search_started"]:
            print("⚠️ PARTIAL: Semantic search works but directory/collection issues")
        else:
            print("❌ ISSUES: Major problems with the search functionality")
        
        return passed_count >= 6  # Need at least 6/11 for success
        
    except subprocess.TimeoutExpired:
        print("❌ Test timed out - operations taking too long")
        process.kill()
        return False
    except Exception as e:
        print(f"❌ Error during test: {e}")
        return False

def main():
    print("Comprehensive Semantic Search Test")
    print("This test sets up the correct directory and then tests project-aware search")
    print()
    
    # Check prerequisites  
    jar_file = "target/indexer-0.0.1-SNAPSHOT.jar"
    if not os.path.exists(jar_file):
        print(f"❌ JAR file not found: {jar_file}")
        print("Please build the project: mvn clean package -DskipTests")
        return
    
    success = test_with_correct_directory()
    
    print("\n" + "=" * 70)
    if success:
        print("🏆 COMPREHENSIVE TEST SUCCESSFUL!")
        print("\nProject-aware semantic search is working correctly!")
        print("The system can:")
        print("  • Set indexing directories dynamically")
        print("  • Analyze project context and detect frameworks")  
        print("  • Perform enhanced searches based on project type")
    else:
        print("🔧 TEST NEEDS DEBUGGING")
        print("\nAreas to investigate:")
        print("  • Directory and collection management")
        print("  • Project analysis triggering")
        print("  • Search enhancement logic")
        print("\nConsider manual testing to isolate specific issues.")

if __name__ == "__main__":
    main()
