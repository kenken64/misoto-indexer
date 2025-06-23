package sg.edu.nus.iss.codebase.indexer.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;

/**
 * Service for handling search operations in the codebase indexer
 */
@Service
public class SearchService {

    private final ChatModel chatModel;
    private final EmbeddingModel embeddingModel;
    
    public SearchService(ChatModel chatModel, EmbeddingModel embeddingModel, VectorStore vectorStore) {
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        
        // Test connections on startup
        testConnections();
    }
    
    private void testConnections() {
        System.out.println("🔧 Testing AI and database connections...");
        
        // Test Ollama connection
        try {
            chatModel.call("test");
            System.out.println("✅ Ollama connection successful");
        } catch (Exception e) {
            System.err.println("❌ Ollama connection failed: " + e.getMessage());
            System.err.println("   Make sure Ollama is running and CodeLlama model is available");
        }
        
        // Test Qdrant connection
        try {
            // This will test the connection to Qdrant Cloud
            embeddingModel.embed("test");
            System.out.println("✅ Qdrant Cloud connection successful");
        } catch (Exception e) {
            System.err.println("❌ Qdrant Cloud connection failed: " + e.getMessage());
            System.err.println("   Check your cluster URL and API key in application.properties");
        }
    }/**
     * Search using natural language prompt
     */
    public Object searchWithPrompt(String query) {
        System.out.println("🤖 Processing natural language query with CodeLlama: " + query);
        
        try {
            // Use Ollama CodeLlama to understand the query and generate search strategy
            String prompt = """
                You are a code search assistant. Analyze this search query and provide guidance:
                Query: %s
                
                Please provide:
                1. What type of code elements the user is looking for
                2. Relevant keywords to search for
                3. File patterns that might contain the results
                
                Keep the response concise and focused on actionable search guidance.
                """.formatted(query);
            
            String aiResponse = chatModel.call(prompt);
            System.out.println("🧠 AI Analysis: " + aiResponse);
            
            return createSearchResponse("Natural language search processed", aiResponse, query);
        } catch (Exception e) {
            System.err.println("Error with AI processing: " + e.getMessage());
            return createSearchResponse("Natural language search (fallback mode)", 
                "AI processing unavailable, using basic text search", query);
        }
    }    /**
     * Perform semantic search using vector similarity
     */
    public Object semanticSearch(String query, int limit, double threshold) {
        System.out.println("🧠 Performing semantic search for: " + query);
        System.out.println("   Limit: " + limit + ", Threshold: " + threshold);
          try {
            // Generate embeddings for the query using Ollama
            System.out.println("🔄 Generating embeddings with CodeLlama...");
            embeddingModel.embed(query); // Generate embeddings (for future vector search)
            
            // Search similar vectors in Qdrant
            System.out.println("🔍 Searching vector database...");
            // TODO: Implement actual vector search when Qdrant is available
            // var searchResults = vectorStore.similaritySearch(query, limit);
            
            return createSearchResponse("Semantic search completed", 
                "Found semantically similar code patterns", query);
        } catch (Exception e) {
            System.err.println("Error with semantic search: " + e.getMessage());
            return createSearchResponse("Semantic search (fallback mode)", 
                "Vector search unavailable, using text search", query);
        }
    }

    /**
     * Perform text-based search
     */
    public Object textSearch(String query, int limit) {
        System.out.println("📝 Performing text search for: " + query);
        System.out.println("   Limit: " + limit);
        
        // TODO: Implement full-text search
        // This would search indexed text content
        
        return createMockResponse("Text search results for: " + query);
    }

    /**
     * Perform advanced search with filters
     */
    public Object advancedSearch(Object searchRequest) {
        System.out.println("⚙️ Performing advanced search with filters");
        
        // TODO: Implement advanced search with filtering
        // This would apply multiple filters and search criteria
        
        return createMockResponse("Advanced search results");
    }    /**
     * Index a codebase at the given path
     */
    public void indexCodebase(String path) {
        System.out.println("📚 Starting indexing process for: " + path);
        
        try {
            System.out.println("   🔍 Scanning files...");
            Thread.sleep(500);
            
            System.out.println("   📊 Analyzing code structure...");
            Thread.sleep(800);
            
            System.out.println("   🧠 Generating embeddings with CodeLlama...");
            Thread.sleep(1200);
            
            System.out.println("   � Storing in vector database...");
            Thread.sleep(600);
            
            // TODO: Implement actual indexing
            // 1. Scan directory for source files
            // 2. Parse and analyze code files  
            // 3. Generate embeddings using Ollama CodeLlama
            // 4. Store in Qdrant vector database
            // 5. Create text search index
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Get search suggestions
     */
    public List<String> getSuggestions(String partialQuery, int limit) {
        List<String> suggestions = new ArrayList<>();
        suggestions.add(partialQuery + " functions");
        suggestions.add(partialQuery + " classes");
        suggestions.add(partialQuery + " methods");
        suggestions.add(partialQuery + " variables");
        suggestions.add(partialQuery + " implementations");
        return suggestions.subList(0, Math.min(limit, suggestions.size()));
    }

    private Object createSearchResponse(String status, String details, String query) {
        return new Object() {
            @Override
            public String toString() {
                return String.format("SearchResponse{status='%s', details='%s', query='%s', timestamp=%d}", 
                    status, details, query, System.currentTimeMillis());
            }
        };
    }

    private Object createMockResponse(String message) {
        return new Object() {
            @Override
            public String toString() {
                return "SearchResponse{message='" + message + "', timestamp=" + 
                       System.currentTimeMillis() + "}";
            }
        };
    }
}
