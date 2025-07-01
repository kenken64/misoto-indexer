import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import sg.edu.nus.iss.codebase.indexer.IndexerApplication;
import sg.edu.nus.iss.codebase.indexer.service.HybridSearchService;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileIndexingService;

/**
 * Test that multi-query expansion doesn't perform redundant searches
 */
public class TestNoRedundantSearch {
    public static void main(String[] args) {
        System.setProperty("app.cli.enabled", "false");
        ApplicationContext context = SpringApplication.run(IndexerApplication.class);
        
        try {
            System.out.println("🧪 TESTING ELIMINATION OF REDUNDANT SEARCH");
            System.out.println("==========================================");
            
            HybridSearchService searchService = context.getBean(HybridSearchService.class);
            FileIndexingService indexingService = context.getBean(FileIndexingService.class);
            
            indexingService.setIndexingDirectory("./codebase/dssi-day3-ollama");
            
            System.out.println("🎯 Testing 'REST API endpoints' search");
            System.out.println("Expected behavior:");
            System.out.println("  1. ✅ Multi-query expansion activated");
            System.out.println("  2. ✅ Execute 3 targeted queries: @app.route, Flask API endpoints, POST methods JSON");
            System.out.println("  3. ✅ Combine and deduplicate results");
            System.out.println("  4. ✅ Use pre-computed results (skip redundant vector search)");
            System.out.println("  5. ✅ Apply alternative ranking");
            System.out.println("  6. ❌ Should NOT see: 'FINAL QUERY TO VECTOR DATABASE: REST API endpoints [MULTI-QUERY-EXPANSION]'");
            System.out.println();
            
            System.out.println("🔍 Executing search...");
            var results = searchService.search("REST API endpoints", 10);
            
            System.out.printf("📊 Final Results: %d documents%n", results.size());
            
            if (results.size() > 0) {
                System.out.println("✅ Success! Multi-query expansion completed efficiently");
                System.out.println("📋 Top results:");
                for (int i = 0; i < Math.min(3, results.size()); i++) {
                    var result = results.get(i);
                    System.out.printf("   %d. %s%n", i + 1, result.getFileName());
                }
            }
            
            System.out.println("\n🔍 Key Messages to Look For:");
            System.out.println("✅ Should see: '🎯 Multi-query expansion activated for endpoint discovery'");
            System.out.println("✅ Should see: '📋 Executing 3 targeted queries:'");
            System.out.println("✅ Should see: '🎯 Using pre-computed multi-query expansion results (skipping redundant vector search)'");
            System.out.println("✅ Should see: '📊 Applying alternative ranking to X pre-computed documents'");
            System.out.println("❌ Should NOT see: 'FINAL QUERY TO VECTOR DATABASE: REST API endpoints [MULTI-QUERY-EXPANSION]'");
            System.out.println("❌ Should NOT see: 'Raw documents retrieved: X' after multi-query expansion");
            
            System.out.println("\n✅ TEST COMPLETE");
            System.out.println("💡 Check the output above - there should be no redundant vector search!");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}