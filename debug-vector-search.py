#!/usr/bin/env python3
"""
Debug script to test vector search and check document types in the index.
This script will help debug why project analysis documents aren't appearing in search results.
"""

import subprocess
import json
import time
import sys

def run_java_search(query, max_results=10):
    """Run semantic search via the Java application"""
    print(f"🔍 Testing semantic search for: '{query}'")
    print("=" * 60)
    
    try:
        # Run the Java application and perform semantic search
        process = subprocess.Popen([
            'java', '-jar', 'target/indexer-0.0.1-SNAPSHOT.jar'
        ], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        
        # Send commands to perform semantic search
        input_commands = f"""3
{query}
8
"""
        
        stdout, stderr = process.communicate(input=input_commands, timeout=60)
        
        print("📊 Search Results:")
        print(stdout)
        
        if stderr:
            print("⚠️ Errors/Warnings:")
            print(stderr)
            
        return stdout, stderr
        
    except subprocess.TimeoutExpired:
        print("❌ Search timed out")
        process.kill()
        return None, "Timeout"
    except Exception as e:
        print(f"❌ Error running search: {e}")
        return None, str(e)

def analyze_search_results(output):
    """Analyze search output to identify document types returned"""
    print("\n📈 ANALYSIS:")
    print("-" * 40)
    
    if not output:
        print("❌ No output to analyze")
        return
    
    # Look for document type indicators in the output
    project_analysis_found = "projectAnalysis" in output or "Project Type:" in output
    dependencies_found = "dependencies" in output or "Dependencies for" in output
    framework_docs_found = "frameworkDocumentation" in output or "Framework documentation" in output
    code_found = any(indicator in output.lower() for indicator in [".py", ".java", ".js", "function", "class", "def "])
    
    print(f"🔍 Project Analysis documents found: {'✅' if project_analysis_found else '❌'}")
    print(f"📦 Dependencies documents found: {'✅' if dependencies_found else '❌'}")
    print(f"📚 Framework docs found: {'✅' if framework_docs_found else '❌'}")
    print(f"💻 Code files found: {'✅' if code_found else '❌'}")
    
    # Count total results
    result_count = output.count("File:") + output.count("file:")
    print(f"📊 Total results returned: {result_count}")
    
    return {
        'project_analysis': project_analysis_found,
        'dependencies': dependencies_found,
        'framework_docs': framework_docs_found,
        'code': code_found,
        'total_results': result_count
    }

def main():
    print("🐛 DEBUG: Vector Search Document Types")
    print("=" * 60)
    
    # Test different types of queries that should return project analysis docs
    test_queries = [
        # Should return project analysis and framework docs
        "@app.route('/api/status')",
        "flask endpoints",
        "python dependencies",
        "project type framework",
        "REST API endpoints",
        "flask framework documentation",
        "project analysis dependencies",
        "what frameworks are used",
        "app.route decorator",
        "spring boot rest controller"
    ]
    
    results_summary = {}
    
    for query in test_queries:
        print(f"\n{'='*80}")
        print(f"🧪 TEST: {query}")
        print(f"{'='*80}")
        
        output, error = run_java_search(query)
        analysis = analyze_search_results(output)
        results_summary[query] = analysis
        
        print("\n" + "="*40)
        time.sleep(2)  # Brief pause between tests
    
    # Final summary
    print(f"\n{'='*80}")
    print("📋 FINAL SUMMARY")
    print(f"{'='*80}")
    
    total_tests = len(test_queries)
    project_analysis_hits = sum(1 for r in results_summary.values() if r and r.get('project_analysis', False))
    dependencies_hits = sum(1 for r in results_summary.values() if r and r.get('dependencies', False))
    framework_hits = sum(1 for r in results_summary.values() if r and r.get('framework_docs', False))
    
    print(f"📊 Project Analysis docs found: {project_analysis_hits}/{total_tests} queries")
    print(f"📦 Dependencies docs found: {dependencies_hits}/{total_tests} queries")
    print(f"📚 Framework docs found: {framework_hits}/{total_tests} queries")
    
    if project_analysis_hits == 0:
        print("\n❌ ISSUE IDENTIFIED: No project analysis documents found in any search!")
        print("   This suggests the issue is either:")
        print("   1. Documents are not being indexed properly")
        print("   2. Documents are being filtered out during search")
        print("   3. Vector search is not finding semantic matches")
        
    elif project_analysis_hits < total_tests // 2:
        print(f"\n⚠️  LOW HIT RATE: Only {project_analysis_hits}/{total_tests} queries returned project analysis docs")
        print("   This suggests the semantic matching or prioritization needs improvement")
        
    else:
        print(f"\n✅ GOOD COVERAGE: {project_analysis_hits}/{total_tests} queries returned project analysis docs")

if __name__ == "__main__":
    main()
