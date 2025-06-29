#!/usr/bin/env python3
"""
Script to force clear collections and reindex the dssi-day3-ollama directory
"""

import subprocess
import sys
import time
import os

def run_command(cmd, description):
    """Run a command and print its description"""
    print(f"\n🔧 {description}")
    print(f"Running: {cmd}")
    
    try:
        # Kill any existing Java processes first
        subprocess.run(["taskkill", "/f", "/im", "java.exe"], 
                      capture_output=True, text=True)
        time.sleep(2)
        
        # Run the command
        result = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=30)
        
        if result.stdout:
            print("STDOUT:", result.stdout)
        if result.stderr:
            print("STDERR:", result.stderr)
        
        print(f"Exit code: {result.returncode}")
        return result.returncode == 0
        
    except subprocess.TimeoutExpired:
        print("❌ Command timed out")
        return False
    except Exception as e:
        print(f"❌ Error: {e}")
        return False

def main():
    print("🚀 Clearing Collections and Reindexing dssi-day3-ollama")
    print("=" * 60)
    
    # Change to project directory
    os.chdir(r"d:\Projects\misoto-indexer")
    
    # Step 1: Stop any running indexer
    print("\n1. Stopping any running indexer...")
    subprocess.run(["taskkill", "/f", "/im", "java.exe"], 
                  capture_output=True, text=True)
    time.sleep(2)
    
    # Step 2: Run the collection clear test to show what needs to be done
    run_command(
        'java -cp "target/indexer-0.0.1-SNAPSHOT.jar;target/test-classes" sg.edu.nus.iss.codebase.indexer.test.CollectionClearTest',
        "Show collection clear workflow"
    )
    
    # Step 3: Force a complete rebuild and reindex
    print("\n" + "=" * 60)
    print("🔄 FORCING COMPLETE REBUILD AND REINDEX")
    print("=" * 60)
    
    # Rebuild the application to ensure latest code
    print("\n2. Rebuilding application with latest improvements...")
    if run_command("mvn clean compile package -DskipTests", "Rebuild application"):
        print("✅ Application rebuilt successfully")
    else:
        print("❌ Failed to rebuild application")
        return
    
    # Step 4: Start indexer with forced clear and reindex
    print("\n3. Starting indexer with automatic directory setup and reindex...")
    
    # Create a batch script to automate the indexer interaction
    batch_content = '''@echo off
echo Starting automated indexer setup...
echo 6
echo 2
echo d:\\Projects\\misoto-indexer\\codebase\\dssi-day3-ollama
echo 3
echo y
timeout /t 30 /nobreak
echo 3
echo REST API endpoints
echo.
echo.
echo 0
'''
    
    with open("auto_reindex.bat", "w") as f:
        f.write(batch_content)
    
    print("Created automation script: auto_reindex.bat")
    print("This will:")
    print("  - Go to option 6 (Index Codebase)")
    print("  - Choose option 2 (Change indexing directory)")
    print("  - Set directory to dssi-day3-ollama")
    print("  - Choose option 3 (Clear cache and reindex)")
    print("  - Wait for indexing to complete")
    print("  - Test semantic search for 'REST API endpoints'")
    
    print("\n" + "=" * 60)
    print("✅ Setup completed!")
    print("💡 Next steps:")
    print("   1. Run: java -jar target\\indexer-0.0.1-SNAPSHOT.jar < auto_reindex.bat")
    print("   2. Or manually start indexer and follow the workflow above")
    print("   3. The improved chunking should now correctly find Flask routes!")
    print("=" * 60)

if __name__ == "__main__":
    main()
