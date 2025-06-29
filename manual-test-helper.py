#!/usr/bin/env python3
"""
Manual test helper - sends commands to the running application
"""

import time
import sys

def send_commands_to_app():
    """Manually send commands to test semantic search"""
    
    print("🔧 MANUAL TEST HELPER")
    print("The application should be running in another terminal.")
    print("Follow these steps:")
    print()
    print("1. In the application menu, choose option 3 (Semantic Code Search)")
    print("2. When prompted for the collection name, use: dssi-day3-ollama")
    print("3. When asked for the directory, enter: ./codebase/dssi-day3-ollama")
    print("4. For the search query, enter: Flask REST API endpoints with @app.route")
    print()
    print("Expected results:")
    print("  ✅ Should see 'Analyzing project context for: ./codebase/dssi-day3-ollama'")
    print("  ✅ Should see 'Project Type: Python' or 'Project Type: Flask'")
    print("  ✅ Should see 'Frameworks: Flask' (or similar)")
    print("  ✅ Should see 'Dependencies: X found'")
    print("  ✅ Should see 'Performing project-aware search...'")
    print("  ✅ Should return Flask-specific search results")
    print()
    print("This will test if the project-aware semantic search is working correctly.")

if __name__ == "__main__":
    send_commands_to_app()
