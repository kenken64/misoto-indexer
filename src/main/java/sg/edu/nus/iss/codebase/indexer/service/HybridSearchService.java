package sg.edu.nus.iss.codebase.indexer.service;

import sg.edu.nus.iss.codebase.indexer.dto.SearchRequest;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileIndexingService;
import sg.edu.nus.iss.codebase.indexer.config.DynamicVectorStoreFactory;
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
            if (indexingService.getIndexedFileCount() > 0) {
                String currentCollection = indexingService.getCurrentCollectionName();
                System.out.println(
                        "🎯 Performing vector-based semantic search using collection: " + currentCollection + "...");
                vectorResults = performVectorSearch(query, maxResults);
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
        // Temporarily suppress gRPC logging during vector search
        suppressLogging();

        try {
            // Stage 1: Intelligent Framework Analysis using Ollama
            String intelligentQuery = performIntelligentQueryAnalysis(query);
            
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
            return new ArrayList<>();
        } finally {
            // Restore normal logging
            restoreLogging();
        }
    }

    /**
     * Stage 1: Use Ollama to analyze the search query and identify relevant frameworks and terms
     */
    private String performIntelligentQueryAnalysis(String originalQuery) {
        try {
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
            // Get the dynamic VectorStore for the current collection
            String currentCollection = indexingService.getCurrentCollectionName();
            String currentDirectory = indexingService.getCurrentIndexingDirectory();

            // Check if a directory has been indexed (not using default collection)
            if ("codebase-index".equals(currentCollection) || currentDirectory == null) {
                System.out.println("⚠️ No specific directory indexed yet. Please index a codebase first using option 6.");
                System.out.println("💡 Current collection: " + currentCollection);
                if (currentDirectory == null) {
                    System.out.println("💡 No indexing directory set. Use 'Index Codebase' to set a directory.");
                }
                return new ArrayList<>(); // Return empty results
            }

            VectorStore dynamicVectorStore = vectorStoreFactory.createVectorStore(currentCollection);
            
            // Perform similarity search
            List<Document> documents = dynamicVectorStore.similaritySearch(query);

            System.out.println("🔍 Searching in collection: " + currentCollection + " (directory: " + currentDirectory + ")");
            System.out.println("📊 Found " + documents.size() + " potential matches");

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
                metadata,
                lineMatches);
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

    private List<SearchResult> performSemanticSearch(SearchRequest request) {
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

            // Use threshold if specified
            List<Document> documents;
            if (request.getThreshold() != null) {
                documents = dynamicVectorStore.similaritySearch(request.getQuery());
                // Filter by threshold - this is a simplified approach
                // In a real implementation, you'd have more sophisticated threshold filtering
            } else {
                documents = dynamicVectorStore.similaritySearch(request.getQuery());
            }

            return documents.stream()
                    .limit(request.getLimit())
                    .map(doc -> convertToSearchResultWithScore(doc, request.getQuery()))
                    .collect(Collectors.toList());

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
        String content = document.getText(); // Use getText() instead of getContent()

        // Add current collection name to metadata for proper display
        String currentCollection = indexingService.getCurrentCollectionName();
        metadata = new HashMap<>(metadata); // Create mutable copy
        metadata.put("collectionName", currentCollection);

        // Calculate a simple relevance score based on query match
        double score = calculateRelevanceScore(content, query);

        // Extract line matches from content
        List<FileSearchService.LineMatch> lineMatches = extractLineMatchesFromContent(content, query);

        return new SearchResult(fileName, filePath, content, score, "semantic", metadata, lineMatches);
    }

    private double calculateRelevanceScore(String content, String query) {
        // Simple relevance scoring - count query terms in content
        String[] queryTerms = query.toLowerCase().split("\\s+");
        String contentLower = content.toLowerCase();

        long matches = Arrays.stream(queryTerms)
                .mapToLong(term -> contentLower.split(term, -1).length - 1)
                .sum();

        // Normalize by content length
        return Math.min(1.0, matches / Math.max(1.0, contentLower.length() / 100.0));
    }

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
            // Get the detailed status from the indexing service
            sg.edu.nus.iss.codebase.indexer.model.IndexingStatus detailedStatus = indexingService.getIndexingStatus();

            // Convert to the simple IndexingStatus for this service
            return new IndexingStatus(
                    detailedStatus.isIndexingComplete(),
                    detailedStatus.isIndexingInProgress(),
                    detailedStatus.getIndexedFiles(),
                    detailedStatus.getTotalFiles(),
                    detailedStatus.getProgress());
        } catch (Exception e) {
            System.err.println("Error retrieving indexing status: " + e.getMessage());
            e.printStackTrace();
            // Return a safe default status
            return new IndexingStatus(false, false, 0, 0, 0.0);
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
     * Search result class with enhanced metadata
     */
    public static class SearchResult {
        private final String fileName;
        private final String filePath;
        private final String content;
        private final double relevanceScore;
        private final String searchType;
        private final Map<String, Object> metadata;
        private final List<FileSearchService.LineMatch> lineMatches;

        public SearchResult(String fileName, String filePath, String content, double relevanceScore,
                String searchType) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.content = content;
            this.relevanceScore = relevanceScore;
            this.searchType = searchType;
            this.metadata = new HashMap<>();
            this.lineMatches = new ArrayList<>();
        }

        public SearchResult(String fileName, String filePath, String content, double relevanceScore, String searchType,
                Map<String, Object> metadata) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.content = content;
            this.relevanceScore = relevanceScore;
            this.searchType = searchType;
            this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
            this.lineMatches = new ArrayList<>();
        }

        public SearchResult(String fileName, String filePath, String content, double relevanceScore, String searchType,
                Map<String, Object> metadata, List<FileSearchService.LineMatch> lineMatches) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.content = content;
            this.relevanceScore = relevanceScore;
            this.searchType = searchType;
            this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
            this.lineMatches = lineMatches != null ? lineMatches : new ArrayList<>();
        }

        public String getFileName() {
            return fileName;
        }

        public String getFilePath() {
            return filePath;
        }

        public String getContent() {
            return content;
        }

        public double getRelevanceScore() {
            return relevanceScore;
        }

        public double getScore() {
            return relevanceScore;
        } // Add getScore() method for compatibility

        public String getSearchType() {
            return searchType;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public List<FileSearchService.LineMatch> getLineMatches() {
            return lineMatches;
        }

        // Convenience methods for common metadata
        public String getLastModifiedDate() {
            return (String) metadata.getOrDefault("lastModifiedDate", "Unknown");
        }

        public String getIndexedAt() {
            return (String) metadata.getOrDefault("indexedAt", "Unknown");
        }

        public String getFileSize() {
            return (String) metadata.getOrDefault("size", "Unknown");
        }

        public String getCollectionName() {
            return (String) metadata.getOrDefault("collectionName", "codebase-index");
        }
    }

    /**
     * Hybrid search result container
     */
    public static class HybridSearchResult {
        private final List<SearchResult> vectorResults;
        private final List<FileSearchService.SearchResult> fileResults;
        private final String aiAnalysis;
        private final boolean usedFallback;

        public HybridSearchResult(List<SearchResult> vectorResults, List<FileSearchService.SearchResult> fileResults,
                String aiAnalysis, boolean usedFallback) {
            this.vectorResults = vectorResults;
            this.fileResults = fileResults;
            this.aiAnalysis = aiAnalysis;
            this.usedFallback = usedFallback;
        }

        public List<SearchResult> getVectorResults() {
            return vectorResults;
        }

        public List<FileSearchService.SearchResult> getFileResults() {
            return fileResults;
        }

        public String getAiAnalysis() {
            return aiAnalysis;
        }

        public boolean isUsedFallback() {
            return usedFallback;
        }

        public int getTotalResults() {
            return vectorResults.size() + fileResults.size();
        }
    }

    /**
     * Indexing status information
     */
    public static class IndexingStatus {
        private final boolean complete;
        private final boolean inProgress;
        private final int indexedFiles;
        private final int totalFiles;
        private final double progress;

        public IndexingStatus(boolean complete, boolean inProgress, int indexedFiles, int totalFiles, double progress) {
            this.complete = complete;
            this.inProgress = inProgress;
            this.indexedFiles = indexedFiles;
            this.totalFiles = totalFiles;
            this.progress = progress;
        }

        public boolean isComplete() {
            return complete;
        }

        public boolean isInProgress() {
            return inProgress;
        }

        public int getIndexedFiles() {
            return indexedFiles;
        }

        public int getTotalFiles() {
            return totalFiles;
        }

        public double getProgress() {
            return progress;
        }
    }

    /**
     * Simple search method that delegates to hybrid search
     */
    public List<SearchResult> search(String query, int maxResults) {
        try {
            HybridSearchResult hybridResult = performHybridSearch(query, maxResults);

            // Combine vector and file results into a single list
            List<SearchResult> allResults = new ArrayList<>(hybridResult.getVectorResults());
            // Convert file results to SearchResult format if needed
            for (FileSearchService.SearchResult fileResult : hybridResult.getFileResults()) {
                Map<String, Object> metadata = new HashMap<>();
                String currentCollection = indexingService.getCurrentCollectionName();
                metadata.put("collectionName", currentCollection);
                metadata.put("searchType", "file-search");
                metadata.put("filename", fileResult.getFileName());
                metadata.put("filepath", fileResult.getFilePath());
                SearchResult searchResult = new SearchResult(
                        fileResult.getFileName(),
                        fileResult.getFilePath(),
                        fileResult.getContent(),
                        fileResult.getRelevanceScore(),
                        "file-search",
                        metadata);
                allResults.add(searchResult);
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
                metadata,
                fileResult.getLineMatches());
    }

    /**
     * Extract line matches from content for vector search results
     */
    private List<FileSearchService.LineMatch> extractLineMatchesFromContent(String content, String query) {
        List<FileSearchService.LineMatch> matches = new ArrayList<>();
        String[] lines = content.split("\n");
        String queryLower = query.toLowerCase();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.toLowerCase().contains(queryLower)) {
                matches.add(new FileSearchService.LineMatch(i + 1, line.trim(), query));
            }
        }

        return matches.stream().limit(5).collect(Collectors.toList()); // Limit to 5 matches per result
    }

    private String enhanceQueryForProgramming(String query) {
        // Enhanced fallback query expansion for better programming concept matching
        String enhancedQuery = query.toLowerCase();
        String originalQuery = query; // Keep original case for certain patterns
        
        // REST API and endpoint patterns - most comprehensive
        if (enhancedQuery.contains("rest api") || enhancedQuery.contains("api endpoint") || 
            enhancedQuery.contains("endpoints") || enhancedQuery.contains("api") || 
            enhancedQuery.contains("endpoint")) {
            enhancedQuery += " @app.route Flask route decorator HTTP endpoint web service API function";
            enhancedQuery += " @RequestMapping @GetMapping @PostMapping Spring Boot controller";
            enhancedQuery += " app.get app.post Express.js router endpoint handler";
            enhancedQuery += " GET POST PUT DELETE HTTP methods REST";
        }
        
        // Framework-specific syntax patterns
        if (enhancedQuery.contains("@app.route") || enhancedQuery.contains("app.route")) {
            enhancedQuery += " Flask endpoint decorator route function HTTP API framework documentation";
            enhancedQuery += " methods=['GET'] methods=['POST'] Flask routing patterns";
        }
        
        if (enhancedQuery.contains("flask")) {
            enhancedQuery += " @app.route endpoint decorator route function render_template jsonify request";
            enhancedQuery += " Flask-SQLAlchemy db.Model app.run framework documentation";
        }
        
        if (enhancedQuery.contains("spring boot") || enhancedQuery.contains("spring")) {
            enhancedQuery += " @RestController @RequestMapping @GetMapping @PostMapping controller";
            enhancedQuery += " @Service @Repository @Autowired annotation framework documentation";
        }
        
        // Database and ORM patterns
        if (enhancedQuery.contains("database") || enhancedQuery.contains("orm") || 
            enhancedQuery.contains("model") || enhancedQuery.contains("entity")) {
            enhancedQuery += " db.Model SQLAlchemy @Entity JPA @Repository";
            enhancedQuery += " query filter findBy save database framework";
        }
        
        // Authentication and security
        if (enhancedQuery.contains("auth") || enhancedQuery.contains("login") || 
            enhancedQuery.contains("security") || enhancedQuery.contains("session")) {
            enhancedQuery += " @login_required session authentication security JWT token";
            enhancedQuery += " @PreAuthorize @Secured Spring Security framework";
        }
        
        // Project type and dependency analysis
        if (enhancedQuery.contains("project type") || enhancedQuery.contains("technology stack") ||
            enhancedQuery.contains("framework") || enhancedQuery.contains("dependencies")) {
            enhancedQuery += " framework library dependency python java javascript spring flask";
            enhancedQuery += " requirements.txt pom.xml package.json project analysis";
        }
        
        if (enhancedQuery.contains("dependency") || enhancedQuery.contains("library") ||
            enhancedQuery.contains("package")) {
            enhancedQuery += " package framework requirements.txt pom.xml package.json import";
            enhancedQuery += " maven gradle npm pip dependency management";
        }
        
        // Programming language specific enhancements
        if (enhancedQuery.contains("python")) {
            enhancedQuery += " flask django requirements.txt pip import package library";
            enhancedQuery += " @app.route def function python framework";
        }
        
        if (enhancedQuery.contains("java")) {
            enhancedQuery += " spring maven gradle pom.xml dependency jar library framework";
            enhancedQuery += " @RestController @Service @Repository annotation";
        }
        
        if (enhancedQuery.contains("javascript") || enhancedQuery.contains("node")) {
            enhancedQuery += " npm package.json express react vue angular library";
            enhancedQuery += " app.get app.post router middleware framework";
        }
        
        // Testing frameworks
        if (enhancedQuery.contains("test") || enhancedQuery.contains("testing")) {
            enhancedQuery += " pytest @Test unittest JUnit Jest test framework";
            enhancedQuery += " def test_ assert expect() beforeEach()";
        }
        
        // Frontend frameworks
        if (enhancedQuery.contains("react") || enhancedQuery.contains("frontend") || 
            enhancedQuery.contains("component")) {
            enhancedQuery += " useState useEffect componentDidMount JSX React framework";
            enhancedQuery += " v-model Vue @Component Angular frontend";
        }
        
        // Add framework documentation search terms
        enhancedQuery += " framework documentation syntax pattern";
        
        return enhancedQuery;
    }
}
