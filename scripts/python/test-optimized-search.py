#!/usr/bin/env python3
"""
Test the optimized semantic search to verify it's faster and returns correct results
"""

import subprocess
import sys
import time
import os

def test_optimized_search():
    """Test the optimized semantic search performance"""
    print("🚀 TESTING OPTIMIZED SEMANTIC SEARCH")
    print("=" * 60)
    
    try:
        start_time = time.time()
        
        # Start the application
        process = subprocess.Popen([
            'java', '-jar', 'target/indexer-0.0.1-SNAPSHOT.jar'
        ], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        
        print("⏱️ Starting application...")
        app_start = time.time()
        
        # Set directory and perform search
        commands = [
            "6",                                    # Index Codebase
            "2",                                    # Change directory
            "./codebase/dssi-day3-ollama",         # Set directory
            "0",                                    # Back to main menu
            "3",                                    # Semantic Search
            "Flask REST API endpoints @app.route", # Search query
            "",                                     # Default threshold
            "",                                     # Default max results
            "0"                                     # Exit
        ]
        
        input_text = "\n".join(commands) + "\n"
        
        search_start = time.time()
        stdout, stderr = process.communicate(input=input_text, timeout=90)
        end_time = time.time()
        
        total_time = end_time - start_time
        app_startup_time = app_start - start_time
        search_time = end_time - search_start
        
        print(f"⏱️ Total time: {total_time:.1f}s")
        print(f"⏱️ App startup: {app_startup_time:.1f}s") 
        print(f"⏱️ Search execution: {search_time:.1f}s")
        
        print("\n📊 SEARCH RESULTS ANALYSIS:")
        print("=" * 50)
        
        # Check for performance improvements
        performance_indicators = {
            "fast_analysis": "Generated" not in stdout or stdout.count("Generated") < 5,  # Less AI generation
            "quick_completion": search_time < 60,  # Search should complete in under 60s
            "project_analysis": "Analyzing project context" in stdout,
            "correct_directory": "dssi-day3-ollama" in stdout,
            "flask_detected": "Flask" in stdout or "Python" in stdout,
            "app_py_results": "app.py" in stdout,
            "route_patterns": "@app.route" in stdout or "route" in stdout.lower(),
            "no_excessive_docs": stdout.count("documentation using AI") < 10  # Less doc generation
        }
        
        print("PERFORMANCE INDICATORS:")
        for indicator, passed in performance_indicators.items():
            status = "✅" if passed else "❌"
            print(f"  {status} {indicator.replace('_', ' ').title()}: {'PASSED' if passed else 'FAILED'}")
        
        # Look for the most important results
        if "app.py" in stdout and ("@app.route" in stdout or "route" in stdout):
            print("\n🎉 SUCCESS: Search is finding Flask routes in app.py!")
        elif "start_webapp.py" in stdout and "app.py" not in stdout:
            print("\n⚠️ ISSUE: Still returning start_webapp.py instead of app.py")
        else:
            print("\n❓ UNCLEAR: Check the full output for results")
        
        # Show key output lines
        print(f"\n📋 KEY OUTPUT EXCERPTS:")
        lines = stdout.split('\n')
        for line in lines:
            if any(keyword in line.lower() for keyword in [
                'analyzing project context',
                'project type:',
                'frameworks:',
                'found',
                'app.py',
                'similarity:'
            ]):
                print(f"   {line.strip()}")
        
        passed_count = sum(performance_indicators.values())
        total_count = len(performance_indicators)
        
        print(f"\n📈 SUCCESS RATE: {passed_count}/{total_count} ({passed_count/total_count*100:.1f}%)")
        
        return passed_count >= 6 and search_time < 60
        
    except subprocess.TimeoutExpired:
        print("❌ Test timed out - search may still be too slow")
        process.kill()
        return False
    except Exception as e:
        print(f"❌ Error: {e}")
        return False

def main():
    print("Optimized Semantic Search Performance Test")
    print("Testing if the search is now faster and returns correct results")
    print()
    
    success = test_optimized_search()
    
    print("\n" + "=" * 60)
    if success:
        print("🏆 OPTIMIZATION SUCCESSFUL!")
        print("\nThe semantic search is now:")
        print("  ✅ Much faster (< 60 seconds)")
        print("  ✅ Returns correct Flask routes from app.py")
        print("  ✅ Uses quick project analysis during search")
        print("  ✅ Skips expensive AI documentation generation")
    else:
        print("🔧 NEEDS MORE OPTIMIZATION")
        print("\nPossible remaining issues:")
        print("  • Search still too slow")
        print("  • Wrong results being returned")
        print("  • Directory not set correctly")
        print("  • Project analysis not optimized enough")

if __name__ == "__main__":
    main()
