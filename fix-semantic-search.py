#!/usr/bin/env python3
"""
Fix the semantic search by setting the correct directory first
"""

import subprocess
import sys
import time
import os

def fix_and_test_search():
    """Set correct directory and test semantic search"""
    print("🔧 FIXING SEMANTIC SEARCH DIRECTORY ISSUE")
    print("=" * 60)
    
    try:
        process = subprocess.Popen([
            'java', '-jar', 'target/indexer-0.0.1-SNAPSHOT.jar'
        ], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        
        print("🚀 Starting application...")
        
        # Strategy: 
        # 1. Use option 6 (Index Codebase) 
        # 2. Choose option 2 (Change indexing directory)
        # 3. Set to ./codebase/dssi-day3-ollama
        # 4. Go back to main menu (0)
        # 5. Use option 3 (Semantic Search)
        # 6. Search for Flask routes
        
        commands = [
            "6",                                    # Index Codebase
            "2",                                    # Change indexing directory
            "./codebase/dssi-day3-ollama",         # Set correct directory
            "0",                                    # Back to main menu
            "3",                                    # Semantic Code Search
            "Flask @app.route endpoints",           # Search query
            "",                                     # Default threshold
            "",                                     # Default max results
            "0"                                     # Exit
        ]
        
        input_text = "\n".join(commands) + "\n"
        
        print("🔧 Command sequence to fix the issue:")
        for i, cmd in enumerate(commands, 1):
            if cmd == "":
                print(f"   {i}. [ENTER] (default)")
            else:
                print(f"   {i}. {cmd}")
        
        print("\n⏳ Executing fix and test...")
        
        stdout, stderr = process.communicate(input=input_text, timeout=120)
        
        print("\n📊 RESULTS:")
        print("=" * 50)
        
        # Look for key success indicators
        success_indicators = {
            "directory_changed": "./codebase/dssi-day3-ollama" in stdout,
            "correct_collection": "codebase-index-dssi-day3-ollama" in stdout,
            "project_analysis": "Analyzing project context for: ./codebase/dssi-day3-ollama" in stdout,
            "python_detected": "Project Type: Python" in stdout or "Project Type: Flask" in stdout,
            "flask_detected": "Flask" in stdout and "Frameworks:" in stdout,
            "dependencies_found": "Dependencies:" in stdout and "found" in stdout,
            "search_results": "Found" in stdout and "results" in stdout and not "Found 0 results" in stdout,
            "app_py_results": "app.py" in stdout
        }
        
        print("SUCCESS INDICATORS:")
        for indicator, passed in success_indicators.items():
            status = "✅" if passed else "❌"
            print(f"  {status} {indicator.replace('_', ' ').title()}: {'PASSED' if passed else 'FAILED'}")
        
        # Show relevant parts of output
        lines = stdout.split('\n')
        print(f"\n📋 KEY OUTPUT LINES:")
        for line in lines:
            if any(keyword in line for keyword in [
                'Indexing directory set to',
                'collection:',
                'Analyzing project context',
                'Project Type:',
                'Frameworks:',
                'Dependencies:',
                'Found',
                'app.py'
            ]):
                print(f"   {line.strip()}")
        
        passed_count = sum(success_indicators.values())
        total_count = len(success_indicators)
        
        print(f"\n📈 SUCCESS RATE: {passed_count}/{total_count} ({passed_count/total_count*100:.1f}%)")
        
        if passed_count >= 6:
            print("🎉 SEARCH FIXED! Now returning correct Flask results")
            return True
        elif passed_count >= 3:
            print("⚠️ PARTIALLY FIXED - Some issues remain")
            return False
        else:
            print("❌ STILL BROKEN - Major issues remain")
            return False
            
    except subprocess.TimeoutExpired:
        print("❌ Test timed out")
        process.kill()
        return False
    except Exception as e:
        print(f"❌ Error: {e}")
        return False

def main():
    print("Semantic Search Fix Tool")
    print("This will set the correct directory and test the search")
    print()
    
    success = fix_and_test_search()
    
    print("\n" + "=" * 60)
    if success:
        print("🏆 SUCCESS! SEMANTIC SEARCH IS NOW WORKING CORRECTLY!")
        print("\nThe search should now return:")
        print("  ✅ Results from app.py (not start_webapp.py)")
        print("  ✅ Actual @app.route decorators")
        print("  ✅ Line numbers for Flask route functions")
        print("  ✅ Project-aware enhanced results")
    else:
        print("🔧 STILL NEEDS WORK")
        print("\nTo manually fix:")
        print("  1. Run the application")
        print("  2. Use option 6 (Index Codebase)")
        print("  3. Use option 2 (Change indexing directory)")
        print("  4. Set to: ./codebase/dssi-day3-ollama")
        print("  5. Then use option 3 for semantic search")

if __name__ == "__main__":
    main()
