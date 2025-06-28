package sg.edu.nus.iss.codebase.indexer.service.impl.search;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sg.edu.nus.iss.codebase.indexer.dto.SearchRequest;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.SearchStrategy;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Enhanced semantic search strategy with proper similarity scoring and thresholds
 */
@Component
public class EnhancedSemanticSearchStrategy implements SearchStrategy {

    private final VectorStore vectorStore;
    
    @Value("${search.vector.similarity-threshold:0.7}")
    private double similarityThreshold;
    
    @Value("${search.vector.max-results:50}")
    private int maxResults;

    @Autowired
    public EnhancedSemanticSearchStrategy(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public List<SearchResult> search(SearchRequest request) {
        try {
            // Execute semantic search using simple similarity search
            List<Document> documents = vectorStore.similaritySearch(request.getQuery());

            // Convert to search results with proper scoring
            return documents.stream()
                    .limit(request.getLimit() != null ? request.getLimit() : 10)
                    .map(doc -> convertToSearchResult(doc, request.getQuery()))
                    .sorted((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("❌ Error in enhanced semantic search: " + e.getMessage());
            return List.of();
        }
    }

    private SearchResult convertToSearchResult(Document doc, String query) {
        // Extract similarity score from metadata (if available)
        double similarityScore = extractSimilarityScore(doc);
        
        // Calculate contextual relevance
        double contextualRelevance = calculateContextualRelevance(doc.getText(), query);
        
        // Calculate final relevance score (weighted combination)
        double finalScore = (similarityScore * 0.7) + (contextualRelevance * 0.3);
        
        return new SearchResult(
                doc.getMetadata().getOrDefault("filepath", "").toString(),
                doc.getMetadata().getOrDefault("filename", "").toString(),
                doc.getText(),
                getLineNumber(doc),
                finalScore,
                createSnippet(doc.getText(), query)
        );
    }

    private double extractSimilarityScore(Document doc) {
        // Try to extract similarity score from document metadata
        Object scoreObj = doc.getMetadata().get("distance");
        if (scoreObj instanceof Number) {
            // Convert distance to similarity (assuming cosine distance)
            double distance = ((Number) scoreObj).doubleValue();
            return Math.max(0.0, 1.0 - distance);
        }
        
        // Fallback to default if no score available
        return 0.8;
    }

    private double calculateContextualRelevance(String content, String query) {
        String[] queryTerms = query.toLowerCase().split("\\s+");
        String contentLower = content.toLowerCase();
        
        double relevance = 0.0;
        
        // Term frequency with position weighting
        for (String term : queryTerms) {
            int firstOccurrence = contentLower.indexOf(term);
            if (firstOccurrence != -1) {
                // Higher weight for terms appearing earlier
                double positionWeight = 1.0 - (firstOccurrence / (double) contentLower.length() * 0.3);
                
                // Count total occurrences
                long occurrences = contentLower.split(term, -1).length - 1;
                
                // Add to relevance with diminishing returns
                relevance += Math.log1p(occurrences) * positionWeight;
            }
        }
        
        // Normalize by content length and query length
        return relevance / (Math.sqrt(contentLower.length()) * queryTerms.length);
    }

    private String createSnippet(String content, String query) {
        String[] queryTerms = query.toLowerCase().split("\\s+");
        String[] lines = content.split("\n");
        
        // Find the best matching line
        int bestLine = 0;
        int maxMatches = 0;
        
        for (int i = 0; i < lines.length; i++) {
            String lineLower = lines[i].toLowerCase();
            int matches = 0;
            for (String term : queryTerms) {
                if (lineLower.contains(term)) {
                    matches++;
                }
            }
            if (matches > maxMatches) {
                maxMatches = matches;
                bestLine = i;
            }
        }
        
        // Extract context around best matching line
        int start = Math.max(0, bestLine - 2);
        int end = Math.min(lines.length, bestLine + 3);
        
        StringBuilder snippet = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (i == bestLine) {
                snippet.append(">>> ").append(lines[i].trim()).append("\n");
            } else {
                snippet.append("    ").append(lines[i].trim()).append("\n");
            }
        }
        
        return snippet.toString().trim();
    }

    private int getLineNumber(Document doc) {
        Object lineObj = doc.getMetadata().get("line");
        if (lineObj instanceof Integer) {
            return (Integer) lineObj;
        }
        return 0;
    }

    @Override
    public boolean supports(SearchRequest.SearchType searchType) {
        return searchType == SearchRequest.SearchType.SEMANTIC;
    }

    @Override
    public SearchRequest.SearchType getSearchType() {
        return SearchRequest.SearchType.SEMANTIC;
    }
}
