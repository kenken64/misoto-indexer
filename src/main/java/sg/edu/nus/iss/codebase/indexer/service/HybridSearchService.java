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
            // Enhance the query for better programming concept matching
            String enhancedQuery = enhanceQueryForProgramming(query);
            
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

            List<Document> documents = dynamicVectorStore.similaritySearch(enhancedQuery);

            System.out.println(
                    "🔍 Searching in collection: " + currentCollection + " (directory: " + currentDirectory + ")");

            return documents.stream()
                    .limit(maxResults)
                    .map(this::convertToSearchResult)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            // Only log non-gRPC related errors
            if (!isGrpcRelatedError(e)) {
                System.err.println("❌ Vector search failed: " + e.getMessage());
            }
            return List.of();
        } finally {
            // Restore normal logging
            restoreLogging();
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
        // Extract line matches for display (using empty query as this is vector search)
        List<FileSearchService.LineMatch> lineMatches = new ArrayList<>();

        return new SearchResult(
                (String) metadata.getOrDefault("filename", "Unknown file"),
                (String) metadata.getOrDefault("filepath", "Unknown path"),
                content,
                1.0, // Vector search doesn't provide explicit scores
                "vector-search",
                metadata,
                lineMatches);
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
        // Enhance queries for better programming concept matching
        String enhancedQuery = query.toLowerCase();
        
        // Map common programming terms to more specific ones
        if (enhancedQuery.contains("rest api") || enhancedQuery.contains("api endpoint")) {
            enhancedQuery += " @app.route Flask route decorator HTTP endpoint web service API function";
        }
        
        if (enhancedQuery.contains("endpoint")) {
            enhancedQuery += " @app.route route decorator Flask web API";
        }
        
        if (enhancedQuery.contains("database") && enhancedQuery.contains("repository")) {
            enhancedQuery += " DAO data access object model entity";
        }
        
        if (enhancedQuery.contains("authentication")) {
            enhancedQuery += " login security auth user session token";
        }
        
        return enhancedQuery;
    }
}
