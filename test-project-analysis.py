#!/usr/bin/env python3
"""
Test script for the new Project Analysis and Dependency Detection feature
"""

import subprocess
import sys
import time
import os

def main():
    print("🔍 TESTING PROJECT ANALYSIS & DEPENDENCY DETECTION")
    print("=" * 60)
    
    # Change to project directory
    os.chdir(r"d:\Projects\misoto-indexer")
    
    # Stop any running processes
    subprocess.run(["taskkill", "/f", "/im", "java.exe"], 
                  capture_output=True, text=True)
    time.sleep(2)
    
    print("\n📋 NEW FEATURE OVERVIEW:")
    print("=" * 40)
    print("🎯 Project Type Discovery:")
    print("   - Detects Python, Java (Maven/Gradle), JavaScript (Node.js)")
    print("   - Identifies Spring Boot and Flask frameworks")
    print("   - Recognizes mixed-language projects")
    print()
    print("📦 Dependency Detection:")
    print("   - Python: requirements.txt, setup.py, pyproject.toml, import statements")
    print("   - Java: pom.xml, build.gradle dependencies")
    print("   - JavaScript: package.json dependencies")
    print("   - Framework-specific libraries and patterns")
    print()
    print("🗄️ Vector Storage:")
    print("   - Project analysis stored as searchable documents")
    print("   - Dependencies grouped by language (python, java, javascript)")
    print("   - Framework detection and technology stack analysis")
    
    print("\n🧪 TEST SCENARIOS:")
    print("=" * 40)
    print("1. Index dssi-day3-ollama (Flask Python project)")
    print("   Expected: Flask, Python dependencies from requirements.txt")
    print("   Should detect: flask, subprocess, json, logging, etc.")
    print()
    print("2. Search queries to test:")
    print("   📍 'project type' - Should show Flask Python project")
    print("   📍 'python dependencies' - Should list Python libraries")
    print("   📍 'flask framework' - Should find Flask-specific features")
    print("   📍 'REST API endpoint' - Should still work with line numbers")
    
    print("\n💻 MANUAL TEST STEPS:")
    print("=" * 40)
    print("1. Run: java -jar target\\indexer-0.0.1-SNAPSHOT.jar")
    print("2. Choose option 6 (Index Codebase)")
    print("3. Choose option 2 (Change indexing directory)")
    print("4. Enter: d:\\Projects\\misoto-indexer\\codebase\\dssi-day3-ollama")
    print("5. Choose option 3 (Clear cache and reindex)")
    print("6. Watch for project analysis output during indexing:")
    print("   🔍 ANALYZING PROJECT TYPE AND DEPENDENCIES")
    print("   📁 Project Type: Flask")
    print("   📦 Dependencies found: X")
    print("   🛠️ Frameworks detected: Y")
    print("7. Test semantic searches:")
    print("   - Choose option 3 (Semantic Code Search)")
    print("   - Try: 'project type'")
    print("   - Try: 'python dependencies'")
    print("   - Try: 'flask framework'")
    print("   - Try: 'REST API endpoint' (should still show line numbers)")
    
    print("\n🎯 EXPECTED RESULTS:")
    print("=" * 40)
    print("For dssi-day3-ollama project:")
    print("✅ Project Type: Flask")
    print("✅ Language: Python")
    print("✅ Dependencies: flask, subprocess, json, logging, os, argparse, etc.")
    print("✅ Frameworks: Flask, Flask Routing, Flask JSON API")
    print("✅ Search results should include:")
    print("   - Project analysis summary with technology stack")
    print("   - Python dependencies list with versions")
    print("   - REST API endpoints with line numbers (31, 36, 126, 153, 181)")
    
    print("\n🚀 BENEFITS:")
    print("=" * 40)
    print("🎯 Smarter Search: Query 'python libraries' to find project dependencies")
    print("🏗️ Technology Aware: System knows the project stack and frameworks")
    print("📊 Project Overview: Get complete technology analysis in search results")
    print("🔗 Context Rich: Dependencies and project type inform better search relevance")
    
    print(f"\n⚡ Ready to test the enhanced indexer!")
    print("=" * 60)

if __name__ == "__main__":
    main()
