#!/usr/bin/env python3
"""
Manual test to see exactly what's taking so long
"""

import subprocess
import time

def manual_debug_test():
    """Run a quick manual test to see what's happening"""
    print("🔍 MANUAL DEBUG TEST")
    print("=" * 40)
    
    try:
        process = subprocess.Popen([
            'java', '-jar', 'target/indexer-0.0.1-SNAPSHOT.jar'
        ], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        
        # Just check status and exit quickly
        commands = ["2", "0"]  # Status, then exit
        input_text = "\n".join(commands) + "\n"
        
        start_time = time.time()
        stdout, stderr = process.communicate(input=input_text, timeout=30)
        end_time = time.time()
        
        print(f"⏱️ App startup + status check: {end_time - start_time:.1f}s")
        
        # Look for current directory info
        if "Current directory:" in stdout:
            for line in stdout.split('\n'):
                if 'Current directory:' in line or 'Indexed files:' in line:
                    print(f"📊 {line.strip()}")
        
        return True
        
    except subprocess.TimeoutExpired:
        print("❌ Even basic startup is timing out")
        process.kill()
        return False
    except Exception as e:
        print(f"❌ Error: {e}")
        return False

def main():
    print("Quick Manual Debug Test")
    print("Checking if basic app functionality is working")
    print()
    
    success = manual_debug_test()
    
    print("\n" + "=" * 40)
    if success:
        print("✅ Basic app startup is working")
        print("\nTo test manually:")
        print("1. java -jar target/indexer-0.0.1-SNAPSHOT.jar")
        print("2. Option 6 → 2 → ./codebase/dssi-day3-ollama → 0")
        print("3. Option 3 → 'Flask @app.route' → Enter → Enter")
        print("4. Should be much faster now!")
    else:
        print("❌ Basic app functionality has issues")

if __name__ == "__main__":
    main()
