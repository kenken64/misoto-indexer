package sg.edu.nus.iss.codebase.indexer.service;

import sg.edu.nus.iss.codebase.indexer.dto.SearchRequest;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileIndexingService;
import sg.edu.nus.iss.codebase.indexer.config.DynamicVectorStoreFactory;
import sg.edu.nus.iss.codebase.indexer.model.IndexingStatus;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class HybridSearchService {
    @Autowired
    private ChatModel chatModel;

    @Autowired
    private DynamicVectorStoreFactory vectorStoreFactory;

    @Autowired
    private FileSearchService fileSearchService;
    @Autowired
    private FileIndexingService indexingService;
    @Autowired
    private ProjectAnalysisService projectAnalysisService;

    /**
     * Hybrid search combining vector search with file-based fallback
     */
    public HybridSearchResult performHybridSearch(String query, int maxResults) {
        System.out.println("🔍 Starting hybrid search for: " + query);

        List<SearchResult> vectorResults = new ArrayList<>();
        List<FileSearchService.SearchResult> fileResults = new ArrayList<>();
        String aiAnalysis = "";
        boolean usedFallback = false;
        try {
            // Try vector search first
            System.out.println("🔍 DEBUG: Checking indexed file count...");
            int indexedFileCount = indexingService.getIndexedFileCount();
            System.out.println("🔍 DEBUG: Indexed file count: " + indexedFileCount);
            
            if (indexedFileCount > 0) {
                String currentCollection = indexingService.getCurrentCollectionName();
                System.out.println(
                        "🎯 Performing vector-based semantic search using collection: " + currentCollection + "...");
                System.out.println("🔍 DEBUG: About to call performVectorSearch...");
                vectorResults = performVectorSearch(query, maxResults);
                System.out.println("🔍 DEBUG: performVectorSearch returned " + vectorResults.size() + " results");
            } else {
                System.out.println("🔍 DEBUG: Skipping vector search - no indexed files");
            }

            // If vector search has limited results or indexing is incomplete, use file
            // search as supplement/fallback
            if (vectorResults.size() < maxResults / 2 || !indexingService.isIndexingComplete()) {
                System.out.println("📂 Supplementing with file-based search...");
                fileResults = fileSearchService.searchInFiles(query);
                usedFallback = true;
            }

            // Generate AI analysis if we have any results
            if (!vectorResults.isEmpty() || !fileResults.isEmpty()) {
                aiAnalysis = generateAIAnalysis(query, vectorResults, fileResults);
            }

        } catch (Exception e) {
            System.err.println("❌ Error in vector search, falling back to file search: " + e.getMessage());
            fileResults = fileSearchService.searchInFiles(query);
            usedFallback = true;
            aiAnalysis = "Search completed using file-based analysis due to vector search limitations.";
        }

        return new HybridSearchResult(vectorResults, fileResults, aiAnalysis, usedFallback);
    }

    private List<SearchResult> performVectorSearch(String query, int maxResults) {
        System.out.println("🔍 DEBUG: performVectorSearch called with query: '" + query + "', maxResults: " + maxResults);
        System.out.println("🔍 DEBUG: Query keywords being sent to vector database: '" + query + "'");
        
        // DEBUG: Temporarily disabled logging suppression to see errors
        // suppressLogging();

        try {
            // Stage 1: Intelligent Framework Analysis using Ollama
            String intelligentQuery = performIntelligentQueryAnalysis(query);
            
            // Only show debug info for non-multi-query expansion
            if (!intelligentQuery.contains("[MULTI-QUERY-EXPANSION]")) {
                System.out.println("🧠 Intelligent query analysis:");
                System.out.println("   Original: \"" + query + "\"");
                System.out.println("   Enhanced: \"" + intelligentQuery + "\"");
            }
            
            // Stage 2: Use the enhanced query for vector search
            List<Document> documents = searchVectorStore(intelligentQuery, maxResults);
            
            List<SearchResult> results = new ArrayList<>();
            for (Document doc : documents) {
                SearchResult result = convertToSearchResultWithScore(doc, query);
                results.add(result);
            }
            
            // Limit results to maxResults
            return results.stream()
                .limit(maxResults)
                .collect(Collectors.toList());
            
        } catch (Exception e) {
            System.err.println("❌ Vector search error: " + e.getMessage());
            e.printStackTrace(); // DEBUG: Show full stack trace
            return new ArrayList<>();
        } finally {
            // DEBUG: Temporarily disabled logging restoration
            // restoreLogging();
        }
    }

    /**
     * Stage 1: Use Ollama to analyze the search query and identify relevant frameworks and terms
     */
    private String performIntelligentQueryAnalysis(String originalQuery) {
        try {
            // Check if this is a multi-query expansion candidate
            if (shouldUseMultiQueryExpansion(originalQuery)) {
                // Store multi-query results for later use
                this.multiQueryResults = performMultiQueryExpansion(originalQuery);
                // Return a special marker to indicate multi-query expansion was used
                return originalQuery + " [MULTI-QUERY-EXPANSION]";
            }
            
            String analysisPrompt = createQueryAnalysisPrompt(originalQuery);
            String ollamaResponse = chatModel.call(analysisPrompt);
            
            if (ollamaResponse != null && !ollamaResponse.trim().isEmpty()) {
                String enhancedQuery = parseFrameworkAnalysisResponse(ollamaResponse, originalQuery);
                System.out.println("🧠 Intelligent query analysis:");
                System.out.println("   Original: \"" + originalQuery + "\"");
                System.out.println("   Enhanced: \"" + enhancedQuery + "\"");
                return enhancedQuery;
            }
        } catch (Exception e) {
            System.err.println("❌ Error in intelligent query analysis: " + e.getMessage());
        }
        
        // Fallback to basic enhancement if Ollama fails
        return enhanceQueryForProgramming(originalQuery);
    }

    /**
     * Check if query should use multi-query expansion
     */
    private boolean shouldUseMultiQueryExpansion(String query) {
        String lowerQuery = query.toLowerCase();
        return lowerQuery.contains("rest api endpoints") ||
               lowerQuery.contains("api endpoints") ||
               lowerQuery.contains("all endpoints") ||
               lowerQuery.contains("list endpoints") ||
               lowerQuery.contains("find endpoints") ||
               lowerQuery.contains("show endpoints") ||
               lowerQuery.contains("web api") ||
               lowerQuery.contains("flask routes") ||
               lowerQuery.contains("spring endpoints");
    }

    /**
     * Perform multi-query expansion for endpoint discovery
     * @return List of all documents found from the multi-query search
     */
    private List<Document> performMultiQueryExpansion(String originalQuery) {
        System.out.println("🎯 Multi-query expansion activated for endpoint discovery");
        System.out.println("📋 Executing 3 targeted queries:");
        
        try {
            // Query 1: Find route decorators
            System.out.println("   1. Searching for route decorators: @app.route");
            List<Document> routeDecorators = searchVectorStoreWithThreshold("@app.route", 10, 0.0);
            
            // Query 2: Find API-related content  
            System.out.println("   2. Searching for Flask API content: Flask API endpoints");
            List<Document> apiContent = searchVectorStoreWithThreshold("Flask API endpoints", 15, 0.0);
            
            // Query 3: Find POST method implementations
            System.out.println("   3. Searching for POST methods: POST methods JSON");
            List<Document> postMethods = searchVectorStoreWithThreshold("POST methods JSON", 10, 0.0);
            
            // Combine and deduplicate results
            Set<String> seenDocuments = new HashSet<>();
            List<Document> combinedResults = new ArrayList<>();
            
            // Add results with deduplication based on content
            addUniqueDocuments(routeDecorators, combinedResults, seenDocuments);
            addUniqueDocuments(apiContent, combinedResults, seenDocuments);
            addUniqueDocuments(postMethods, combinedResults, seenDocuments);
            
            System.out.printf("📊 Multi-query results: %d route decorators + %d API content + %d POST methods = %d unique documents%n",
                routeDecorators.size(), apiContent.size(), postMethods.size(), combinedResults.size());
            
            // Provide feedback on successful queries
            int successfulQueries = (routeDecorators.isEmpty() ? 0 : 1) + 
                                   (apiContent.isEmpty() ? 0 : 1) + 
                                   (postMethods.isEmpty() ? 0 : 1);
            System.out.printf("✅ %d out of 3 queries successful%n", successfulQueries);
            
            // Return all combined documents directly
            return combinedResults;
            
        } catch (Exception e) {
            System.err.println("❌ Multi-query expansion failed: " + e.getMessage());
            System.err.println("💡 Returning empty results due to error.");
            return new ArrayList<>();
        }
    }
    
    // Store multi-query results
    private List<Document> multiQueryResults = null;
    
    /**
     * Add unique documents to results based on content similarity
     */
    private int addUniqueDocuments(List<Document> source, List<Document> target, Set<String> seenDocuments) {
        int addedCount = 0;
        for (Document doc : source) {
            String signature = createDocumentSignature(doc);
            if (!seenDocuments.contains(signature)) {
                seenDocuments.add(signature);
                target.add(doc);
                addedCount++;
            }
        }
        return addedCount;
    }
    
    /**
     * Create a signature for document deduplication
     */
    private String createDocumentSignature(Document doc) {
        String filename = (String) doc.getMetadata().getOrDefault("filename", "");
        String chunk = (String) doc.getMetadata().getOrDefault("chunk", "");
        String content = doc.getText().substring(0, Math.min(100, doc.getText().length()));
        return filename + ":" + chunk + ":" + content.hashCode();
    }
    
    /**
     * Search vector store with specific threshold and retry logic
     */
    private List<Document> searchVectorStoreWithThreshold(String query, int maxResults, double threshold) {
        int maxRetries = 2;
        int retryCount = 0;
        
        while (retryCount <= maxRetries) {
            try {
                // Get the dynamic VectorStore for the current collection
                String currentCollection = indexingService.getCurrentCollectionName();
                VectorStore dynamicVectorStore = vectorStoreFactory.createVectorStore(currentCollection);
                
                org.springframework.ai.vectorstore.SearchRequest searchRequest = 
                    org.springframework.ai.vectorstore.SearchRequest.builder()
                        .query(query)
                        .topK(maxResults)
                        .similarityThreshold(threshold)
                        .build();
                
                List<Document> results = dynamicVectorStore.similaritySearch(searchRequest);
                
                // Success - return results
                if (retryCount > 0) {
                    System.out.println("✅ Vector search succeeded on retry " + retryCount + " for query: " + query);
                }
                return results;
                
            } catch (Exception e) {
                retryCount++;
                boolean isConnectionError = e.getMessage() != null && 
                    (e.getMessage().contains("UNAVAILABLE") || 
                     e.getMessage().contains("io exception") ||
                     e.getMessage().contains("Connection refused"));
                
                if (retryCount <= maxRetries && isConnectionError) {
                    System.out.printf("⚠️ Vector search attempt %d failed for '%s': %s. Retrying...%n", 
                        retryCount, query, e.getMessage());
                    
                    try {
                        Thread.sleep(1000 * retryCount); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    // Final failure or non-connection error
                    if (isConnectionError) {
                        System.err.printf("❌ Vector search failed for '%s' after %d attempts: %s%n", 
                            query, retryCount, e.getMessage());
                        System.err.println("💡 This may be a temporary network issue. The search will continue with other queries.");
                    } else {
                        System.err.printf("❌ Vector search failed for '%s': %s%n", query, e.getMessage());
                    }
                    break;
                }
            }
        }
        
        return new ArrayList<>();
    }

    /**
     * Create a prompt to analyze the search query and identify relevant frameworks
     */
    private String createQueryAnalysisPrompt(String query) {
        return String.format("""
            You are an expert software architect analyzing a code search query. Your task is to identify relevant frameworks and suggest specific syntax patterns that should be searched.
            
            SEARCH QUERY: "%s"
            
            Please analyze this query and provide:
            1. FRAMEWORKS that are most relevant to this query
            2. SPECIFIC SYNTAX/PATTERNS the user is likely looking for
            3. RELATED CONCEPTS that should be included in the search
            
            Consider these framework patterns and their syntax:
            
            REST API ENDPOINTS:
            - Flask: @app.route('/path'), @app.route('/path', methods=['POST']), def function_name():
            - Spring Boot: @RestController, @GetMapping("/path"), @PostMapping("/path"), @RequestMapping
            - Express.js: app.get('/path', handler), app.post('/path', handler), router.use()
            - Django: urlpatterns, path('', view), def view(request):
            
            DATABASE ACCESS:
            - Flask-SQLAlchemy: db.Model, db.Column, db.relationship, query.filter()
            - Spring Data JPA: @Repository, @Entity, findBy, @Query
            - Mongoose: Schema, model, find(), save()
            
            AUTHENTICATION/SECURITY:
            - Flask: session, login_required, @login_required
            - Spring Security: @PreAuthorize, @Secured, SecurityConfig
            - JWT: token, authenticate, authorize
            
            TESTING FRAMEWORKS:
            - pytest: def test_, assert, fixture, @pytest.mark
            - JUnit: @Test, @Before, @After, assertEquals
            - Jest: test(), expect(), beforeEach()
            
            FRONTEND FRAMEWORKS:
            - React: useState, useEffect, componentDidMount, JSX
            - Vue: v-model, v-for, computed, methods
            - Angular: ngFor, ngIf, @Component, @Injectable
            
            Based on the search query, identify:
            1. Which frameworks are most relevant
            2. What specific syntax patterns the user is likely searching for
            3. What additional concepts should be included
            
            Format your response as:
            FRAMEWORKS: [comma-separated list of relevant frameworks]
            SYNTAX: [specific syntax patterns to search for, separated by commas]
            CONCEPTS: [related programming concepts, separated by commas]
            SEARCH_TERMS: [additional specific search terms that will help find relevant code]
            
            Example:
            For query "REST API endpoints":
            FRAMEWORKS: Flask, Spring Boot, Express.js
            SYNTAX: @app.route, @GetMapping, @PostMapping, @RequestMapping, app.get, app.post
            CONCEPTS: HTTP methods, route decorators, endpoint functions, API handlers
            SEARCH_TERMS: route, endpoint, API, HTTP, GET, POST, PUT, DELETE, handler
            
            Focus on the most likely frameworks and syntax based on the query intent.
            """, query);
    }

    /**
     * Parse Ollama's analysis response and construct enhanced search query
     */
    private String parseFrameworkAnalysisResponse(String response, String originalQuery) {
        StringBuilder enhancedQuery = new StringBuilder(originalQuery);
        
        String[] lines = response.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("FRAMEWORKS:")) {
                String frameworks = line.replace("FRAMEWORKS:", "").trim();
                if (!frameworks.isEmpty()) {
                    enhancedQuery.append(" ").append(frameworks);
                }
            } else if (line.startsWith("SYNTAX:")) {
                String syntax = line.replace("SYNTAX:", "").trim();
                if (!syntax.isEmpty()) {
                    enhancedQuery.append(" ").append(syntax);
                }
            } else if (line.startsWith("CONCEPTS:")) {
                String concepts = line.replace("CONCEPTS:", "").trim();
                if (!concepts.isEmpty()) {
                    enhancedQuery.append(" ").append(concepts);
                }
            } else if (line.startsWith("SEARCH_TERMS:")) {
                String searchTerms = line.replace("SEARCH_TERMS:", "").trim();
                if (!searchTerms.isEmpty()) {
                    enhancedQuery.append(" ").append(searchTerms);
                }
            }
        }
        
        return enhancedQuery.toString();
    }

    /**
     * Search the vector store with the enhanced query
     */
    private List<Document> searchVectorStore(String query, int maxResults) {
        try {
            // Check if we have pre-computed multi-query results
            if (multiQueryResults != null && query.contains("[MULTI-QUERY-EXPANSION]")) {
                System.out.println("🎯 Using pre-computed multi-query expansion results (skipping redundant vector search)");
                List<Document> results = new ArrayList<>(multiQueryResults);
                
                // Clear the stored results
                multiQueryResults = null;
                
                // Apply alternative ranking to the combined results
                if (!results.isEmpty()) {
                    String originalQuery = query.replace(" [MULTI-QUERY-EXPANSION]", "");
                    System.out.println("📊 Applying alternative ranking to " + results.size() + " pre-computed documents");
                    results = applyAlternativeRanking(results, originalQuery, maxResults);
                    System.out.println("📊 Multi-query expansion final results: " + results.size() + " documents");
                } else {
                    System.out.println("⚠️ No pre-computed results available, multi-query expansion may have failed");
                }
                
                return results;
            }
            
            // Get the dynamic VectorStore for the current collection
            String currentCollection = indexingService.getCurrentCollectionName();
            String currentDirectory = indexingService.getCurrentIndexingDirectory();

            // DEBUG: Temporarily disabled validation to test vector search
            System.out.println("🔍 DEBUG: Collection check - currentCollection: " + currentCollection + ", currentDirectory: " + currentDirectory);
            
            // Check if a directory has been indexed (not using default collection)
            // TEMPORARILY COMMENTED OUT FOR DEBUGGING
            /*
            if ("codebase-index".equals(currentCollection) || currentDirectory == null) {
                System.out.println("⚠️ No specific directory indexed yet. Please index a codebase first using option 6.");
                System.out.println("💡 Current collection: " + currentCollection);
                if (currentDirectory == null) {
                    System.out.println("💡 No indexing directory set. Use 'Index Codebase' to set a directory.");
                }
                return new ArrayList<>(); // Return empty results
            }
            */

            VectorStore dynamicVectorStore = vectorStoreFactory.createVectorStore(currentCollection);
            
            // Perform similarity search with debug
            System.out.println("🔍 Searching in collection: " + currentCollection + " (directory: " + currentDirectory + ")");
            System.out.println("🔍 Search query length: " + query.length() + " chars");
            System.out.println("🔍 Search query preview: " + (query.length() > 100 ? query.substring(0, 100) + "..." : query));
            
            // NEW: Alternative ranking approach - get ALL results without threshold filtering
            List<Document> documents;
            try {
                System.out.println("🎯 Using alternative ranking system instead of threshold filtering");
                
                // Get all potential matches using basic search
                // We'll rank them with our alternative ranking system instead of relying on Qdrant thresholds
                
                System.out.println("🔍 FINAL QUERY TO QDRANT: '" + query + "'");
                System.out.println("🔍 Collection: " + currentCollection);
                
                documents = dynamicVectorStore.similaritySearch(query);
                System.out.println("📊 Raw vector search found " + documents.size() + " matches");
                
                // Apply our alternative ranking system to improve results
                if (!documents.isEmpty()) {
                    documents = applyAlternativeRanking(documents, query, maxResults);
                    System.out.println("📊 After alternative ranking: " + documents.size() + " matches");
                } else {
                    // Fallback: try broader search with key terms
                    System.out.println("🔍 No raw results, trying broader search...");
                    String keyTerms = extractKeyTerms(query);
                    if (!keyTerms.equals(query)) {
                        documents = dynamicVectorStore.similaritySearch(keyTerms);
                        if (!documents.isEmpty()) {
                            documents = applyAlternativeRanking(documents, query, maxResults);
                            System.out.println("📊 Broader search + ranking: " + documents.size() + " matches");
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("❌ Error with alternative ranking, trying basic search: " + e.getMessage());
                documents = dynamicVectorStore.similaritySearch(query);
            }
            
            System.out.println("📊 Final result: " + documents.size() + " documents");

            return documents;
            
        } catch (Exception e) {
            // Only log non-gRPC related errors
            if (!isGrpcRelatedError(e)) {
                System.err.println("❌ Vector store search error: " + e.getMessage());
            }
            return new ArrayList<>();
        }
    }

    private void suppressLogging() {
        // Suppress gRPC and Qdrant loggers programmatically
        setLoggerLevel("io.grpc", Level.OFF);
        setLoggerLevel("io.qdrant", Level.OFF);
        setLoggerLevel("io.netty", Level.OFF);
        setLoggerLevel("grpc", Level.OFF);
    }

    private void restoreLogging() {
        // Restore to WARN level (as per application.properties)
        setLoggerLevel("io.grpc", Level.WARN);
        setLoggerLevel("io.qdrant", Level.WARN);
        setLoggerLevel("io.netty", Level.WARN);
        setLoggerLevel("grpc", Level.WARN);
    }

    private void setLoggerLevel(String loggerName, Level level) {
        try {
            Logger logger = (Logger) LoggerFactory.getLogger(loggerName);
            logger.setLevel(level);
        } catch (Exception e) {
            // Ignore any errors setting log levels
        }
    }

    private boolean isGrpcRelatedError(Exception e) {
        String message = e.getMessage();
        return message != null && (message.contains("NOT_FOUND") ||
                message.contains("doesn't exist") ||
                message.contains("grpc") ||
                message.contains("Collection") ||
                message.contains("Qdrant"));
    }

    private SearchResult convertToSearchResult(Document document) {
        Map<String, Object> metadata = document.getMetadata();

        // Add current collection name to metadata for proper display
        String currentCollection = indexingService.getCurrentCollectionName();
        metadata = new HashMap<>(metadata); // Create mutable copy
        metadata.put("collectionName", currentCollection);

        String content = document.getText();
        
        // Extract line number from metadata if available
        String lineNumberStr = (String) metadata.get("lineNumber");
        List<FileSearchService.LineMatch> lineMatches = new ArrayList<>();
        
        if (lineNumberStr != null && !lineNumberStr.isEmpty()) {
            try {
                int lineNumber = Integer.parseInt(lineNumberStr);
                String documentType = (String) metadata.getOrDefault("documentType", "");
                String elementName = "";
                
                switch (documentType) {
                    case "restApiEndpoint":
                        elementName = (String) metadata.getOrDefault("endpointName", "");
                        break;
                    case "function":
                        elementName = (String) metadata.getOrDefault("functionName", "");
                        break;
                    case "class":
                        elementName = (String) metadata.getOrDefault("className", "");
                        break;
                }
                
                if (!elementName.isEmpty()) {
                    // Extract the main line from content for display
                    String[] lines = content.split("\n");
                    String mainLine = lines.length > 2 ? lines[1] : content; // Usually line 1 contains the main definition
                    lineMatches.add(new FileSearchService.LineMatch(lineNumber, mainLine, elementName));
                }
            } catch (NumberFormatException e) {
                // Ignore invalid line numbers
            }
        }

        return new SearchResult(
                (String) metadata.getOrDefault("filename", "Unknown file"),
                (String) metadata.getOrDefault("filepath", "Unknown path"),
                content,
                1.0, // Vector search doesn't provide explicit scores
                "vector-search",
                convertLineMatches(lineMatches),
                metadata);
    }

    /**
     * Convert FileSearchService.LineMatch objects to HybridSearchService.LineMatch objects
     */
    private List<LineMatch> convertLineMatches(List<FileSearchService.LineMatch> fileLineMatches) {
        if (fileLineMatches == null) {
            return new ArrayList<>();
        }
        
        return fileLineMatches.stream()
            .map(fileMatch -> new LineMatch(
                fileMatch.getLineNumber(),
                fileMatch.getLineContent(),
                fileMatch.getMatchedTerm()
            ))
            .collect(Collectors.toList());
    }

    /**
     * Compare search results for sorting - prioritize based on document type and query relevance
     */
    private int compareSearchResults(SearchResult a, SearchResult b, String query) {
        String queryLower = query.toLowerCase();
        
        // Get document types
        String typeA = (String) a.getMetadata().getOrDefault("documentType", "");
        String typeB = (String) b.getMetadata().getOrDefault("documentType", "");
        
        // Priority scoring for document types based on query
        int scoreA = getDocumentTypeScore(typeA, queryLower);
        int scoreB = getDocumentTypeScore(typeB, queryLower);
        
        if (scoreA != scoreB) {
            return Integer.compare(scoreB, scoreA); // Higher score first
        }
        
        // If same document type, compare by line number (lower line numbers usually more important)
        String lineA = (String) a.getMetadata().get("lineNumber");
        String lineB = (String) b.getMetadata().get("lineNumber");
        
        if (lineA != null && lineB != null) {
            try {
                return Integer.compare(Integer.parseInt(lineA), Integer.parseInt(lineB));
            } catch (NumberFormatException e) {
                // Ignore and continue to next comparison
            }
        }
        
        // Finally, compare by filename
        return a.getFileName().compareTo(b.getFileName());
    }

    /**
     * Score document types based on query relevance
     */
    private int getDocumentTypeScore(String documentType, String queryLower) {
        // Handle project analysis and dependency queries
        if (queryLower.contains("project type") || queryLower.contains("framework") || queryLower.contains("technology")) {
            switch (documentType) {
                case "projectAnalysis": return 100;
                case "frameworkDocumentation": return 90;
                case "dependencies": return 80;
                case "summary": return 60;
                default: return 10;
            }
        }
        
        if (queryLower.contains("dependency") || queryLower.contains("library") || queryLower.contains("package")) {
            switch (documentType) {
                case "dependencies": return 100;
                case "projectAnalysis": return 80;
                case "frameworkDocumentation": return 60;
                case "summary": return 40;
                default: return 10;
            }
        }
        
        // Framework-specific syntax queries (like @app.route, @RequestMapping)
        if (queryLower.contains("@") && (queryLower.contains("app.route") || queryLower.contains("route") 
            || queryLower.contains("mapping") || queryLower.contains("get") || queryLower.contains("post"))) {
            switch (documentType) {
                case "frameworkDocumentation": return 100;
                case "restApiEndpoint": return 90;
                case "function": return 60;
                case "summary": return 40;
                default: return 10;
            }
        }
        
        if (queryLower.contains("flask") || queryLower.contains("spring")) {
            switch (documentType) {
                case "frameworkDocumentation": return 100;
                case "projectAnalysis": return 80;
                case "restApiEndpoint": return 70;
                case "function": return 60;
                default: return 20;
            }
        }
        
        if (queryLower.contains("rest api") || queryLower.contains("endpoint") || queryLower.contains("route")) {
            switch (documentType) {
                case "restApiEndpoint": return 100;
                case "frameworkDocumentation": return 90;
                case "function": return 60;
                case "summary": return 40;
                case "projectAnalysis": return 30;
                case "class": return 20;
                default: return 10;
            }
        }
        
        if (queryLower.contains("function") || queryLower.contains("method")) {
            switch (documentType) {
                case "function": return 100;
                case "restApiEndpoint": return 80;
                case "class": return 40;
                case "summary": return 30;
                default: return 10;
            }
        }
        
        if (queryLower.contains("class") || queryLower.contains("service")) {
            switch (documentType) {
                case "class": return 100;
                case "function": return 60;
                case "summary": return 50;
                case "restApiEndpoint": return 30;
                default: return 10;
            }
        }
        
        // Default scoring when query doesn't match specific patterns
        switch (documentType) {
            case "projectAnalysis": return 85;
            case "summary": return 80;
            case "frameworkDocumentation": return 75;
            case "restApiEndpoint": return 70;
            case "dependencies": return 65;
            case "function": return 60;
            case "class": return 50;
            default: return 30;
        }
    }

    private String generateAIAnalysis(String query, List<SearchResult> vectorResults,
            List<FileSearchService.SearchResult> fileResults) {
        try {
            StringBuilder context = new StringBuilder();
            context.append("Query: ").append(query).append("\n\n");

            // Add vector search results
            if (!vectorResults.isEmpty()) {
                context.append("Vector Search Results:\n");
                for (int i = 0; i < Math.min(3, vectorResults.size()); i++) {
                    SearchResult result = vectorResults.get(i);
                    context.append("File: ").append(result.getFileName()).append("\n");
                    context.append("Content: ")
                            .append(result.getContent().substring(0, Math.min(500, result.getContent().length())))
                            .append("...\n\n");
                }
            }

            // Add file search results
            if (!fileResults.isEmpty()) {
                context.append("File Search Results:\n");
                for (int i = 0; i < Math.min(2, fileResults.size()); i++) {
                    FileSearchService.SearchResult result = fileResults.get(i);
                    context.append("File: ").append(result.getFileName()).append("\n");
                    context.append("Content: ")
                            .append(result.getContent().substring(0, Math.min(300, result.getContent().length())))
                            .append("...\n\n");
                }
            }

            String prompt = "Based on the following code search results, provide a brief analysis of what was found and how it relates to the query '"
                    + query + "':\n\n" +
                    context.toString() + "\n\n" +
                    "Please provide:\n" +
                    "1. A summary of the main findings\n" +
                    "2. How the results relate to the query\n" +
                    "3. Key insights about the codebase structure\n\n" +
                    "Keep the response concise (max 200 words).";

            return chatModel.call(prompt);

        } catch (Exception e) {
            System.err.println("❌ Error generating AI analysis: " + e.getMessage());
            return "Analysis unavailable - results found but AI processing failed.";
        }
    }

    /**
     * Perform advanced search based on SearchRequest
     */
    public HybridSearchResult performAdvancedSearch(SearchRequest request) {
        System.out.println("🔍 Starting advanced search for: " + request.getQuery());

        List<SearchResult> vectorResults = new ArrayList<>();
        List<FileSearchService.SearchResult> fileResults = new ArrayList<>();
        String aiAnalysis = "";
        boolean usedFallback = false;

        try {
            switch (request.getSearchType()) {
                case SEMANTIC -> {
                    System.out.println("🧠 Performing semantic search...");
                    vectorResults = performSemanticSearch(request);
                    if(vectorResults != null){
                        System.out.println("📊 Semantic search completed - " + vectorResults.size() + " results found");
                    }   
                }
                case TEXT -> {
                    System.out.println("📝 Performing text search...");
                    fileResults = performTextSearch(request);
                }
                case HYBRID -> {
                    System.out.println("🔄 Performing hybrid search...");
                    vectorResults = performSemanticSearch(request);
                    if (vectorResults.size() < request.getLimit() / 2) {
                        fileResults = performTextSearch(request);
                        usedFallback = true;
                    }
                }
            }

            // Generate AI analysis if we have any results
            if (!vectorResults.isEmpty() || !fileResults.isEmpty()) {
                aiAnalysis = generateAdvancedAIAnalysis(request, vectorResults, fileResults);
            }

        } catch (Exception e) {
            System.err.println("❌ Error in advanced search: " + e.getMessage());
            // Fallback to text search
            fileResults = performTextSearch(request);
            usedFallback = true;
            aiAnalysis = "Search completed using text-based analysis due to technical limitations.";
        }

        return new HybridSearchResult(vectorResults, fileResults, aiAnalysis, usedFallback);
    }

    /**
     * Perform advanced multi-step semantic search with comprehensive code discovery capabilities.
     * 
     * This method implements a sophisticated 5-step search pipeline:
     * 1. Project Context Analysis - Detects frameworks, dependencies, and project type
     * 2. Framework Context Integration - Augments queries with framework-specific terms
     * 3. Code-Semantic Phrase Conversion - Transforms natural language to code patterns
     * 4. Semantic Variants Generation - Creates multiple query expressions for better recall
     * 5. Multi-Query Vector Search - Executes all variants with metadata-enhanced filtering
     * 
     * @param request The search request containing query, limits, and filters
     * @return List of enhanced SearchResult objects with relevance scoring and metadata
     */
    private List<SearchResult> performSemanticSearch(SearchRequest request) {
        System.out.println("🎯 SEMANTIC SEARCH NOW USING ALTERNATIVE RANKING SYSTEM");
        
        // Temporarily suppress gRPC logging during semantic search
        suppressLogging();

        try {
            // Get the dynamic VectorStore for the current collection
            String currentCollection = indexingService.getCurrentCollectionName();
            String currentDirectory = indexingService.getCurrentIndexingDirectory();

            // Check if a directory has been indexed (not using default collection)
            if ("codebase-index".equals(currentCollection) || currentDirectory == null) {
                System.out
                        .println("⚠️ No specific directory indexed yet. Please index a codebase first using option 6.");
                System.out.println("💡 Current collection: " + currentCollection);
                if (currentDirectory == null) {
                    System.out.println("💡 No indexing directory set. Use 'Index Codebase' to set a directory.");
                }
                return new ArrayList<>(); // Return empty results
            }

            VectorStore dynamicVectorStore = vectorStoreFactory.createVectorStore(currentCollection);

            System.out.println("🔍 Semantic search in collection: " + currentCollection + " (directory: "
                    + currentDirectory + ")");

            // STEP 1: Generate enhanced query using intelligent analysis
            String originalQuery = request.getQuery();
            String enhancedQuery = performIntelligentQueryAnalysis(originalQuery);
            
            // STEP 2: Check if we have pre-computed multi-query results
            List<Document> rawDocuments;
            if (multiQueryResults != null && enhancedQuery.contains("[MULTI-QUERY-EXPANSION]")) {
                System.out.println("🎯 Using pre-computed multi-query expansion results (skipping redundant vector search)");
                rawDocuments = new ArrayList<>(multiQueryResults);
                
                // Clear the stored results
                multiQueryResults = null;
                
                System.out.println("📊 Using " + rawDocuments.size() + " pre-computed documents from multi-query expansion");
            } else {
                // Only show debug output for non-multi-query expansion searches
                System.out.println("🔍 ORIGINAL QUERY: '" + originalQuery + "'");
                System.out.println("🔍 ENHANCED QUERY: '" + enhancedQuery + "'");
                System.out.println("🔍 FINAL QUERY TO VECTOR DATABASE: '" + enhancedQuery + "'");
                System.out.println("🔍 Query length: " + enhancedQuery.length() + " characters");
                System.out.println("🔍 Query type: " + request.getSearchType());
                
                // Perform the actual vector search
                rawDocuments = dynamicVectorStore.similaritySearch(enhancedQuery);
                System.out.println("📊 Raw documents retrieved: " + rawDocuments.size());
            }
            
            if (rawDocuments.isEmpty()) {
                System.out.println("❌ No documents found in vector search");
                return new ArrayList<>();
            }
            
            // STEP 3: Extract file paths from vector results for targeted file-based search
            System.out.println("🔍 Performing file-based search on vector result files...");
            Set<String> vectorResultFiles = rawDocuments.stream()
                .map(doc -> doc.getMetadata().getOrDefault("filepath", "").toString())
                .filter(path -> !path.isEmpty())
                .collect(Collectors.toSet());
            
            System.out.println("📁 Vector search found files: " + vectorResultFiles.size());
            
            // STEP 4: Apply 3-query file-based search on the vector result files
            List<FileSearchService.SearchResult> fileBasedResults = new ArrayList<>();
            if (!vectorResultFiles.isEmpty()) {
                // Query 1: Search for route decorators and filter to vector result files
                System.out.println("   🔍 File search 1: @app.route in vector result files");
                List<FileSearchService.SearchResult> routeResults = fileSearchService.searchInFiles("@app.route");
                List<FileSearchService.SearchResult> filteredRouteResults = filterResultsByFiles(routeResults, vectorResultFiles);
                fileBasedResults.addAll(filteredRouteResults);
                
                // Query 2: Search for Flask API content and filter to vector result files  
                System.out.println("   🔍 File search 2: Flask API endpoints in vector result files");
                List<FileSearchService.SearchResult> apiResults = fileSearchService.searchInFiles("Flask API endpoints");
                List<FileSearchService.SearchResult> filteredApiResults = filterResultsByFiles(apiResults, vectorResultFiles);
                fileBasedResults.addAll(filteredApiResults);
                
                // Query 3: Search for POST methods and filter to vector result files
                System.out.println("   🔍 File search 3: POST methods JSON in vector result files");
                List<FileSearchService.SearchResult> postResults = fileSearchService.searchInFiles("POST methods JSON");
                List<FileSearchService.SearchResult> filteredPostResults = filterResultsByFiles(postResults, vectorResultFiles);
                fileBasedResults.addAll(filteredPostResults);
                
                System.out.println("📊 File-based search results: " + fileBasedResults.size() + " matches in vector result files");
            }
            
            // STEP 5: Convert vector results to SearchResults with line matching
            List<SearchResult> vectorSearchResults = rawDocuments.stream()
                .map(doc -> convertToSearchResultWithScore(doc, originalQuery))
                .collect(Collectors.toList());
            
            // STEP 6: Convert file-based results to SearchResults and combine
            List<SearchResult> combinedResults = new ArrayList<>(vectorSearchResults);
            for (FileSearchService.SearchResult fileResult : fileBasedResults) {
                SearchResult convertedResult = convertFileSearchResultToSearchResult(fileResult, "file-enhanced");
                combinedResults.add(convertedResult);
            }
            
            // Remove duplicates based on file path and line number
            List<SearchResult> deduplicatedResults = removeDuplicateSearchResults(combinedResults);
            
            // Sort by highest similarity/relevance score (descending order)
            List<SearchResult> results = deduplicatedResults.stream()
                .sorted((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()))
                .collect(Collectors.toList());
            
            System.out.println("✅ Semantic search with similarity sorting completed: " + results.size() + " results");
            System.out.println("📊 Top result similarity: " + (results.isEmpty() ? "N/A" : 
                String.format("%.3f", results.get(0).getRelevanceScore())));
            return results;

        } catch (Exception e) {
            // Only log non-gRPC related errors
            if (!isGrpcRelatedError(e)) {
                System.err.println("❌ Semantic search failed: " + e.getMessage());
            }
            return List.of();
        } finally {
            // Restore normal logging
            restoreLogging();
        }
    }

    private List<FileSearchService.SearchResult> performTextSearch(SearchRequest request) {
        try {
            // Extract case sensitivity from filters if specified
            boolean caseSensitive = false;
            if (request.getFilters() != null && request.getFilters().containsKey("caseSensitive")) {
                caseSensitive = Boolean.TRUE.equals(request.getFilters().get("caseSensitive"));
            }
            
            return fileSearchService.searchInFiles(request.getQuery(), caseSensitive)
                    .stream()
                    .limit(request.getLimit())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("❌ Text search failed: " + e.getMessage());
            return List.of();
        }
    }

    private SearchResult convertToSearchResultWithScore(Document document, String query) {
        Map<String, Object> metadata = document.getMetadata();
        String fileName = metadata.getOrDefault("filename", "Unknown").toString();
        String filePath = metadata.getOrDefault("filepath", "Unknown").toString();
        String fullContent = document.getText(); // Use getText() instead of getContent()

        // Add current collection name to metadata for proper display
        String currentCollection = indexingService.getCurrentCollectionName();
        metadata = new HashMap<>(metadata); // Create mutable copy
        metadata.put("collectionName", currentCollection);

        // Extract line matches from content first
        List<FileSearchService.LineMatch> lineMatches = extractLineMatchesFromContent(fullContent, query);

        // Create focused content based on line matches instead of showing entire document
        String focusedContent = createFocusedContent(fullContent, lineMatches, query);

        // Calculate relevance score with document type boost
        double score = calculateRelevanceScoreWithMetadata(fullContent, query, metadata);

        return new SearchResult(fileName, filePath, focusedContent, score, "semantic", 
                                convertLineMatches(lineMatches), metadata);
    }

    /**
     * Create focused content based on line matches instead of showing entire document
     */
    private String createFocusedContent(String fullContent, List<FileSearchService.LineMatch> lineMatches, String query) {
        if (lineMatches.isEmpty()) {
            // If no line matches found, return a snippet of the full content
            return createContentSnippet(fullContent, query);
        }
        
        StringBuilder focusedContent = new StringBuilder();
        String[] allLines = fullContent.split("\n");
        
        // Show context around each line match
        for (int i = 0; i < lineMatches.size(); i++) {
            FileSearchService.LineMatch match = lineMatches.get(i);
            int lineNumber = match.getLineNumber();
            
            if (i > 0) {
                focusedContent.append("\n...\n");
            }
            
            // Show 2 lines of context before and after the match
            int startLine = Math.max(0, lineNumber - 3); // -3 because lineNumber is 1-based
            int endLine = Math.min(allLines.length - 1, lineNumber + 1); // +1 for context after
            
            for (int j = startLine; j <= endLine; j++) {
                if (j < allLines.length) {
                    String line = allLines[j];
                    int displayLineNumber = j + 1;
                    
                    // Highlight the matched line
                    if (displayLineNumber == lineNumber) {
                        focusedContent.append(String.format(">>> %d: %s\n", displayLineNumber, line));
                    } else {
                        focusedContent.append(String.format("    %d: %s\n", displayLineNumber, line));
                    }
                }
            }
        }
        
        return focusedContent.toString();
    }
    
    /**
     * Create a content snippet when no specific line matches are found
     */
    private String createContentSnippet(String fullContent, String query) {
        String[] lines = fullContent.split("\n");
        
        // Try to find the first line that contains any query term
        String[] queryTerms = query.toLowerCase().split("\\s+");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String lineLower = line.toLowerCase();
            
            for (String term : queryTerms) {
                if (term.length() > 2 && lineLower.contains(term)) {
                    // Found a relevant line, show context around it
                    StringBuilder snippet = new StringBuilder();
                    int startLine = Math.max(0, i - 2);
                    int endLine = Math.min(lines.length - 1, i + 5);
                    
                    for (int j = startLine; j <= endLine; j++) {
                        int displayLineNumber = j + 1;
                        if (j == i) {
                            snippet.append(String.format(">>> %d: %s\n", displayLineNumber, lines[j]));
                        } else {
                            snippet.append(String.format("    %d: %s\n", displayLineNumber, lines[j]));
                        }
                    }
                    
                    if (endLine < lines.length - 1) {
                        snippet.append("    ...\n");
                    }
                    
                    return snippet.toString();
                }
            }
        }
        
        // If no specific terms found, show the first few lines
        StringBuilder snippet = new StringBuilder();
        int maxLines = Math.min(8, lines.length);
        for (int i = 0; i < maxLines; i++) {
            snippet.append(String.format("    %d: %s\n", i + 1, lines[i]));
        }
        
        if (lines.length > maxLines) {
            snippet.append("    ...\n");
        }
        
        return snippet.toString();
    }

    /**
     * Simple search method that delegates to hybrid search
     */
    public List<SearchResult> search(String query, int maxResults) {
        try {
            HybridSearchResult hybridResult = performHybridSearch(query, maxResults);

            // Combine vector and file results into a single list
            List<SearchResult> allResults = new ArrayList<>(hybridResult.getVectorResults());
            
            // Convert file results to SearchResult format
            for (FileSearchService.SearchResult fileResult : hybridResult.getFileResults()) {
                SearchResult convertedResult = convertFileSearchResult(fileResult);
                allResults.add(convertedResult);
            }

            return allResults.stream()
                    .limit(maxResults)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("❌ Search failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Convert FileSearchService.SearchResult to HybridSearchService.SearchResult
     */
    private SearchResult convertFileSearchResult(FileSearchService.SearchResult fileResult) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("collectionName", indexingService.getCurrentCollectionName());
        metadata.put("searchType", fileResult.getSearchType());

        return new SearchResult(
                fileResult.getFileName(),
                fileResult.getFilePath(),
                fileResult.getContent(),
                fileResult.getRelevanceScore(),
                fileResult.getSearchType(),
                convertLineMatches(fileResult.getLineMatches()),
                metadata);
    }

    /**
     * Restart indexing process
     */
    public void restartIndexing() {
        try {
            indexingService.restartIndexing();
        } catch (Exception e) {
            throw new RuntimeException("Failed to restart indexing", e);
        }
    }

    /**
     * Clear cache and reindex all files
     */
    public void clearCacheAndReindex() {
        try {
            indexingService.clearCacheAndReindex();
        } catch (Exception e) {
            throw new RuntimeException("Failed to clear cache and reindex", e);
        }
    }

    /**
     * Get current indexing directory
     */
    public String getCurrentIndexingDirectory() {
        try {
            return indexingService.getCurrentIndexingDirectory();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /**
     * Get indexing status for display
     */
    public IndexingStatus getIndexingStatus() {
        try {
            // Return the detailed status from the indexing service directly
            return indexingService.getIndexingStatus();
        } catch (Exception e) {
            System.err.println("Error retrieving indexing status: " + e.getMessage());
            e.printStackTrace();
            // Return a safe default status using builder
            return IndexingStatus.builder()
                    .indexingComplete(false)
                    .indexingInProgress(false)
                    .indexedFiles(0)
                    .totalFiles(0)
                    .build();
        }
    }

    /**
     * Set the directory to index
     */
    public void setIndexingDirectory(String directory) {
        // Use the new method that sets both directory and collection name
        indexingService.setIndexingDirectoryWithCollection(directory);
        fileSearchService.setSearchDirectory(directory);
    }

    /**
     * Get the underlying IndexingService for detailed metrics
     */
    public FileIndexingService getIndexingService() {
        return indexingService;
    }

    /**
     * Extract line matches from content for vector search results, with special handling for REST API endpoints
     */
    private List<FileSearchService.LineMatch> extractLineMatchesFromContent(String content, String query) {
        List<FileSearchService.LineMatch> matches = new ArrayList<>();
        String[] lines = content.split("\n");
        String queryLower = query.toLowerCase();

        // Enhanced query matching - tokenize query for better matches
        String[] queryTerms = query.toLowerCase().split("\\s+");
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String lineLower = line.toLowerCase();
            
            // Score line relevance
            double lineRelevance = calculateLineRelevance(lineLower, queryTerms, query);
            
            // Lower threshold for semantic search to capture more matches
            if (lineRelevance > 0.1) {
                String actualMatchedTerm = findBestMatchingTerm(lineLower, queryTerms, query);
                matches.add(new FileSearchService.LineMatch(i + 1, line.trim(), actualMatchedTerm));
            }
        }

        // Sort by relevance and return top matches
        List<FileSearchService.LineMatch> sortedMatches = matches.stream()
            .sorted((a, b) -> Double.compare(
                calculateLineRelevance(b.getLineContent().toLowerCase(), queryTerms, query),
                calculateLineRelevance(a.getLineContent().toLowerCase(), queryTerms, query)
            ))
            .limit(8) // Show top 8 most relevant matches
            .collect(Collectors.toList());
        
        System.out.println("🔍 DEBUG: Line extraction for query '" + query + "' found " + matches.size() + " matches, returning " + sortedMatches.size());
        
        // If no matches found with relevance scoring, try simple string matching as fallback
        if (sortedMatches.isEmpty()) {
            System.out.println("🔍 DEBUG: No relevance matches found, trying simple string matching...");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                String lineLower = line.toLowerCase();
                
                // Simple containment check for any query term
                for (String term : queryTerms) {
                    if (lineLower.contains(term)) {
                        sortedMatches.add(new FileSearchService.LineMatch(i + 1, line.trim(), term));
                        break; // Only add each line once
                    }
                }
                
                // Limit to prevent too many results
                if (sortedMatches.size() >= 5) break;
            }
            System.out.println("🔍 DEBUG: Simple string matching found " + sortedMatches.size() + " additional matches");
        }
        
        return sortedMatches;
    }
    
    /**
     * Calculate line relevance score based on query terms
     */
    private double calculateLineRelevance(String lineLower, String[] queryTerms, String originalQuery) {
        double score = 0.0;
        
        // Exact query match gets highest score
        if (lineLower.contains(originalQuery.toLowerCase())) {
            score += 1.0;
        }
        
        // Count individual term matches
        int termMatches = 0;
        for (String term : queryTerms) {
            if (term.length() > 2 && lineLower.contains(term)) { // Skip very short terms
                termMatches++;
                score += 0.3;
            }
        }
        
        // Bonus for multiple term matches in same line
        if (termMatches > 1) {
            score += 0.2 * termMatches;
        }
        
        // Bonus for important code patterns
        if (lineLower.contains("def ") || lineLower.contains("class ") || 
            lineLower.contains("function ") || lineLower.contains("@")) {
            score += 0.2;
        }
        
        // Bonus for import statements and function signatures
        if (lineLower.contains("import ") || lineLower.contains("from ") ||
            lineLower.contains("public ") || lineLower.contains("private ")) {
            score += 0.1;
        }
        
        return Math.min(score, 1.0);
    }
    
    /**
     * Find the best matching term from the query that appears in the line
     */
    private String findBestMatchingTerm(String lineLower, String[] queryTerms, String originalQuery) {
        // First check for exact query match
        if (lineLower.contains(originalQuery.toLowerCase())) {
            return originalQuery;
        }
        
        // Find the longest matching term
        String bestMatch = "";
        for (String term : queryTerms) {
            if (term.length() > 2 && lineLower.contains(term) && term.length() > bestMatch.length()) {
                bestMatch = term;
            }
        }
        
        return bestMatch.isEmpty() ? originalQuery : bestMatch;
    }

    /**
     * Calculate relevance score with document type boost
     */
    private double calculateRelevanceScoreWithMetadata(String content, String query, Map<String, Object> metadata) {
        // Simple relevance scoring - count query terms in content
        String[] queryTerms = query.toLowerCase().split("\\s+");
        String contentLower = content.toLowerCase();

        long matches = Arrays.stream(queryTerms)
                .mapToLong(term -> contentLower.split(term, -1).length - 1)
                .sum();

        // Base score normalized by content length
        double baseScore = Math.min(1.0, matches / Math.max(1.0, contentLower.length() / 100.0));
        
        // Apply document type boost
        String documentType = (String) metadata.getOrDefault("documentType", "");
        double documentTypeBoost = getDocumentTypeBoost(documentType, query);
        
        // Apply the boost
        double finalScore = Math.min(1.0, baseScore * documentTypeBoost);
        
        return Math.max(0.1, finalScore); // Ensure minimum score for prioritized documents
    }
    
    private double getDocumentTypeBoost(String documentType, String query) {
        String queryLower = query.toLowerCase();
        
        // Strong boost for project analysis queries
        if (isProjectAnalysisQuery(queryLower)) {
            switch (documentType) {
                case "projectAnalysis": return 3.0;
                case "frameworkDocumentation": return 2.5;
                case "dependencies": return 2.0;
                default: return 1.0;
            }
        }
        
        // Standard boost for all queries
        switch (documentType) {
            case "projectAnalysis": return 1.5;
            case "frameworkDocumentation": return 1.3;
            case "dependencies": return 1.2;
            default: return 1.0;
        }
    }

    /**
     * Check if the query is related to project analysis, frameworks, or dependencies
     */
    private boolean isProjectAnalysisQuery(String query) {
        return query.contains("project") || query.contains("framework") || 
               query.contains("dependency") || query.contains("technology") ||
               query.contains("language") || query.contains("stack");
    }

    /**
     * Enhanced fallback query expansion for better programming concept matching
     */
    private String enhanceQueryForProgramming(String query) {
        StringBuilder enhanced = new StringBuilder(query);
        String queryLower = query.toLowerCase();
        
        // Add programming-specific terms based on query content
        if (queryLower.contains("api") || queryLower.contains("endpoint")) {
            enhanced.append(" route decorator function method HTTP REST");
        }
        
        if (queryLower.contains("database") || queryLower.contains("db")) {
            enhanced.append(" model query ORM SQLAlchemy repository entity");
        }
        
        if (queryLower.contains("function") || queryLower.contains("method")) {
            enhanced.append(" def function method implementation code");
        }
        
        if (queryLower.contains("class") || queryLower.contains("service")) {
            enhanced.append(" class service component module implementation");
        }
        
        return enhanced.toString();
    }

    /**
     * Extract key terms from query for broader search
     */
    private String extractKeyTerms(String query) {
        // Simple keyword extraction - remove common words and keep important terms
        String[] words = query.toLowerCase().split("\\s+");
        StringBuilder keyTerms = new StringBuilder();
        
        Set<String> stopWords = Set.of("the", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by");
        
        for (String word : words) {
            if (word.length() > 2 && !stopWords.contains(word)) {
                if (keyTerms.length() > 0) {
                    keyTerms.append(" ");
                }
                keyTerms.append(word);
            }
        }
        
        return keyTerms.toString();
    }

    /**
     * Apply alternative ranking system to documents
     */
    private List<Document> applyAlternativeRanking(List<Document> documents, String query, int maxResults) {
        // Simple ranking based on metadata and content relevance
        return documents.stream()
            .sorted((a, b) -> {
                String typeA = (String) a.getMetadata().getOrDefault("documentType", "");
                String typeB = (String) b.getMetadata().getOrDefault("documentType", "");
                
                int scoreA = getDocumentTypeScore(typeA, query.toLowerCase());
                int scoreB = getDocumentTypeScore(typeB, query.toLowerCase());
                
                return Integer.compare(scoreB, scoreA); // Higher score first
            })
            .limit(maxResults)
            .collect(Collectors.toList());
    }

    /**
     * Generate advanced AI analysis for search results
     */
    private String generateAdvancedAIAnalysis(SearchRequest request, List<SearchResult> vectorResults,
            List<FileSearchService.SearchResult> fileResults) {
        try {
            StringBuilder prompt = new StringBuilder();
            prompt.append("Analyze the following search results for the query: '").append(request.getQuery())
                    .append("'\n");
            prompt.append("Search type: ").append(request.getSearchType()).append("\n\n");

            if (!vectorResults.isEmpty()) {
                prompt.append("Semantic matches found:\n");
                vectorResults.stream().limit(3)
                        .forEach(result -> prompt.append("- ").append(result.getFileName()).append(": ")
                                .append(result.getContent().substring(0, Math.min(100, result.getContent().length())))
                                .append("...\n"));
            }

            if (!fileResults.isEmpty()) {
                prompt.append("\nText matches found:\n");
                fileResults.stream().limit(3)
                        .forEach(result -> prompt.append("- ").append(result.getFileName()).append(": ")
                                .append(result.getContent().substring(0, Math.min(100, result.getContent().length())))
                                .append("...\n"));
            }

            prompt.append("\nProvide a brief analysis of these results and suggestions for the developer.");

            return chatModel.call(prompt.toString());
        } catch (Exception e) {
            return "Advanced analysis temporarily unavailable. Results show relevant code matches for your query.";
        }
    }

    /**
     * Represents a search result with enhanced metadata and scoring
     */
    public static class SearchResult {
        private final String fileName;
        private final String filePath;
        private final String content;
        private final double relevanceScore;
        private final String searchType;
        private final List<LineMatch> lineMatches;
        private final Map<String, Object> metadata;

        public SearchResult(String fileName, String filePath, String content, double relevanceScore, 
                            String searchType) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.content = content;
            this.relevanceScore = relevanceScore;
            this.searchType = searchType;
            this.lineMatches = new ArrayList<>();
            this.metadata = new HashMap<>();
        }

        public SearchResult(String fileName, String filePath, String content, double relevanceScore, 
                            String searchType, List<LineMatch> lineMatches) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.content = content;
            this.relevanceScore = relevanceScore;
            this.searchType = searchType;
            this.lineMatches = lineMatches != null ? lineMatches : new ArrayList<>();
            this.metadata = new HashMap<>();
        }

        public SearchResult(String fileName, String filePath, String content, double relevanceScore, 
                            String searchType, List<LineMatch> lineMatches, Map<String, Object> metadata) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.content = content;
            this.relevanceScore = relevanceScore;
            this.searchType = searchType;
            this.lineMatches = lineMatches != null ? lineMatches : new ArrayList<>();
            this.metadata = metadata != null ? metadata : new HashMap<>();
        }

        // Getters
        public String getFileName() { return fileName; }
        public String getFilePath() { return filePath; }
        public String getContent() { return content; }
        public double getRelevanceScore() { return relevanceScore; }
        public double getScore() { return relevanceScore; } // Alias for compatibility
        public String getSearchType() { return searchType; }
        public List<LineMatch> getLineMatches() { return lineMatches; }
        public Map<String, Object> getMetadata() { return metadata; }
        
        // Convenience methods for common metadata
        public String getCollectionName() {
            return (String) metadata.getOrDefault("collectionName", "codebase-index");
        }
        
        public String getLastModifiedDate() {
            return (String) metadata.getOrDefault("lastModifiedDate", "Unknown");
        }
        
        public String getIndexedAt() {
            return (String) metadata.getOrDefault("indexedAt", "Unknown");
        }
        
        public String getFileSize() {
            return (String) metadata.getOrDefault("size", "Unknown");
        }
    }

    /**
     * Represents a line match within a file
     */
    public static class LineMatch {
        private final int lineNumber;
        private final String lineContent;
        private final String matchedTerm;
        private final int startIndex;
        private final int endIndex;

        public LineMatch(int lineNumber, String lineContent, String matchedTerm) {
            this.lineNumber = lineNumber;
            this.lineContent = lineContent;
            this.matchedTerm = matchedTerm;
            this.startIndex = -1;
            this.endIndex = -1;
        }

        public LineMatch(int lineNumber, String lineContent, String matchedTerm, int startIndex, int endIndex) {
            this.lineNumber = lineNumber;
            this.lineContent = lineContent;
            this.matchedTerm = matchedTerm;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        // Getters
        public int getLineNumber() { return lineNumber; }
        public String getLineContent() { return lineContent; }
        public String getMatchedTerm() { return matchedTerm; }
        public int getStartIndex() { return startIndex; }
        public int getEndIndex() { return endIndex; }
    }

    /**
     * Represents the result of a hybrid search combining vector and file-based results
     */
    public static class HybridSearchResult {
        private final List<SearchResult> vectorResults;
        private final List<FileSearchService.SearchResult> fileResults;
        private final String aiAnalysis;
        private final boolean usedFallback;
        private final long searchDuration;
        private final Map<String, Object> searchMetadata;

        public HybridSearchResult(List<SearchResult> vectorResults, 
                                  List<FileSearchService.SearchResult> fileResults,
                                  String aiAnalysis, boolean usedFallback) {
            this.vectorResults = vectorResults != null ? vectorResults : new ArrayList<>();
            this.fileResults = fileResults != null ? fileResults : new ArrayList<>();
            this.aiAnalysis = aiAnalysis;
            this.usedFallback = usedFallback;
            this.searchDuration = 0;
            this.searchMetadata = new HashMap<>();
        }

        public HybridSearchResult(List<SearchResult> vectorResults, 
                                  List<FileSearchService.SearchResult> fileResults,
                                  String aiAnalysis, boolean usedFallback, long searchDuration) {
            this.vectorResults = vectorResults != null ? vectorResults : new ArrayList<>();
            this.fileResults = fileResults != null ? fileResults : new ArrayList<>();
            this.aiAnalysis = aiAnalysis;
            this.usedFallback = usedFallback;
            this.searchDuration = searchDuration;
            this.searchMetadata = new HashMap<>();
        }

        // Getters
        public List<SearchResult> getVectorResults() { return vectorResults; }
        public List<FileSearchService.SearchResult> getFileResults() { return fileResults; }
        public String getAiAnalysis() { return aiAnalysis; }
        public boolean isUsedFallback() { return usedFallback; }
        public long getSearchDuration() { return searchDuration; }
        public Map<String, Object> getSearchMetadata() { return searchMetadata; }
        
        public int getTotalResults() { 
            return vectorResults.size() + fileResults.size(); 
        }
        
        public boolean hasResults() {
            return !vectorResults.isEmpty() || !fileResults.isEmpty();
        }
    }

    /**
     * Filter file search results to only include files from vector results
     */
    private List<FileSearchService.SearchResult> filterResultsByFiles(List<FileSearchService.SearchResult> results, Set<String> vectorResultFiles) {
        return results.stream()
            .filter(result -> vectorResultFiles.contains(result.getFilePath()))
            .collect(Collectors.toList());
    }

    /**
     * Convert FileSearchService.SearchResult to SearchResult for consistent display
     */
    private SearchResult convertFileSearchResultToSearchResult(FileSearchService.SearchResult fileResult, String searchType) {
        // Create metadata map with file information
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("filename", fileResult.getFileName());
        metadata.put("filepath", fileResult.getFilePath());
        metadata.put("searchType", searchType);
        
        // Convert line matches from FileSearchService format to our format
        List<LineMatch> lineMatches = new ArrayList<>();
        for (FileSearchService.LineMatch fileLineMatch : fileResult.getLineMatches()) {
            lineMatches.add(new LineMatch(
                fileLineMatch.getLineNumber(),
                fileLineMatch.getLineContent(),
                fileLineMatch.getMatchedTerm()
            ));
        }
        
        return new SearchResult(
            fileResult.getFileName(),
            fileResult.getFilePath(),
            fileResult.getContent(),
            fileResult.getRelevanceScore(),
            searchType,
            lineMatches,
            metadata
        );
    }

    /**
     * Remove duplicate search results based on file path and line numbers
     */
    private List<SearchResult> removeDuplicateSearchResults(List<SearchResult> results) {
        Map<String, SearchResult> uniqueResults = new LinkedHashMap<>();
        
        for (SearchResult result : results) {
            // Create a unique key based on file path and line numbers
            StringBuilder keyBuilder = new StringBuilder(result.getFilePath());
            for (LineMatch lineMatch : result.getLineMatches()) {
                keyBuilder.append(":").append(lineMatch.getLineNumber());
            }
            String uniqueKey = keyBuilder.toString();
            
            // Keep the first occurrence or the one with higher relevance score
            if (!uniqueResults.containsKey(uniqueKey) || 
                uniqueResults.get(uniqueKey).getRelevanceScore() < result.getRelevanceScore()) {
                uniqueResults.put(uniqueKey, result);
            }
        }
        
        return new ArrayList<>(uniqueResults.values());
    }

}
