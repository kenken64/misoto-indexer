package sg.edu.nus.iss.codebase.indexer.service.impl.search;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sg.edu.nus.iss.codebase.indexer.dto.SearchRequest;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.SearchStrategy;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Semantic search strategy using vector similarity
 * Implements Strategy Pattern for semantic search
 */
@Component
public class SemanticSearchStrategy implements SearchStrategy {

    private final VectorStore vectorStore;

    @Autowired
    public SemanticSearchStrategy(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public List<SearchResult> search(SearchRequest request) {
        try {
            // Execute semantic search using vector store
            List<Document> documents = vectorStore.similaritySearch(request.getQuery());

            // Convert to search results
            return documents.stream()
                    .limit(request.getLimit() != null ? request.getLimit() : 10)
                    .map(doc -> new SearchResult(
                            doc.getMetadata().getOrDefault("filepath", "").toString(),
                            doc.getMetadata().getOrDefault("filename", "").toString(),
                            doc.getText(),
                            getLineNumber(doc),
                            1.0, // Relevance score would come from vector similarity
                            createSnippet(doc.getText())
                    ))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("❌ Error in semantic search: " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public boolean supports(SearchRequest.SearchType searchType) {
        return searchType == SearchRequest.SearchType.SEMANTIC;
    }

    @Override
    public SearchRequest.SearchType getSearchType() {
        return SearchRequest.SearchType.SEMANTIC;
    }

    private int getLineNumber(Document doc) {
        // Try to extract line number from metadata if available
        Object lineObj = doc.getMetadata().get("line");
        if (lineObj instanceof Integer) {
            return (Integer) lineObj;
        }
        return 0;
    }

    private String createSnippet(String content) {
        if (content.length() > 200) {
            return content.substring(0, 200) + "...";
        }
        return content;
    }
}
