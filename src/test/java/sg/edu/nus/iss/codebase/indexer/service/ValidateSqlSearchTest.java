package sg.edu.nus.iss.codebase.indexer.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import sg.edu.nus.iss.codebase.indexer.config.DynamicVectorStoreFactory;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileIndexingService;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Test to demonstrate that search for validate-sql endpoint works correctly
 */
@ExtendWith(MockitoExtension.class)
class ValidateSqlSearchTest {

    @Mock
    private FileIndexingService indexingService;

    @Mock
    private FileSearchService fileSearchService;

    @Mock
    private ChatModel chatModel;

    @Mock
    private DynamicVectorStoreFactory vectorStoreFactory;

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private HybridSearchService hybridSearchService;

    @Test
    void searchForValidateSqlEndpoint_ShouldFindTheEndpoint() {
        // Arrange - Create a mock document containing the validate-sql endpoint
        String appPyContent = """
            @app.route('/api/validate-sql', methods=['POST'])
            def validate_sql():
                \"\"\"API endpoint to validate SQL syntax\"\"\"
                try:
                    data = request.get_json()
                    sql_query = data.get('sql', '').strip()
                    
                    if not sql_query:
                        return jsonify({
                            'success': False,
                            'error': 'SQL query is required'
                        })
                    
                    is_valid, message = SQLValidator.validate_sql(sql_query)
                    
                    return jsonify({
                        'success': True,
                        'is_valid': is_valid,
                        'message': message
                    })
                
                except Exception as e:
                    return jsonify({
                        'success': False,
                        'error': str(e)
                    })
            """;

        Document mockDocument = new Document(appPyContent, Map.of(
            "filename", "app.py",
            "filepath", "d:/Projects/misoto-indexer/codebase/dssi-day3-ollama/app.py",
            "filetype", ".py"
        ));

        FileSearchService.SearchResult mockFileResult = new FileSearchService.SearchResult(
            "app.py", 
            "d:/Projects/misoto-indexer/codebase/dssi-day3-ollama/app.py", 
            appPyContent, 
            0.95, 
            "file-search"
        );

        // Setup mocks
        lenient().when(indexingService.getIndexedFileCount()).thenReturn(100);
        lenient().when(indexingService.getCurrentCollectionName()).thenReturn("codebase-index-dssi-day3-ollama");
        lenient().when(indexingService.getCurrentIndexingDirectory()).thenReturn("codebase/dssi-day3-ollama");
        lenient().when(indexingService.isIndexingComplete()).thenReturn(true);

        when(vectorStoreFactory.createVectorStore("codebase-index-dssi-day3-ollama")).thenReturn(vectorStore);
        when(vectorStore.similaritySearch("validate-sql endpoint")).thenReturn(List.of(mockDocument));
        when(fileSearchService.searchInFiles("validate-sql endpoint")).thenReturn(List.of(mockFileResult));
        when(chatModel.call(anyString())).thenReturn("Found validate-sql endpoint in app.py");

        // Act - Search for the validate-sql endpoint
        HybridSearchService.HybridSearchResult result = hybridSearchService.performHybridSearch("validate-sql endpoint", 10);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalResults()).isGreaterThan(0);

        // Check vector search results
        if (!result.getVectorResults().isEmpty()) {
            boolean foundInVector = result.getVectorResults().stream()
                .anyMatch(r -> r.getContent().contains("@app.route('/api/validate-sql'"));
            assertThat(foundInVector).isTrue();
        }

        // Check file search results
        if (!result.getFileResults().isEmpty()) {
            boolean foundInFile = result.getFileResults().stream()
                .anyMatch(r -> r.getContent().contains("@app.route('/api/validate-sql'"));
            assertThat(foundInFile).isTrue();
        }

        // At least one search method should have found it
        boolean foundEndpoint = (!result.getVectorResults().isEmpty() && 
                                result.getVectorResults().stream().anyMatch(r -> r.getContent().contains("@app.route('/api/validate-sql'"))) ||
                               (!result.getFileResults().isEmpty() && 
                                result.getFileResults().stream().anyMatch(r -> r.getContent().contains("@app.route('/api/validate-sql'")));
        
        assertThat(foundEndpoint).withFailMessage("Should find the validate-sql endpoint").isTrue();
    }

    @Test
    void searchForSqlValidationQueries_ShouldFindRelevantResults() {
        // Test different search queries that should find the endpoint
        String[] searchQueries = {
            "validate sql endpoint",
            "app.route validate-sql", 
            "server side endpoint validate SQL syntax",
            "POST api validate sql",
            "@app.route validate",
            "validate_sql function"
        };

        // Mock a simple result for each query
        FileSearchService.SearchResult mockResult = new FileSearchService.SearchResult(
            "app.py", 
            "codebase/dssi-day3-ollama/app.py", 
            "@app.route('/api/validate-sql', methods=['POST'])\ndef validate_sql():", 
            0.9, 
            "file-search"
        );

        // Setup basic mocks
        lenient().when(indexingService.getIndexedFileCount()).thenReturn(100);
        lenient().when(indexingService.getCurrentCollectionName()).thenReturn("codebase-index-dssi-day3-ollama");
        lenient().when(indexingService.getCurrentIndexingDirectory()).thenReturn("codebase/dssi-day3-ollama");
        lenient().when(indexingService.isIndexingComplete()).thenReturn(true);

        for (String query : searchQueries) {
            when(fileSearchService.searchInFiles(query)).thenReturn(List.of(mockResult));
            
            // Act
            HybridSearchService.HybridSearchResult result = hybridSearchService.performHybridSearch(query, 10);
            
            // Assert
            assertThat(result).withFailMessage("Query '" + query + "' should return results").isNotNull();
            assertThat(result.getTotalResults()).withFailMessage("Query '" + query + "' should find results").isGreaterThan(0);
        }
    }
}
