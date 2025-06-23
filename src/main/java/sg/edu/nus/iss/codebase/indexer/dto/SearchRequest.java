package sg.edu.nus.iss.codebase.indexer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for advanced search operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {
    
    /**
     * The search query text
     */
    private String query;
    
    /**
     * Maximum number of results to return
     */
    @Builder.Default
    private Integer limit = 10;
    
    /**
     * Offset for pagination
     */
    @Builder.Default
    private Integer offset = 0;
    
    /**
     * Search filters for specific fields
     */
    private Map<String, Object> filters;
    
    /**
     * Sort criteria
     */
    private List<SortCriteria> sortBy;
    
    /**
     * Search type (TEXT, SEMANTIC, HYBRID)
     */
    @Builder.Default
    private SearchType searchType = SearchType.HYBRID;
    
    /**
     * Similarity threshold for semantic search (0.0 to 1.0)
     */
    @Builder.Default
    private Double threshold = 0.7;
    
    /**
     * Include metadata in response
     */
    @Builder.Default
    private Boolean includeMetadata = true;
    
    /**
     * Include source code snippets in response
     */
    @Builder.Default
    private Boolean includeSnippets = true;
    
    /**
     * Language filters for code search
     */
    private List<String> languages;
    
    /**
     * File type filters
     */
    private List<String> fileTypes;
    
    /**
     * Repository or project filters
     */
    private List<String> repositories;
    
    /**
     * Date range filters
     */
    private DateRange dateRange;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SortCriteria {
        private String field;
        private SortDirection direction;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateRange {
        private String from;
        private String to;
    }
    
    public enum SearchType {
        TEXT, SEMANTIC, HYBRID
    }
    
    public enum SortDirection {
        ASC, DESC
    }
}
