import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import sg.edu.nus.iss.codebase.indexer.IndexerApplication;
import sg.edu.nus.iss.codebase.indexer.service.HybridSearchService;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileIndexingService;

import java.lang.reflect.Method;

/**
 * Simple test to verify multi-query expansion trigger detection
 */
public class TestMultiQuerySimple {
    public static void main(String[] args) {
        System.setProperty("app.cli.enabled", "false");
        ApplicationContext context = SpringApplication.run(IndexerApplication.class);
        
        try {
            System.out.println("🧪 TESTING MULTI-QUERY EXPANSION TRIGGER DETECTION");
            System.out.println("==================================================");
            
            HybridSearchService searchService = context.getBean(HybridSearchService.class);
            FileIndexingService indexingService = context.getBean(FileIndexingService.class);
            
            // Set up directory
            indexingService.setIndexingDirectory("./codebase/dssi-day3-ollama");
            
            // Use reflection to test the private shouldUseMultiQueryExpansion method
            Method shouldUseMethod = HybridSearchService.class.getDeclaredMethod("shouldUseMultiQueryExpansion", String.class);
            shouldUseMethod.setAccessible(true);
            
            // Test queries that should trigger multi-query expansion
            String[] triggerQueries = {
                "REST API endpoints",
                "api endpoints", 
                "all endpoints",
                "list endpoints",
                "find endpoints",
                "show endpoints",
                "web api",
                "flask routes",
                "spring endpoints"
            };
            
            // Test queries that should NOT trigger multi-query expansion
            String[] normalQueries = {
                "Flask web application",
                "database connection",
                "authentication logic",
                "error handling",
                "user management"
            };
            
            System.out.println("✅ Testing TRIGGER queries (should activate multi-query expansion):");
            for (String query : triggerQueries) {
                boolean shouldTrigger = (Boolean) shouldUseMethod.invoke(searchService, query);
                System.out.printf("   '%s': %s%n", query, shouldTrigger ? "✅ TRIGGERS" : "❌ NO TRIGGER");
            }
            
            System.out.println("\n✅ Testing NORMAL queries (should NOT activate multi-query expansion):");
            for (String query : normalQueries) {
                boolean shouldTrigger = (Boolean) shouldUseMethod.invoke(searchService, query);
                System.out.printf("   '%s': %s%n", query, shouldTrigger ? "❌ TRIGGERS (unexpected)" : "✅ NO TRIGGER");
            }
            
            System.out.println("\n🎯 QUICK SEARCH TEST:");
            System.out.println("Testing 'REST API endpoints' search (should show multi-query expansion in logs)...");
            
            // This should trigger the multi-query expansion and show debug output
            var results = searchService.search("REST API endpoints", 10);
            System.out.printf("📊 Search completed: %d results%n", results.size());
            
            if (results.size() > 0) {
                System.out.println("✅ Multi-query expansion appears to be working!");
                System.out.println("📋 Top results:");
                for (int i = 0; i < Math.min(3, results.size()); i++) {
                    var result = results.get(i);
                    System.out.printf("   %d. %s%n", i + 1, result.getFileName());
                }
            } else {
                System.out.println("❌ No results returned - check logs for multi-query expansion activity");
            }
            
            System.out.println("\n✅ TEST COMPLETE");
            System.out.println("💡 Check the logs above for multi-query expansion debug messages");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}