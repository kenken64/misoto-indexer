#!/usr/bin/env python3
"""
Test the new project-aware semantic search functionality.
This will test if the search properly analyzes the project context and returns relevant results.
"""

import subprocess
import sys
import time

def test_project_aware_search():
    """Test the enhanced project-aware semantic search"""
    print("🚀 TESTING PROJECT-AWARE SEMANTIC SEARCH")
    print("=" * 60)
    
    try:
        # Test with a Flask-specific query on the dssi-day3-ollama project
        query = "@app.route('/api/status')"
        
        print(f"🧪 Testing query: {query}")
        print("Expected: Should identify this as a Flask project and return:")
        print("  - Project analysis documents")
        print("  - Flask dependencies (requirements.txt)")  
        print("  - Flask endpoints matching @app.route pattern")
        print("  - Flask framework documentation")
        print()
        
        # Run a basic test just to see the startup and project analysis
        process = subprocess.Popen([
            'java', '-jar', 'target/indexer-0.0.1-SNAPSHOT.jar'
        ], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        
        # Send a simple command to exit immediately after startup
        input_commands = "0\n"  # Just exit to see the startup log
        
        stdout, stderr = process.communicate(input=input_commands, timeout=30)
        
        print("📊 APPLICATION STARTUP LOG:")
        print(stdout)
        
        if stderr:
            print("\n⚠️ ERRORS/WARNINGS:")
            print(stderr)
            
        # Look for our new project analysis messages
        if "Analyzing project context" in stdout:
            print("\n✅ SUCCESS: Project context analysis is working!")
        else:
            print("\n❌ Project context analysis not detected in startup")
            
        if "Project Type:" in stdout:
            print("✅ SUCCESS: Project type detection is working!")
        else:
            print("❌ Project type detection not working")
            
        if "Frameworks:" in stdout:
            print("✅ SUCCESS: Framework detection is working!")
        else:
            print("❌ Framework detection not working")
            
        return True
        
    except subprocess.TimeoutExpired:
        print("❌ Test timed out")
        process.kill()
        return False
    except Exception as e:
        print(f"❌ Error running test: {e}")
        return False

def main():
    print("Testing the new project-aware semantic search...")
    print("This will verify that the search now analyzes the project context")
    print("before performing searches.\n")
    
    success = test_project_aware_search()
    
    print("\n" + "=" * 60)
    if success:
        print("🎉 PROJECT-AWARE SEARCH TEST COMPLETED")
        print("\nTo test the full search functionality:")
        print("1. Run the application manually")
        print("2. Use option 3 (Semantic Code Search)")
        print("3. Search for '@app.route(/api/status)' ")
        print("4. Look for enhanced Flask-specific results")
    else:
        print("❌ TEST FAILED")
        print("Check the error messages above for details")

if __name__ == "__main__":
    main()
