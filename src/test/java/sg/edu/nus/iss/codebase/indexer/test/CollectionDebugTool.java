package sg.edu.nus.iss.codebase.indexer.test;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import sg.edu.nus.iss.codebase.indexer.IndexerApplication;
import sg.edu.nus.iss.codebase.indexer.config.DynamicVectorStoreFactory;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileIndexingService;
import org.springframework.ai.vectorstore.VectorStore;

/**
 * Debug tool to check Qdrant collections and identify the collection issue
 */
public class CollectionDebugTool {
    
    public static void main(String[] args) {
        System.out.println("=== COLLECTION DEBUG TOOL ===");
        System.out.println("Investigating collection naming issue for dssi-day3-ollama");
        System.out.println("==============================================");
        
        try {
            // Start Spring Boot application
            ConfigurableApplicationContext context = SpringApplication.run(IndexerApplication.class, args);
            
            // Get beans
            FileIndexingService fileIndexingService = context.getBean(FileIndexingService.class);
            DynamicVectorStoreFactory vectorStoreFactory = context.getBean(DynamicVectorStoreFactory.class);
            
            // Test 1: Check what collection is currently being used
            String currentCollection = fileIndexingService.getCurrentCollectionName();
            System.out.println("1. Current collection name: " + currentCollection);
            System.out.println("   Expected for dssi-day3-ollama: codebase-index-dssi-day3-ollama");
            System.out.println("   Match: " + (currentCollection.equals("codebase-index-dssi-day3-ollama") ? "✅ YES" : "❌ NO"));
            
            // Test 2: Set directory and check collection change
            System.out.println("\n2. Setting dssi-day3-ollama directory...");
            String testDir = "d:\\Projects\\misoto-indexer\\codebase\\dssi-day3-ollama";
            fileIndexingService.setIndexingDirectoryWithCollection(testDir);
            
            String newCollection = fileIndexingService.getCurrentCollectionName();
            System.out.println("   Collection after setting directory: " + newCollection);
            System.out.println("   Expected: codebase-index-dssi-day3-ollama");
            System.out.println("   Match: " + (newCollection.equals("codebase-index-dssi-day3-ollama") ? "✅ YES" : "❌ NO"));
            
            // Test 3: Try to access both collections
            System.out.println("\n3. Testing collection access...");
            
            try {
                VectorStore defaultStore = vectorStoreFactory.createVectorStore("codebase-index");
                System.out.println("   ✅ Default collection 'codebase-index' exists and accessible");
                
                // Try a search in the default collection
                var defaultResults = defaultStore.similaritySearch("text_to_sql_train.py");
                System.out.println("   Default collection search results: " + defaultResults.size() + " documents found");
                if (!defaultResults.isEmpty()) {
                    System.out.println("   First result source: " + defaultResults.get(0).getMetadata().get("source"));
                }
                
            } catch (Exception e) {
                System.out.println("   ❌ Default collection 'codebase-index' error: " + e.getMessage());
            }
            
            try {
                VectorStore dynamicStore = vectorStoreFactory.createVectorStore("codebase-index-dssi-day3-ollama");
                System.out.println("   ✅ Dynamic collection 'codebase-index-dssi-day3-ollama' exists and accessible");
                
                // Try a search in the dynamic collection
                var dynamicResults = dynamicStore.similaritySearch("text_to_sql_train.py");
                System.out.println("   Dynamic collection search results: " + dynamicResults.size() + " documents found");
                if (!dynamicResults.isEmpty()) {
                    System.out.println("   First result source: " + dynamicResults.get(0).getMetadata().get("source"));
                }
                
            } catch (Exception e) {
                System.out.println("   ❌ Dynamic collection 'codebase-index-dssi-day3-ollama' error: " + e.getMessage());
            }
            
            // Test 4: Check directory and collection consistency
            System.out.println("\n4. Directory and Collection Consistency Check:");
            String currentDir = fileIndexingService.getCurrentIndexingDirectory();
            System.out.println("   Current directory: " + currentDir);
            System.out.println("   Current collection: " + fileIndexingService.getCurrentCollectionName());
            
            if (currentDir != null && currentDir.contains("dssi-day3-ollama")) {
                System.out.println("   ✅ Directory contains dssi-day3-ollama");
                if (fileIndexingService.getCurrentCollectionName().equals("codebase-index-dssi-day3-ollama")) {
                    System.out.println("   ✅ Collection name matches directory");
                } else {
                    System.out.println("   ❌ Collection name does NOT match directory");
                }
            } else {
                System.out.println("   ❌ Directory does not contain dssi-day3-ollama or is null");
            }
            
            System.out.println("\n=== DIAGNOSIS ===");
            if (currentCollection.equals("codebase-index")) {
                System.out.println("❌ ISSUE FOUND: Application is using default collection 'codebase-index'");
                System.out.println("   This means files were indexed before dynamic collection naming was implemented,");
                System.out.println("   or the search is not using the correct collection.");
                System.out.println("\n💡 SOLUTION: Need to re-index dssi-day3-ollama directory with proper collection naming");
            } else if (currentCollection.equals("codebase-index-dssi-day3-ollama")) {
                System.out.println("✅ Collection naming is working correctly");
                System.out.println("   The issue might be elsewhere (old cached data, etc.)");
            }
            
            context.close();
            
        } catch (Exception e) {
            System.err.println("Debug tool failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
