package sg.edu.nus.iss.codebase.indexer.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class HybridSearchService {

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private FileSearchService fileSearchService;

    @Autowired
    private IndexingService indexingService;

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
                System.out.println("🎯 Performing vector-based semantic search...");
                vectorResults = performVectorSearch(query, maxResults);
            }

            // If vector search has limited results or indexing is incomplete, use file search as supplement/fallback
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
    }    private List<SearchResult> performVectorSearch(String query, int maxResults) {
        try {
            List<Document> documents = vectorStore.similaritySearch(query);
            
            return documents.stream()
                    .limit(maxResults)
                    .map(this::convertToSearchResult)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            System.err.println("❌ Vector search failed: " + e.getMessage());
            return List.of();
        }
    }

    private SearchResult convertToSearchResult(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        return new SearchResult(
                (String) metadata.getOrDefault("filename", "Unknown file"),
                (String) metadata.getOrDefault("filepath", "Unknown path"),
                document.getText(),
                1.0, // Vector search doesn't provide explicit scores
                "vector-search"
        );
    }

    private String generateAIAnalysis(String query, List<SearchResult> vectorResults, List<FileSearchService.SearchResult> fileResults) {
        try {
            StringBuilder context = new StringBuilder();
            context.append("Query: ").append(query).append("\n\n");
            
            // Add vector search results
            if (!vectorResults.isEmpty()) {
                context.append("Vector Search Results:\n");
                for (int i = 0; i < Math.min(3, vectorResults.size()); i++) {
                    SearchResult result = vectorResults.get(i);
                    context.append("File: ").append(result.getFileName()).append("\n");
                    context.append("Content: ").append(result.getContent().substring(0, Math.min(500, result.getContent().length()))).append("...\n\n");
                }
            }
            
            // Add file search results
            if (!fileResults.isEmpty()) {
                context.append("File Search Results:\n");
                for (int i = 0; i < Math.min(2, fileResults.size()); i++) {
                    FileSearchService.SearchResult result = fileResults.get(i);
                    context.append("File: ").append(result.getFileName()).append("\n");
                    context.append("Content: ").append(result.getContent().substring(0, Math.min(300, result.getContent().length()))).append("...\n\n");
                }
            }

            String prompt = "Based on the following code search results, provide a brief analysis of what was found and how it relates to the query '" + query + "':\n\n" +
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
     * Get indexing status for display
     */
    public IndexingStatus getIndexingStatus() {
        return new IndexingStatus(
                indexingService.isIndexingComplete(),
                indexingService.isIndexingInProgress(),
                indexingService.getIndexedFileCount(),
                indexingService.getTotalFileCount(),
                indexingService.getIndexingProgress()
        );
    }    /**
     * Set the directory to index
     */
    public void setIndexingDirectory(String directory) {
        indexingService.setIndexingDirectory(directory);
        fileSearchService.setSearchDirectory(directory);
    }

    /**
     * Get the underlying IndexingService for detailed metrics
     */
    public IndexingService getIndexingService() {
        return indexingService;
    }    /**
     * Search result class
     */
    public static class SearchResult {
        private final String fileName;
        private final String filePath;
        private final String content;
        private final double relevanceScore;
        private final String searchType;

        public SearchResult(String fileName, String filePath, String content, double relevanceScore, String searchType) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.content = content;
            this.relevanceScore = relevanceScore;
            this.searchType = searchType;
        }

        public String getFileName() { return fileName; }
        public String getFilePath() { return filePath; }
        public String getContent() { return content; }
        public double getRelevanceScore() { return relevanceScore; }
        public String getSearchType() { return searchType; }
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

        public List<SearchResult> getVectorResults() { return vectorResults; }
        public List<FileSearchService.SearchResult> getFileResults() { return fileResults; }
        public String getAiAnalysis() { return aiAnalysis; }
        public boolean isUsedFallback() { return usedFallback; }
        
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

        public boolean isComplete() { return complete; }
        public boolean isInProgress() { return inProgress; }
        public int getIndexedFiles() { return indexedFiles; }
        public int getTotalFiles() { return totalFiles; }
        public double getProgress() { return progress; }
    }
}
