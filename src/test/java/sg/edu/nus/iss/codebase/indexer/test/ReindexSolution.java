package sg.edu.nus.iss.codebase.indexer.test;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import sg.edu.nus.iss.codebase.indexer.IndexerApplication;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileIndexingService;

/**
 * Solution script to properly index dssi-day3-ollama directory
 * This will solve the collection issue by re-indexing with correct collection name
 */
public class ReindexSolution {
    
    public static void main(String[] args) {
        System.out.println("=== RE-INDEXING SOLUTION FOR dssi-day3-ollama ===");
        System.out.println("This will fix the collection issue by properly indexing");
        System.out.println("the dssi-day3-ollama directory with the correct collection name.");
        System.out.println("===================================================");
        
        try {
            // Start Spring Boot application
            ConfigurableApplicationContext context = SpringApplication.run(IndexerApplication.class, args);
            
            // Get the indexing service
            FileIndexingService indexingService = context.getBean(FileIndexingService.class);
            
            // The problematic directory
            String dssiDir = "d:\\Projects\\misoto-indexer\\codebase\\dssi-day3-ollama";
            
            System.out.println("🔧 Step 1: Setting indexing directory and collection...");
            System.out.println("Directory: " + dssiDir);
            
            // This will:
            // 1. Set the directory
            // 2. Generate the correct collection name (codebase-index-dssi-day3-ollama)
            // 3. Set the cache file name
            // 4. Start indexing automatically
            indexingService.setIndexingDirectoryWithCollection(dssiDir);
            
            System.out.println("✅ Directory set and indexing started!");
            System.out.println("Current collection: " + indexingService.getCurrentCollectionName());
            System.out.println("Current directory: " + indexingService.getCurrentIndexingDirectory());
            
            // Wait for indexing to complete
            System.out.println("⏳ Waiting for indexing to complete...");
            
            // Check status periodically
            int attempts = 0;
            while (!indexingService.isIndexingComplete() && attempts < 60) {
                Thread.sleep(2000); // Wait 2 seconds
                attempts++;
                
                if (attempts % 5 == 0) { // Every 10 seconds
                    System.out.printf("   Progress: %.1f%% (%d/%d files)%n", 
                        indexingService.getIndexingProgress() * 100,
                        indexingService.getIndexedFileCount(),
                        indexingService.getTotalFileCount());
                }
            }
            
            if (indexingService.isIndexingComplete()) {
                System.out.println("✅ Indexing completed successfully!");
                System.out.printf("   Total files indexed: %d%n", indexingService.getIndexedFileCount());
                System.out.printf("   Collection: %s%n", indexingService.getCurrentCollectionName());
                System.out.println("\n🎉 SOLUTION COMPLETE!");
                System.out.println("Now when you search for 'gradient_accumulation_steps', it should:");
                System.out.println("1. Show Collection: codebase-index-dssi-day3-ollama");
                System.out.println("2. Find the correct value: gradient_accumulation_steps = 4");
                System.out.println("3. Show the proper file content from text_to_sql_train.py");
            } else {
                System.out.println("⚠️ Indexing did not complete within timeout");
                System.out.printf("   Current progress: %.1f%%\\n", indexingService.getIndexingProgress() * 100);
            }
            
            context.close();
            
        } catch (Exception e) {
            System.err.println("❌ Solution failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
