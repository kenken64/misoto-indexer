import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import sg.edu.nus.iss.codebase.indexer.IndexerApplication;
import sg.edu.nus.iss.codebase.indexer.service.HybridSearchService;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileIndexingService;
import sg.edu.nus.iss.codebase.indexer.dto.SearchRequest;

import java.util.List;

/**
 * Test the multi-query expansion feature for endpoint discovery
 */
public class TestMultiQueryExpansion {
    public static void main(String[] args) {
        System.setProperty("app.cli.enabled", "false");
        ApplicationContext context = SpringApplication.run(IndexerApplication.class);
        
        try {
            System.out.println("🧪 TESTING MULTI-QUERY EXPANSION FOR ENDPOINT DISCOVERY");
            System.out.println("======================================================");
            
            HybridSearchService searchService = context.getBean(HybridSearchService.class);
            FileIndexingService indexingService = context.getBean(FileIndexingService.class);
            
            // Set up to use the Flask app directory collection
            indexingService.setIndexingDirectory("./codebase/dssi-day3-ollama");
            String currentCollection = indexingService.getCurrentCollectionName();
            
            System.out.println("📊 Test Setup:");
            System.out.printf("   • Collection: %s%n", currentCollection);
            System.out.printf("   • Directory: %s%n", indexingService.getCurrentIndexingDirectory());
            System.out.printf("   • Indexed Files: %d%n", indexingService.getIndexedFileCount());
            System.out.println();
            
            // Test 1: Regular query (should NOT trigger multi-query expansion)
            System.out.println("🔍 Test 1: Regular query (no expansion expected)");
            SearchRequest regularRequest = new SearchRequest();
            regularRequest.setQuery("Flask web application");
            regularRequest.setSearchType(SearchRequest.SearchType.SEMANTIC);
            regularRequest.setLimit(5);
            
            var regularResults = searchService.search(regularRequest.getQuery(), regularRequest.getLimit());
            System.out.printf("   Results: %d documents%n", regularResults.size());
            System.out.println();
            
            // Test 2: Endpoint discovery query (SHOULD trigger multi-query expansion)
            System.out.println("🎯 Test 2: REST API endpoints query (expansion expected)");
            SearchRequest endpointRequest = new SearchRequest();
            endpointRequest.setQuery("REST API endpoints");
            endpointRequest.setSearchType(SearchRequest.SearchType.SEMANTIC);
            endpointRequest.setLimit(15);
            
            System.out.println("📋 Expected behavior:");
            System.out.println("   1. Query: @app.route (max results: 10, threshold: 0.0)");
            System.out.println("   2. Query: Flask API endpoints (max results: 15, threshold: 0.0)");
            System.out.println("   3. Query: POST methods JSON (max results: 10, threshold: 0.0)");
            System.out.println("   4. Combine and deduplicate results");
            System.out.println("   5. Apply alternative ranking");
            System.out.println();
            
            var endpointResults = searchService.search(endpointRequest.getQuery(), endpointRequest.getLimit());
            
            System.out.printf("📊 Final Results: %d documents%n", endpointResults.size());
            
            if (!endpointResults.isEmpty()) {
                System.out.println("✅ Multi-query expansion results:");
                for (int i = 0; i < Math.min(endpointResults.size(), 5); i++) {
                    var result = endpointResults.get(i);
                    System.out.printf("   %d. 📄 %s%n", i + 1, result.getFileName());
                    
                    if (result.getContent().contains("@app.route")) {
                        System.out.println("      🎯 Contains Flask route decorators!");
                    }
                    if (result.getContent().contains("POST") || result.getContent().contains("methods")) {
                        System.out.println("      🔗 Contains HTTP method information!");
                    }
                    if (result.getContent().contains("API") || result.getContent().contains("endpoint")) {
                        System.out.println("      🌐 Contains API-related content!");
                    }
                }
            } else {
                System.out.println("❌ No results found - multi-query expansion may not be working");
            }
            
            // Test 3: Other trigger phrases
            System.out.println("\n🔍 Test 3: Other trigger phrases");
            String[] triggerPhrases = {
                "all endpoints",
                "list endpoints", 
                "find endpoints",
                "Flask routes",
                "API endpoints"
            };
            
            for (String phrase : triggerPhrases) {
                SearchRequest triggerRequest = new SearchRequest();
                triggerRequest.setQuery(phrase);
                triggerRequest.setSearchType(SearchRequest.SearchType.SEMANTIC);
                triggerRequest.setLimit(10);
                
                var triggerResults = searchService.search(triggerRequest.getQuery(), triggerRequest.getLimit());
                System.out.printf("   '%s': %d results%n", phrase, triggerResults.size());
            }
            
            System.out.println("\n✅ MULTI-QUERY EXPANSION TEST COMPLETE");
            System.out.println("💡 When you search for 'REST API endpoints' in the CLI, it should:");
            System.out.println("   • Execute 3 targeted queries automatically");
            System.out.println("   • Combine and deduplicate results");
            System.out.println("   • Apply alternative ranking for best results");
            System.out.println("   • Find all Flask endpoints, routes, and API methods");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}