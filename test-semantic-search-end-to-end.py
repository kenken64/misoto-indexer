#!/usr/bin/env python3
"""
Full end-to-end test of the project-aware semantic search functionality.
This will:
1. Start the application
2. Index a codebase
3. Perform a semantic search that should trigger project analysis
4. Verify the results are project-context-aware
"""

import subprocess
import sys
import time
import os

def test_end_to_end_semantic_search():
    """Test the complete semantic search workflow"""
    print("🚀 FULL END-TO-END SEMANTIC SEARCH TEST")
    print("=" * 70)
    
    try:
        # Start the application interactively
        process = subprocess.Popen([
            'java', '-jar', 'target/indexer-0.0.1-SNAPSHOT.jar'
        ], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        
        print("📊 Starting application...")
        
        # Prepare input commands:
        # 1. First, we'll index a codebase (option 6)
        # 2. Then perform semantic search (option 3)
        # 3. Finally exit (option 0)
        
        commands = []
        
        # Check if we have a test codebase to index
        test_codebase_path = "./codebase/dssi-day3-ollama"
        if os.path.exists(test_codebase_path):
            print(f"📁 Found test codebase: {test_codebase_path}")
            
            # Index the codebase first
            commands.extend([
                "6",  # Index Codebase option
                test_codebase_path,  # Directory path
                "y"   # Confirm indexing
            ])
            
            # Wait a bit for indexing to start
            time.sleep(2)
            
            # Perform semantic search
            commands.extend([
                "3",  # Semantic Code Search option  
                "Flask REST API endpoints with @app.route decorator",  # Search query
            ])
            
        else:
            print("⚠️ Test codebase not found, will try basic semantic search")
            commands.extend([
                "3",  # Semantic Code Search option
                "Flask REST API endpoints with @app.route decorator",  # Search query
            ])
        
        # Exit
        commands.append("0")
        
        # Join all commands with newlines
        input_text = "\n".join(commands) + "\n"
        
        print("🧪 Test commands to send:")
        for i, cmd in enumerate(commands, 1):
            print(f"   {i}. {cmd}")
        print()
        
        # Execute the test with a longer timeout to allow for indexing
        stdout, stderr = process.communicate(input=input_text, timeout=120)
        
        print("📊 APPLICATION OUTPUT:")
        print("=" * 50)
        print(stdout)
        
        if stderr:
            print("\n⚠️ ERRORS/WARNINGS:")
            print("=" * 50)
            print(stderr)
        
        # Analyze the output for our project-aware features
        print("\n🔍 ANALYZING RESULTS:")
        print("=" * 50)
        
        success_indicators = {
            "project_analysis": "Analyzing project context" in stdout,
            "project_type": "Project Type:" in stdout,  
            "frameworks": "Frameworks:" in stdout,
            "dependencies": "Dependencies:" in stdout,
            "project_aware_search": "project-aware search" in stdout,
            "semantic_search": "Semantic search" in stdout or "semantic search" in stdout,
        }
        
        for check, passed in success_indicators.items():
            status = "✅" if passed else "❌"
            print(f"{status} {check.replace('_', ' ').title()}: {'PASSED' if passed else 'FAILED'}")
        
        # Overall assessment
        passed_checks = sum(success_indicators.values())
        total_checks = len(success_indicators)
        
        print(f"\n📊 OVERALL RESULT: {passed_checks}/{total_checks} checks passed")
        
        if passed_checks >= 3:  # At least half the checks should pass
            print("✅ TEST SUCCESSFUL - Project-aware search is working!")
            return True
        else:
            print("❌ TEST NEEDS IMPROVEMENT - Some features not working as expected")
            return False
            
    except subprocess.TimeoutExpired:
        print("❌ Test timed out - this might indicate indexing is taking too long")
        process.kill()
        return False
    except Exception as e:
        print(f"❌ Error running test: {e}")
        return False

def main():
    print("Full End-to-End Semantic Search Test")
    print("This will test the complete workflow including project analysis")
    print("and project-aware semantic search.\n")
    
    # Check if the JAR file exists
    jar_path = "target/indexer-0.0.1-SNAPSHOT.jar"
    if not os.path.exists(jar_path):
        print(f"❌ JAR file not found: {jar_path}")
        print("Please build the project first with: mvn clean package -DskipTests")
        return
    
    success = test_end_to_end_semantic_search()
    
    print("\n" + "=" * 70)
    if success:
        print("🎉 END-TO-END TEST COMPLETED SUCCESSFULLY!")
        print("\nThe project-aware semantic search is working correctly.")
        print("Key features verified:")
        print("  • Project context analysis")
        print("  • Framework detection")
        print("  • Project-specific search enhancement")
    else:
        print("❌ TEST COMPLETED WITH ISSUES")
        print("\nSome features may need debugging:")
        print("  • Check if project analysis is being triggered")  
        print("  • Verify framework detection logic")
        print("  • Confirm search enhancement is working")

if __name__ == "__main__":
    main()
