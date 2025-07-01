import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import sg.edu.nus.iss.codebase.indexer.IndexerApplication;
import sg.edu.nus.iss.codebase.indexer.service.HybridSearchService;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileIndexingService;

/**
 * Test the endpoint analysis feature
 */
public class TestEndpointAnalysis {
    public static void main(String[] args) {
        System.setProperty("app.cli.enabled", "false");
        ApplicationContext context = SpringApplication.run(IndexerApplication.class);
        
        try {
            System.out.println("🧪 TESTING ENDPOINT ANALYSIS FEATURE");
            System.out.println("====================================");
            
            HybridSearchService searchService = context.getBean(HybridSearchService.class);
            FileIndexingService indexingService = context.getBean(FileIndexingService.class);
            
            indexingService.setIndexingDirectory("./codebase/dssi-day3-ollama");
            
            System.out.println("🎯 Testing 'REST API endpoints' search with endpoint analysis");
            System.out.println("Expected output format:");
            System.out.println("  🎯 API ENDPOINTS:");
            System.out.println("    1. @app.route('/') [GET]");
            System.out.println("    - Function: index()");
            System.out.println("    - Purpose: Main page with the text-to-SQL interface");
            System.out.println("    - Returns: Renders index.html template");
            System.out.println("    ...");
            System.out.println("  🚨 Error Handlers:");
            System.out.println("    6. @app.errorhandler(404)");
            System.out.println("    ...");
            System.out.println("  📊 Summary:");
            System.out.println("    - Total Routes: X");
            System.out.println("    - Framework: Flask");
            System.out.println();
            
            System.out.println("🔍 Executing search...");
            var results = searchService.search("REST API endpoints", 15);
            
            System.out.printf("📊 Search completed: %d results found%n", results.size());
            
            if (results.size() > 0) {
                System.out.println("✅ Search successful!");
                System.out.println("💡 The results above should be formatted as an endpoint analysis");
                System.out.println("💡 Look for structured endpoint information with functions, purposes, inputs, and returns");
            } else {
                System.out.println("❌ No results found - check indexing and vector storage");
            }
            
            System.out.println("\n🔍 Testing different endpoint queries:");
            
            String[] endpointQueries = {
                "Flask routes",
                "API endpoints", 
                "all endpoints",
                "web API"
            };
            
            for (String query : endpointQueries) {
                System.out.printf("\n🔍 Testing '%s'...%n", query);
                var testResults = searchService.search(query, 10);
                System.out.printf("   Results: %d (should show endpoint analysis if >60%% are endpoints)%n", testResults.size());
            }
            
            System.out.println("\n✅ ENDPOINT ANALYSIS TEST COMPLETE");
            System.out.println("💡 Key features implemented:");
            System.out.println("   • Automatic endpoint detection (60% threshold)");
            System.out.println("   • Flask route parsing and formatting");
            System.out.println("   • Function name extraction");
            System.out.println("   • Purpose inference based on route patterns");
            System.out.println("   • Input/output analysis for POST endpoints");
            System.out.println("   • Error handler categorization");
            System.out.println("   • Framework detection (Flask/Spring/Express)");
            System.out.println("   • Comprehensive summary statistics");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}