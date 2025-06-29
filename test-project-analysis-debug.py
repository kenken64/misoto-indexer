#!/usr/bin/env python3
"""
Test to specifically check if project-aware search is working and fix the collection issue
"""

import subprocess
import sys
import time
import os

def test_project_aware_search_debug():
    """Test with detailed debug output to see what's happening"""
    print("🔍 PROJECT-AWARE SEARCH DEBUG TEST")
    print("=" * 60)
    
    try:
        # Start the application
        process = subprocess.Popen([
            'java', '-jar', 'target/indexer-0.0.1-SNAPSHOT.jar'
        ], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        
        print("🚀 Starting application...")
        
        # First check indexing status to see current directory
        # Then perform semantic search
        commands = [
            "2",                                    # Indexing Status
            "3",                                    # Semantic Code Search  
            "Flask app.route decorator",            # Search query (simplified)
            "",                                     # Use default threshold
            "",                                     # Use default max results
            "0"                                     # Exit
        ]
        
        input_text = "\n".join(commands) + "\n"
        
        print("🔧 Test sequence:")
        for i, cmd in enumerate(commands, 1):
            if cmd == "":
                print(f"   {i}. [ENTER] (default)")
            else:
                print(f"   {i}. {cmd}")
        
        print("\n⏳ Executing...")
        
        # Execute with timeout
        stdout, stderr = process.communicate(input=input_text, timeout=90)
        
        print("\n📊 FULL OUTPUT:")
        print("=" * 50)
        print(stdout)
        
        # Analyze the output in detail
        print("\n🔍 DETAILED ANALYSIS:")
        print("=" * 50)
        
        lines = stdout.split('\n')
        
        # Look for key indicators
        current_directory = None
        current_collection = None
        indexed_files = None
        project_analysis_triggered = False
        search_collection_used = None
        
        for line in lines:
            if 'Current directory:' in line:
                current_directory = line.strip()
                print(f"🏠 {current_directory}")
            elif 'Current collection:' in line or 'collection:' in line.lower():
                current_collection = line.strip() 
                print(f"📦 {current_collection}")
            elif 'Indexed files:' in line:
                indexed_files = line.strip()
                print(f"📊 {indexed_files}")
            elif 'Analyzing project context' in line:
                project_analysis_triggered = True
                print(f"🔍 PROJECT ANALYSIS: {line.strip()}")
            elif 'Project Type:' in line:
                print(f"📁 {line.strip()}")
            elif 'Frameworks:' in line:
                print(f"🛠️ {line.strip()}")
            elif 'Dependencies:' in line:
                print(f"📦 {line.strip()}")
            elif 'Semantic search in collection:' in line:
                search_collection_used = line.strip()
                print(f"🎯 SEARCH COLLECTION: {line.strip()}")
        
        print(f"\n📋 SUMMARY:")
        print(f"   Current Directory: {current_directory or 'Not shown'}")
        print(f"   Collection Used: {search_collection_used or 'Not shown'}")
        print(f"   Project Analysis: {'✅ Triggered' if project_analysis_triggered else '❌ Not triggered'}")
        
        # Identify the main issues
        print(f"\n🎯 IDENTIFIED ISSUES:")
        if not project_analysis_triggered:
            print("❌ Project analysis was not triggered during search")
        if search_collection_used and 'dssi-day3-ollama' not in search_collection_used:
            print("❌ Search used wrong collection (should be dssi-day3-ollama)")
        if current_directory and 'dssi-day3-ollama' not in current_directory:
            print("❌ Current directory not set to dssi-day3-ollama")
            
        return project_analysis_triggered
        
    except subprocess.TimeoutExpired:
        print("❌ Test timed out")
        process.kill()
        return False
    except Exception as e:
        print(f"❌ Error during test: {e}")
        return False

def main():
    print("Project-Aware Search Debug Test")
    print("Specifically checking why project analysis is not working")
    print()
    
    success = test_project_aware_search_debug()
    
    print("\n" + "=" * 60)
    if success:
        print("✅ PROJECT ANALYSIS IS WORKING!")
        print("The search should now return correct Flask routes from app.py")
    else:
        print("❌ PROJECT ANALYSIS NOT WORKING")
        print("\nThe issue is likely:")
        print("1. Current directory not set to dssi-day3-ollama")
        print("2. Search using wrong collection") 
        print("3. Project analysis not being triggered")
        print("\nSolution: Use option 6 to set directory to ./codebase/dssi-day3-ollama")

if __name__ == "__main__":
    main()
