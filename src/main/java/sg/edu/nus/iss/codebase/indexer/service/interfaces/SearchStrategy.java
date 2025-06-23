package sg.edu.nus.iss.codebase.indexer.service.interfaces;

import sg.edu.nus.iss.codebase.indexer.dto.SearchRequest;
import java.util.List;

/**
 * Strategy interface for different search implementations
 * Implements Strategy Pattern
 */
public interface SearchStrategy {
    
    /**
     * Execute search with the given request
     * @param request The search request
     * @return List of search results
     */
    List<SearchResult> search(SearchRequest request);
    
    /**
     * Check if this strategy supports the given search type
     * @param searchType The search type to check
     * @return true if this strategy supports the search type
     */
    boolean supports(SearchRequest.SearchType searchType);
    
    /**
     * Get the search type this strategy handles
     * @return The search type
     */
    SearchRequest.SearchType getSearchType();
    
    /**
     * Search result model
     */
    class SearchResult {
        private final String filePath;
        private final String fileName;
        private final String content;
        private final int lineNumber;
        private final double relevanceScore;
        private final String snippet;
        
        public SearchResult(String filePath, String fileName, String content, 
                          int lineNumber, double relevanceScore, String snippet) {
            this.filePath = filePath;
            this.fileName = fileName;
            this.content = content;
            this.lineNumber = lineNumber;
            this.relevanceScore = relevanceScore;
            this.snippet = snippet;
        }
        
        // Getters
        public String getFilePath() { return filePath; }
        public String getFileName() { return fileName; }
        public String getContent() { return content; }
        public int getLineNumber() { return lineNumber; }
        public double getRelevanceScore() { return relevanceScore; }
        public String getSnippet() { return snippet; }
    }
}
