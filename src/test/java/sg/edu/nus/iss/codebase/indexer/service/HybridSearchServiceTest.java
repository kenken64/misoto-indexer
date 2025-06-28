package sg.edu.nus.iss.codebase.indexer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import sg.edu.nus.iss.codebase.indexer.config.DynamicVectorStoreFactory;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileIndexingService;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HybridSearchServiceTest {

    @Mock
    private FileIndexingService indexingService;

    @Mock
    private SearchService searchService;

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

    private Document mockDocument;
    private FileSearchService.SearchResult mockFileResult;

    @BeforeEach
    void setUp() {
        mockDocument = new Document("test content", Map.of("fileName", "test.java"));
        mockFileResult = new FileSearchService.SearchResult(
            "test.java", "/path/test.java", "test content", 5.0, "file-search"
        );
        
        // Setup lenient default mocks for indexingService to avoid unnecessary stubbing errors
        lenient().when(indexingService.getIndexedFileCount()).thenReturn(100);
        lenient().when(indexingService.getCurrentCollectionName()).thenReturn("test-collection");
        lenient().when(indexingService.getCurrentIndexingDirectory()).thenReturn("/test/directory");
        lenient().when(indexingService.isIndexingComplete()).thenReturn(true);
    }

    @Test
    void performHybridSearch_ShouldReturnCombinedResults_WhenBothVectorAndFileSearchSucceed() {
        // Arrange
        String query = "test query";
        int maxResults = 10;

        when(indexingService.getCurrentCollectionName()).thenReturn("test-collection");
        when(indexingService.getCurrentIndexingDirectory()).thenReturn("/test/directory");
        when(vectorStoreFactory.createVectorStore("test-collection")).thenReturn(vectorStore);
        when(vectorStore.similaritySearch("test query")).thenReturn(List.of(mockDocument));
        when(fileSearchService.searchInFiles(query)).thenReturn(List.of(mockFileResult));
        when(chatModel.call(anyString())).thenReturn("AI analysis of search results");

        // Act
        HybridSearchService.HybridSearchResult result = hybridSearchService.performHybridSearch(query, maxResults);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getVectorResults()).hasSize(1);
        assertThat(result.getFileResults()).hasSize(1);
        assertThat(result.getAiAnalysis()).isEqualTo("AI analysis of search results");
        assertThat(result.getTotalResults()).isEqualTo(2);
    }

    @Test
    void performHybridSearch_ShouldReturnFileResultsOnly_WhenVectorSearchFails() {
        // Arrange
        String query = "test query";
        int maxResults = 10;

        when(indexingService.getCurrentCollectionName()).thenReturn("test-collection");
        when(indexingService.getCurrentIndexingDirectory()).thenReturn("/test/directory");
        when(vectorStoreFactory.createVectorStore("test-collection")).thenReturn(vectorStore);
        when(vectorStore.similaritySearch("test query")).thenReturn(List.of());
        when(fileSearchService.searchInFiles(query)).thenReturn(List.of(mockFileResult));

        // Act
        HybridSearchService.HybridSearchResult result = hybridSearchService.performHybridSearch(query, maxResults);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getVectorResults()).isEmpty();
        assertThat(result.getFileResults()).hasSize(1);
        assertThat(result.getTotalResults()).isEqualTo(1);
    }

    @Test
    void performHybridSearch_ShouldReturnEmptyResults_WhenBothSearchesFail() {
        // Arrange
        String query = "test query";
        int maxResults = 10;

        when(indexingService.getCurrentCollectionName()).thenReturn("test-collection");
        when(indexingService.getCurrentIndexingDirectory()).thenReturn("/test/directory");
        when(vectorStoreFactory.createVectorStore("test-collection")).thenReturn(vectorStore);
        when(vectorStore.similaritySearch("test query")).thenReturn(List.of());
        when(fileSearchService.searchInFiles(query)).thenReturn(List.of());

        // Act
        HybridSearchService.HybridSearchResult result = hybridSearchService.performHybridSearch(query, maxResults);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getVectorResults()).isEmpty();
        assertThat(result.getFileResults()).isEmpty();
        assertThat(result.getTotalResults()).isEqualTo(0);
    }

    @Test
    void performHybridSearch_ShouldHandleNullQuery() {
        // Act
        HybridSearchService.HybridSearchResult result = hybridSearchService.performHybridSearch(null, 10);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getVectorResults()).isEmpty();
        assertThat(result.getFileResults()).isEmpty();
        assertThat(result.getTotalResults()).isEqualTo(0);
    }

    @Test
    void performHybridSearch_ShouldHandleEmptyQuery() {
        // Act
        HybridSearchService.HybridSearchResult result = hybridSearchService.performHybridSearch("", 10);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getVectorResults()).isEmpty();
        assertThat(result.getFileResults()).isEmpty();
        assertThat(result.getTotalResults()).isEqualTo(0);
    }
}
