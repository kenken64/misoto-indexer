import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import sg.edu.nus.iss.codebase.indexer.IndexerApplication;
import sg.edu.nus.iss.codebase.indexer.service.HybridSearchService;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileIndexingService;

/**
 * Debug line matching to understand why inaccurate results are showing
 */
public class DebugLineMatching {
    public static void main(String[] args) {
        System.setProperty("app.cli.enabled", "false");
        ApplicationContext context = SpringApplication.run(IndexerApplication.class);
        
        try {
            System.out.println("🔍 DEBUGGING LINE MATCHING ACCURACY");
            System.out.println("==================================");
            
            HybridSearchService searchService = context.getBean(HybridSearchService.class);
            FileIndexingService indexingService = context.getBean(FileIndexingService.class);
            
            // Index Python project
            indexingService.setIndexingDirectory("./codebase/dssi-day3-ollama");
            
            System.out.println("📊 Project Info:");
            System.out.printf("   • Directory: %s%n", indexingService.getCurrentIndexingDirectory());
            System.out.printf("   • Collection: %s%n", indexingService.getCurrentCollectionName());
            System.out.println();
            
            // Test search with expected results
            System.out.println("🎯 Testing specific search that should find app.py");
            System.out.println("Expected findings:");
            System.out.println("   • app.py should be found (contains Flask imports and @app.route)");
            System.out.println("   • Line 7: from flask import Flask, render_template, request, jsonify, flash");
            System.out.println("   • Line 31: @app.route('/')");
            System.out.println("   • Line 36: @app.route('/api/generate-sql', methods=['POST'])");
            System.out.println();
            
            // Test Flask import search
            System.out.println("🔍 Searching for 'from flask import'...");
            var results = searchService.search("from flask import", 3);
            
            System.out.printf("📊 Found %d results%n", results.size());
            System.out.println();
            
            for (int i = 0; i < results.size(); i++) {
                var result = results.get(i);
                System.out.printf("Result %d:%n", i + 1);
                System.out.printf("   📄 File: %s%n", result.getFileName());
                System.out.printf("   📁 Path: %s%n", result.getFilePath());
                System.out.printf("   📊 Score: %.3f%n", result.getRelevanceScore());
                System.out.printf("   🔍 Search Type: %s%n", result.getSearchType());
                
                // Check line matches
                var lineMatches = result.getLineMatches();
                if (lineMatches != null && !lineMatches.isEmpty()) {
                    System.out.printf("   🎯 Line Matches (%d found):%n", lineMatches.size());
                    for (int j = 0; j < lineMatches.size(); j++) {
                        var lineMatch = lineMatches.get(j);
                        System.out.printf("      Line %d: %s%n", 
                            lineMatch.getLineNumber(), 
                            lineMatch.getLineContent());
                        System.out.printf("      Matched term: '%s'%n", lineMatch.getMatchedTerm());
                    }
                } else {
                    System.out.println("   ⚠️ No line matches found");
                }
                
                // Show content info
                String content = result.getContent();
                if (content != null) {
                    System.out.printf("   📝 Content length: %d characters%n", content.length());
                    System.out.printf("   📝 Content preview: %s...%n", 
                        content.substring(0, Math.min(100, content.length())).replace("\n", "\\n"));
                }
                
                System.out.println();
            }
            
            // Check if app.py was found
            boolean foundAppPy = results.stream()
                .anyMatch(r -> r.getFileName().equals("app.py"));
                
            if (foundAppPy) {
                System.out.println("✅ app.py was found in results");
            } else {
                System.out.println("❌ app.py was NOT found - this is the problem!");
                System.out.println("💡 Need to investigate why app.py is not being indexed or found");
            }
            
            System.out.println();
            System.out.println("🔍 ANALYSIS:");
            System.out.println("   • Are the right files being found?");
            System.out.println("   • Are line numbers accurate?");
            System.out.println("   • Is the content relevant to the search?");
            System.out.println("   • Are duplicate results appearing?");
            
        } catch (Exception e) {
            System.err.println("❌ Debug failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}