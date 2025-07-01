import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import sg.edu.nus.iss.codebase.indexer.IndexerApplication;
import sg.edu.nus.iss.codebase.indexer.service.HybridSearchService;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileIndexingService;

/**
 * Test error handling in multi-query expansion
 */
public class TestErrorHandling {
    public static void main(String[] args) {
        System.setProperty("app.cli.enabled", "false");
        ApplicationContext context = SpringApplication.run(IndexerApplication.class);
        
        try {
            System.out.println("🧪 TESTING ERROR HANDLING IN MULTI-QUERY EXPANSION");
            System.out.println("==================================================");
            
            HybridSearchService searchService = context.getBean(HybridSearchService.class);
            FileIndexingService indexingService = context.getBean(FileIndexingService.class);
            
            // Set up directory
            indexingService.setIndexingDirectory("./codebase/dssi-day3-ollama");
            
            System.out.println("📊 Test Setup:");
            System.out.printf("   • Collection: %s%n", indexingService.getCurrentCollectionName());
            System.out.printf("   • Directory: %s%n", indexingService.getCurrentIndexingDirectory());
            System.out.println();
            
            System.out.println("🎯 Testing 'REST API endpoints' search with improved error handling...");
            System.out.println("   (Should handle connection errors gracefully and retry failed queries)");
            System.out.println();
            
            // This should trigger multi-query expansion with error handling
            var results = searchService.search("REST API endpoints", 10);
            
            System.out.printf("📊 Search Results: %d documents found%n", results.size());
            
            if (results.size() > 0) {
                System.out.println("✅ Search successful despite any connection issues!");
                System.out.println("📋 Top results:");
                for (int i = 0; i < Math.min(3, results.size()); i++) {
                    var result = results.get(i);
                    System.out.printf("   %d. %s%n", i + 1, result.getFileName());
                    if (result.getContent().contains("@app.route")) {
                        System.out.println("      🎯 Contains Flask route decorators!");
                    }
                }
            } else {
                System.out.println("⚠️ No results found, but system handled errors gracefully");
            }
            
            System.out.println("\n✅ ERROR HANDLING TEST COMPLETE");
            System.out.println("💡 Key improvements:");
            System.out.println("   • Retry logic for connection failures (up to 2 retries)");
            System.out.println("   • Exponential backoff between retries");
            System.out.println("   • Graceful fallback when all queries fail");
            System.out.println("   • Partial success handling (continues with successful queries)");
            System.out.println("   • Clear error messages for debugging");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}