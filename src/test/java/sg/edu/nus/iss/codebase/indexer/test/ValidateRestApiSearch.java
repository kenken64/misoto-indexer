package sg.edu.nus.iss.codebase.indexer.test;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import sg.edu.nus.iss.codebase.indexer.IndexerApplication;
import sg.edu.nus.iss.codebase.indexer.service.HybridSearchService;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileIndexingService;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Test to validate that the improved chunking and search works correctly
 * for finding Flask REST API endpoints in app.py
 */
public class ValidateRestApiSearch {
    
    public static void main(String[] args) {
        System.out.println("=== VALIDATE REST API ENDPOINT SEARCH ===");
        System.out.println("Testing improved chunking and semantic search");
        System.out.println("============================================================");
        
        try {
            // Start Spring Boot application
            ConfigurableApplicationContext context = SpringApplication.run(IndexerApplication.class, args);
            
            // Get services
            FileIndexingService fileIndexingService = context.getBean(FileIndexingService.class);
            HybridSearchService hybridSearchService = context.getBean(HybridSearchService.class);
            
            // Step 1: Set the indexing directory to dssi-day3-ollama
            String targetDir = "d:\\Projects\\misoto-indexer\\codebase\\dssi-day3-ollama";
            System.out.println("1. Setting indexing directory: " + targetDir);
            
            // Verify the directory exists and contains app.py
            File appPy = new File(targetDir, "app.py");
            if (!appPy.exists()) {
                System.err.println("❌ ERROR: app.py not found at " + appPy.getAbsolutePath());
                return;
            }
            
            System.out.println("✅ Found app.py at: " + appPy.getAbsolutePath());
            
            // Read app.py to verify it contains the expected Flask routes
            String appPyContent = Files.readString(Paths.get(appPy.getAbsolutePath()));
            System.out.println("\n2. Checking app.py content for Flask routes:");
            
            String[] expectedRoutes = {
                "@app.route('/')",
                "@app.route('/api/generate-sql', methods=['POST'])",
                "@app.route('/api/validate-sql', methods=['POST'])",
                "@app.route('/api/status')",
                "@app.route('/examples')"
            };
            
            for (String route : expectedRoutes) {
                if (appPyContent.contains(route)) {
                    System.out.println("   ✅ Found: " + route);
                } else {
                    System.out.println("   ❌ Missing: " + route);
                }
            }
            
            // Set the indexing directory
            fileIndexingService.setIndexingDirectoryWithCollection(targetDir);
            String collectionName = fileIndexingService.getCurrentCollectionName();
            System.out.println("\n3. Collection set to: " + collectionName);
            
            // Step 2: Clear cache and reindex to ensure we use the new chunking logic
            System.out.println("\n4. Clearing cache and reindexing with improved chunking...");
            fileIndexingService.clearCacheAndReindex();
            
            // Wait for indexing to complete
            System.out.println("5. Waiting for indexing to complete...");
            Thread.sleep(15000); // Wait 15 seconds for indexing
            
            // Step 3: Test semantic search for "REST API endpoints"
            System.out.println("\n6. Testing semantic search for 'REST API endpoints'...");
            
            HybridSearchService.HybridSearchResult result = hybridSearchService.performHybridSearch(
                "REST API endpoints", 10
            );
            
            System.out.println("\n7. Search Results Analysis:");
            System.out.println("==================================================");
            
            if (result.getTotalResults() == 0) {
                System.out.println("❌ NO RESULTS FOUND");
                System.out.println("This indicates the indexing may not have completed or there's an issue with the search.");
            } else {
                System.out.println("Found " + result.getTotalResults() + " results");
                
                boolean foundAppPy = false;
                boolean foundFlaskRoutes = false;
                
                // Check vector results
                for (int i = 0; i < result.getVectorResults().size(); i++) {
                    var searchResult = result.getVectorResults().get(i);
                    String fileName = searchResult.getMetadata().get("filename").toString();
                    String content = searchResult.getContent();
                    
                    System.out.println("\n" + (i + 1) + ". File: " + fileName);
                    System.out.println("   Content preview: " + content.substring(0, Math.min(content.length(), 100)) + "...");
                    
                    if (fileName.contains("app.py")) {
                        foundAppPy = true;
                        System.out.println("   ✅ Found app.py");
                        
                        // Check if this chunk contains Flask routes
                        if (content.contains("@app.route")) {
                            foundFlaskRoutes = true;
                            System.out.println("   ✅ Contains Flask routes (@app.route)");
                            
                            // Count and list the routes found in this chunk
                            int routeCount = 0;
                            for (String route : expectedRoutes) {
                                if (content.contains(route.split("'")[1])) { // Extract the route path
                                    routeCount++;
                                }
                            }
                            System.out.println("   📊 Route endpoints found in this chunk: " + routeCount);
                        } else {
                            System.out.println("   ⚠️ app.py chunk found but doesn't contain @app.route");
                        }
                    } else {
                        System.out.println("   ℹ️ Different file: " + fileName);
                    }
                }
                
                // Check file results
                for (int i = 0; i < result.getFileResults().size(); i++) {
                    var searchResult = result.getFileResults().get(i);
                    String fileName = searchResult.getFileName();
                    String content = searchResult.getContent();
                    
                    System.out.println("\n" + (i + result.getVectorResults().size() + 1) + ". File: " + fileName);
                    System.out.println("   Content preview: " + content.substring(0, Math.min(content.length(), 100)) + "...");
                    
                    if (fileName.contains("app.py")) {
                        foundAppPy = true;
                        System.out.println("   ✅ Found app.py");
                        
                        // Check if this chunk contains Flask routes
                        if (content.contains("@app.route")) {
                            foundFlaskRoutes = true;
                            System.out.println("   ✅ Contains Flask routes (@app.route)");
                            
                            // Count and list the routes found in this chunk
                            int routeCount = 0;
                            for (String route : expectedRoutes) {
                                if (content.contains(route.split("'")[1])) { // Extract the route path
                                    routeCount++;
                                }
                            }
                            System.out.println("   📊 Route endpoints found in this chunk: " + routeCount);
                        } else {
                            System.out.println("   ⚠️ app.py chunk found but doesn't contain @app.route");
                        }
                    } else {
                        System.out.println("   ℹ️ Different file: " + fileName);
                    }
                }
                
                System.out.println("\n8. 🎯 VALIDATION RESULTS:");
                System.out.println("========================================");
                
                if (foundAppPy && foundFlaskRoutes) {
                    System.out.println("✅ SUCCESS: Semantic search correctly found app.py with Flask routes!");
                    System.out.println("   The improved chunking and query enhancement is working.");
                } else if (foundAppPy && !foundFlaskRoutes) {
                    System.out.println("⚠️ PARTIAL SUCCESS: Found app.py but not the Flask route chunks.");
                    System.out.println("   The chunking logic may need further refinement.");
                } else {
                    System.out.println("❌ FAILURE: Did not find app.py in search results.");
                    System.out.println("   This indicates the search or indexing is not working as expected.");
                }
            }
            
            System.out.println("\n9. 🔍 Next Steps:");
            if (result.getTotalResults() == 0 || !result.getVectorResults().stream()
                    .anyMatch(r -> r.getMetadata().get("filename").toString().contains("app.py")) 
                    && !result.getFileResults().stream()
                    .anyMatch(r -> r.getFileName().contains("app.py"))) {
                System.out.println("   - Check if indexing completed successfully");
                System.out.println("   - Verify the collection contains the app.py file");
                System.out.println("   - Test with a different search query");
                System.out.println("   - Check the embedding model is working correctly");
            } else {
                System.out.println("   - The search is working! You can now test with the CLI.");
                System.out.println("   - Try searching for other programming concepts.");
            }
            
            context.close();
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
