package sg.edu.nus.iss.codebase.indexer.test;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import sg.edu.nus.iss.codebase.indexer.IndexerApplication;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileIndexingService;

/**
 * Force clear collections and reindex dssi-day3-ollama directory
 */
public class ForceClearAndReindex {
    
    public static void main(String[] args) {
        System.out.println("=== FORCE CLEAR AND REINDEX dssi-day3-ollama ===");
        
        try {
            // Start Spring Boot application
            ConfigurableApplicationContext context = SpringApplication.run(IndexerApplication.class, args);
            
            // Get the FileIndexingService
            FileIndexingService fileIndexingService = context.getBean(FileIndexingService.class);
            
            // Step 1: Set the indexing directory to dssi-day3-ollama
            String targetDir = "d:\\Projects\\misoto-indexer\\codebase\\dssi-day3-ollama";
            System.out.println("\n1. Setting indexing directory to: " + targetDir);
            fileIndexingService.setIndexingDirectoryWithCollection(targetDir);
            
            // Verify the collection name
            String collectionName = fileIndexingService.getCurrentCollectionName();
            System.out.println("2. Current collection name: " + collectionName);
            System.out.println("   Expected: codebase-index-dssi-day3-ollama");
            
            // Step 2: Clear cache and reindex (this will delete and recreate the collection)
            System.out.println("\n3. Clearing cache and reindexing...");
            fileIndexingService.clearCacheAndReindex();
            
            System.out.println("\n4. ✅ Clear and reindex process started!");
            System.out.println("   The collection has been deleted and recreated.");
            System.out.println("   Files are being re-indexed with improved chunking logic.");
            
            // Wait a bit for indexing to start
            Thread.sleep(5000);
            
            System.out.println("\n5. 📋 Current status:");
            System.out.println("   - Collection: " + fileIndexingService.getCurrentCollectionName());
            System.out.println("   - Directory: " + fileIndexingService.getCurrentIndexingDirectory());
            
            System.out.println("\n🎯 NEXT STEPS:");
            System.out.println("   Wait for indexing to complete, then test semantic search for 'REST API endpoints'");
            System.out.println("   It should now find the Flask routes in app.py with correct line numbers.");
            
            // Keep running to allow indexing to complete
            System.out.println("\n⏳ Keeping application running for indexing to complete...");
            System.out.println("   Press Ctrl+C to stop when ready to test.");
            
            // Keep the context alive
            Thread.currentThread().join();
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
