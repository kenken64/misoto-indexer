package sg.edu.nus.iss.codebase.indexer.test;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import sg.edu.nus.iss.codebase.indexer.IndexerApplication;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileIndexingService;

/**
 * Diagnostic utility to check indexing status and collection information
 */
public class IndexingDiagnosticTest {
    public static void main(String[] args) {
        System.setProperty("app.cli.enabled", "false");
        ApplicationContext context = SpringApplication.run(IndexerApplication.class);
        
        try {
            FileIndexingService indexingService = context.getBean(FileIndexingService.class);
            
            System.out.println("🔍 INDEXING DIAGNOSTIC");
            System.out.println("======================");
            
            // Get current status
            System.out.println("📊 Current Collection: " + indexingService.getCurrentCollectionName());
            System.out.println("📂 Current Directory: " + indexingService.getCurrentIndexingDirectory());
            System.out.println("📄 Indexed Files: " + indexingService.getIndexedFileCount());
            
            // Get indexing status
            var status = indexingService.getIndexingStatus();
            System.out.println("🔄 Indexing Complete: " + status.isIndexingComplete());
            System.out.println("⏳ Indexing In Progress: " + status.isIndexingInProgress());
            System.out.println("📈 Progress: " + String.format("%.1f%%", status.getProgress() * 100));
            
            // List some collection details
            System.out.println("\n📋 Available Collections:");
            try {
                // This would require adding a method to list collections
                System.out.println("   (Collection listing would require additional implementation)");
            } catch (Exception e) {
                System.out.println("   Error listing collections: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("❌ Diagnostic failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}
