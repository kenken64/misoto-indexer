#!/usr/bin/env python3
"""
Quick test to check if the directory fix worked
"""

import subprocess
import sys

def quick_status_check():
    """Quick check of current status"""
    print("⚡ QUICK STATUS CHECK")
    print("=" * 40)
    
    try:
        process = subprocess.Popen([
            'java', '-jar', 'target/indexer-0.0.1-SNAPSHOT.jar'
        ], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        
        # Just check status and exit
        commands = ["2", "0"]  # Status, then exit
        input_text = "\n".join(commands) + "\n"
        
        stdout, stderr = process.communicate(input=input_text, timeout=45)
        
        # Parse the output
        lines = stdout.split('\n')
        for line in lines:
            if 'Current directory:' in line:
                print(f"🏠 {line.strip()}")
            elif 'Indexed files:' in line:
                print(f"📊 {line.strip()}")
            elif 'Total files:' in line:
                print(f"📁 {line.strip()}")
        
        # Check if dssi-day3-ollama is mentioned
        if 'dssi-day3-ollama' in stdout:
            print("✅ dssi-day3-ollama directory is active")
            return True
        else:
            print("❌ Wrong directory still active")
            return False
            
    except Exception as e:
        print(f"❌ Error: {e}")
        return False

def main():
    print("Quick Status Check")
    print("Checking if the directory fix worked")
    print()
    
    success = quick_status_check()
    
    print("\n" + "=" * 40)
    if success:
        print("✅ DIRECTORY FIX SUCCESSFUL!")
        print("Now manually test semantic search:")
        print("1. Run: java -jar target/indexer-0.0.1-SNAPSHOT.jar")
        print("2. Choose option 3 (Semantic Search)")
        print("3. Search: Flask @app.route endpoints")
        print("4. Should now return app.py with actual routes!")
    else:
        print("❌ Directory not fixed yet")
        print("Try the manual steps in the previous output")

if __name__ == "__main__":
    main()
