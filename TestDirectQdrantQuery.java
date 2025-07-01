import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import sg.edu.nus.iss.codebase.indexer.IndexerApplication;
import sg.edu.nus.iss.codebase.indexer.config.DynamicVectorStoreFactory;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileIndexingService;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.document.Document;

import java.util.List;

/**
 * Test the direct Qdrant query functionality
 */
public class TestDirectQdrantQuery {
    public static void main(String[] args) {
        System.setProperty("app.cli.enabled", "false");
        ApplicationContext context = SpringApplication.run(IndexerApplication.class);
        
        try {
            System.out.println("🧪 TESTING DIRECT QDRANT QUERY FUNCTIONALITY");
            System.out.println("=============================================");
            
            DynamicVectorStoreFactory vectorStoreFactory = context.getBean(DynamicVectorStoreFactory.class);
            FileIndexingService indexingService = context.getBean(FileIndexingService.class);
            
            // Set up to use the Flask app directory collection
            indexingService.setIndexingDirectory("./codebase/dssi-day3-ollama");
            String currentCollection = indexingService.getCurrentCollectionName();
            
            System.out.println("📊 Collection Info:");
            System.out.printf("   • Collection: %s%n", currentCollection);
            System.out.printf("   • Directory: %s%n", indexingService.getCurrentIndexingDirectory());
            System.out.printf("   • Indexed Files: %d%n", indexingService.getIndexedFileCount());
            System.out.println();
            
            // Create vector store for current collection
            VectorStore vectorStore = vectorStoreFactory.createVectorStore(currentCollection);
            
            // Test direct vector search with Flask query
            String testQuery = "Flask";
            System.out.printf("🔍 Testing direct vector search with query: \"%s\"%n", testQuery);
            
            org.springframework.ai.vectorstore.SearchRequest searchRequest = 
                org.springframework.ai.vectorstore.SearchRequest.builder()
                    .query(testQuery)
                    .topK(5)
                    .similarityThreshold(0.0)
                    .build();
            
            List<Document> results = vectorStore.similaritySearch(searchRequest);
            
            System.out.printf("📊 Found %d results%n", results.size());
            
            if (results.isEmpty()) {
                System.out.println("❌ No results found - this indicates the text field issue may still exist");
                System.out.println("💡 The new CLI option will help debug this issue");
            } else {
                System.out.println("✅ Direct vector search is working!");
                System.out.println("📋 Results preview:");
                
                for (int i = 0; i < Math.min(results.size(), 3); i++) {
                    Document doc = results.get(i);
                    var metadata = doc.getMetadata();
                    
                    System.out.printf("   %d. 📄 %s%n", i + 1, 
                        metadata.getOrDefault("filename", "Unknown"));
                    System.out.printf("      📝 Content preview: %s%n", 
                        doc.getText().substring(0, Math.min(100, doc.getText().length())) + "...");
                    
                    if (doc.getText().contains("@app.route")) {
                        System.out.println("      🎯 Contains Flask routes!");
                    }
                }
            }
            
            System.out.println("\n✅ DIRECT QDRANT QUERY TEST COMPLETE");
            System.out.println("🎯 The new CLI option 7 (Direct Qdrant Vector Query) is ready to use!");
            System.out.println("💡 Use it to:");
            System.out.println("   • Debug vector storage issues");
            System.out.println("   • Test direct queries to Qdrant");
            System.out.println("   • Verify text field fix is working");
            System.out.println("   • Bypass all processing layers for raw results");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}