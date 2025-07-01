import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import sg.edu.nus.iss.codebase.indexer.IndexerApplication;
import sg.edu.nus.iss.codebase.indexer.service.HybridSearchService;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileIndexingService;

/**
 * Test that regular searches (non-multi-query) still show debug output correctly
 */
public class TestRegularSearch {
    public static void main(String[] args) {
        System.setProperty("app.cli.enabled", "false");
        ApplicationContext context = SpringApplication.run(IndexerApplication.class);
        
        try {
            System.out.println("🧪 TESTING REGULAR SEARCH DEBUG OUTPUT");
            System.out.println("====================================");
            
            HybridSearchService searchService = context.getBean(HybridSearchService.class);
            FileIndexingService indexingService = context.getBean(FileIndexingService.class);
            
            indexingService.setIndexingDirectory("./codebase/dssi-day3-ollama");
            
            System.out.println("🎯 Testing regular search with 'Python function'");
            System.out.println("Expected behavior:");
            System.out.println("  ✅ Should see: 'ORIGINAL QUERY', 'ENHANCED QUERY', 'FINAL QUERY TO VECTOR DATABASE'");
            System.out.println("  ✅ Should see: 'Raw documents retrieved: X'");
            System.out.println("  ❌ Should NOT trigger multi-query expansion");
            System.out.println();
            
            System.out.println("🔍 Executing regular search...");
            var results = searchService.search("Python function", 5);
            
            System.out.printf("📊 Regular Search Results: %d documents found%n", results.size());
            
            if (results.size() > 0) {
                System.out.println("✅ Regular search successful!");
                System.out.println("📋 Top results:");
                for (int i = 0; i < Math.min(2, results.size()); i++) {
                    var result = results.get(i);
                    System.out.printf("   %d. %s%n", i + 1, result.getFileName());
                }
            }
            
            System.out.println("\\n✅ REGULAR SEARCH TEST COMPLETE");
            System.out.println("💡 This search should show normal debug output since it's not endpoint-related");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}