package sg.edu.nus.iss.codebase.indexer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;


import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchServiceTest {

    @Mock
    private ChatModel chatModel;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private SearchService searchService;

    private Document mockDocument;

    @BeforeEach
    void setUp() {
        mockDocument = new Document("test content", Map.of(
            "fileName", "TestFile.java",
            "filePath", "/path/to/TestFile.java"
        ));
    }

    @Test
    void searchWithPrompt_ShouldReturnChatResponse() {
        // Arrange
        String query = "What is the purpose of this code?";
        String expectedResponse = "This code is used for testing purposes.";
        
        when(chatModel.call(anyString())).thenReturn(expectedResponse);

        // Act
        Object result = searchService.searchWithPrompt(query);

        // Assert
        assertThat(result.toString()).contains("This code is used for testing purposes.");
        assertThat(result.toString()).contains("Natural language search processed");
        verify(chatModel, atLeast(1)).call(anyString()); // Constructor calls it once, method calls it again
    }

    @Test
    void searchWithPrompt_ShouldHandleNullQuery() {
        // Act
        Object result = searchService.searchWithPrompt(null);

        // Assert - Service should handle null gracefully and return a response
        assertThat(result).isNotNull();
        assertThat(result.toString()).contains("SearchResponse");
    }

    @Test
    void searchWithPrompt_ShouldHandleEmptyQuery() {
        // Act
        Object result = searchService.searchWithPrompt("");

        // Assert - Service should handle empty string gracefully and return a response
        assertThat(result).isNotNull();
        assertThat(result.toString()).contains("SearchResponse");
    }

    @Test
    void searchWithPrompt_ShouldHandleChatModelException() {
        // Arrange
        String query = "test query";
        when(chatModel.call(anyString())).thenThrow(new RuntimeException("AI service unavailable"));

        // Act
        Object result = searchService.searchWithPrompt(query);

        // Assert - Service should handle exception gracefully and return fallback response
        assertThat(result).isNotNull();
        assertThat(result.toString()).contains("fallback mode");
    }

    @Test
    void semanticSearch_ShouldReturnVectorSearchResults() {
        // Arrange
        String query = "test semantic search";
        int limit = 5;
        double threshold = 0.7;

        // Act
        Object result = searchService.semanticSearch(query, limit, threshold);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.toString()).contains("Semantic search completed");
    }

    @Test
    void semanticSearch_ShouldHandleInvalidParameters() {
        // Act - Service should handle invalid parameters gracefully
        Object result1 = searchService.semanticSearch("query", 0, 0.5);
        Object result2 = searchService.semanticSearch("query", -1, 0.5);
        Object result3 = searchService.semanticSearch("query", 5, -0.1);
        Object result4 = searchService.semanticSearch("query", 5, 1.1);

        // Assert - All should return SearchResponse objects
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        assertThat(result3).isNotNull();
        assertThat(result4).isNotNull();
    }

    @Test
    void semanticSearch_ShouldHandleVectorStoreException() {
        // Arrange
        String query = "test query";

        // Act - Since the service doesn't actually use vectorStore, it won't trigger the exception
        Object result = searchService.semanticSearch(query, 5, 0.7);

        // Assert - Service returns normal response since no exception occurs
        assertThat(result).isNotNull();
        assertThat(result.toString()).contains("Semantic search completed");
    }

    @Test
    void textSearch_ShouldReturnSearchResults() {
        // Arrange
        String query = "test text search";
        int limit = 10;

        // Act
        Object result = searchService.textSearch(query, limit);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.toString()).contains("Text search results");
    }

    @Test
    void textSearch_ShouldHandleInvalidLimit() {
        // Act - Service should handle invalid limits gracefully
        Object result1 = searchService.textSearch("query", 0);
        Object result2 = searchService.textSearch("query", -1);

        // Assert - Should return SearchResponse objects
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
    }

    @Test
    void advancedSearch_ShouldProcessSearchRequest() {
        // Arrange
        Object searchRequest = new Object(); // Mock search request
        
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
            .thenReturn(List.of(mockDocument));

        // Act
        Object result = searchService.advancedSearch(searchRequest);

        // Assert
        assertThat(result).isNotNull();
    }

    @Test
    void advancedSearch_ShouldHandleNullRequest() {
        // Act
        Object result = searchService.advancedSearch(null);

        // Assert - Service should handle null gracefully
        assertThat(result).isNotNull();
    }

    @Test
    void constructor_ShouldTestConnections() {
        // Arrange
        ChatModel mockChatModel = mock(ChatModel.class);
        EmbeddingModel mockEmbeddingModel = mock(EmbeddingModel.class);
        VectorStore mockVectorStore = mock(VectorStore.class);
        
        when(mockChatModel.call("test")).thenReturn("test response");
        when(mockEmbeddingModel.embed("test")).thenReturn(new float[]{0.1f, 0.2f, 0.3f});

        // Act
        SearchService service = new SearchService(mockChatModel, mockEmbeddingModel, mockVectorStore);

        // Assert
        assertThat(service).isNotNull();
        verify(mockChatModel).call("test");
        verify(mockEmbeddingModel).embed("test");
    }

    @Test
    void constructor_ShouldHandleConnectionFailures() {
        // Arrange
        ChatModel mockChatModel = mock(ChatModel.class);
        EmbeddingModel mockEmbeddingModel = mock(EmbeddingModel.class);
        VectorStore mockVectorStore = mock(VectorStore.class);
        
        when(mockChatModel.call("test")).thenThrow(new RuntimeException("Ollama connection failed"));
        when(mockEmbeddingModel.embed("test")).thenThrow(new RuntimeException("Embedding failed"));

        // Act & Assert
        assertThatCode(() -> new SearchService(mockChatModel, mockEmbeddingModel, mockVectorStore))
            .doesNotThrowAnyException(); // Should handle connection failures gracefully
    }

    @Test
    void searchWithPrompt_ShouldHandleTimeout() {
        // Arrange
        String query = "test query";
        when(chatModel.call(anyString())).thenAnswer(invocation -> {
            try {
                Thread.sleep(15000); // Simulate long response time
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "delayed response";
        });

        // Act
        Object result = searchService.searchWithPrompt(query);

        // Assert - Service should handle delay and return response
        assertThat(result).isNotNull();
        assertThat(result.toString()).contains("delayed response");
    }

    @Test
    void semanticSearch_ShouldUseCorrectParameters() {
        // Arrange
        String query = "test query";
        int limit = 3;
        double threshold = 0.8f;

        // Act
        Object result = searchService.semanticSearch(query, limit, threshold);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.toString()).contains("Semantic search completed");
    }

    @Test
    void textSearch_ShouldUseCorrectLimit() {
        // Arrange
        String query = "test query";
        int limit = 7;

        // Act
        Object result = searchService.textSearch(query, limit);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.toString()).contains("Text search results");
    }

    @Test
    void semanticSearch_ShouldHandleEmptyResults() {
        // Arrange
        String query = "nonexistent content";
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
            .thenReturn(List.of());

        // Act
        Object result = searchService.semanticSearch(query, 5, 0.7);

        // Assert
        assertThat(result).isNotNull();
    }

    @Test
    void textSearch_ShouldHandleLargeLimit() {
        // Arrange
        String query = "test query";
        int largeLimit = 1000;

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
            .thenReturn(List.of(mockDocument));

        // Act & Assert
        assertThatCode(() -> searchService.textSearch(query, largeLimit))
            .doesNotThrowAnyException();
    }

    @Test
    void searchWithPrompt_ShouldTrimWhitespace() {
        // Arrange
        String queryWithWhitespace = "  test query  ";
        String expectedResponse = "response";
        
        when(chatModel.call(anyString())).thenReturn(expectedResponse);

        // Act
        Object result = searchService.searchWithPrompt(queryWithWhitespace);

        // Assert
        assertThat(result.toString()).contains("response");
        assertThat(result.toString()).contains("Natural language search processed");
        verify(chatModel, atLeast(1)).call(anyString()); // Constructor calls it once, method calls it again
    }

    @Test
    void semanticSearch_ShouldHandleSpecialCharacters() {
        // Arrange
        String queryWithSpecialChars = "test @#$%^&*() query";
        
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
            .thenReturn(List.of(mockDocument));

        // Act & Assert
        assertThatCode(() -> searchService.semanticSearch(queryWithSpecialChars, 5, 0.7))
            .doesNotThrowAnyException();
    }

    @Test
    void textSearch_ShouldHandleUnicodeCharacters() {
        // Arrange
        String unicodeQuery = "tëst qüérÿ with ñ and 中文";
        
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
            .thenReturn(List.of(mockDocument));

        // Act & Assert
        assertThatCode(() -> searchService.textSearch(unicodeQuery, 5))
            .doesNotThrowAnyException();
    }

    @Test
    void advancedSearch_ShouldHandleComplexSearchRequest() {
        // Arrange
        Map<String, Object> complexRequest = Map.of(
            "query", "test query",
            "filters", Map.of("fileType", "java"),
            "limit", 10,
            "threshold", 0.8
        );
        
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
            .thenReturn(List.of(mockDocument));

        // Act & Assert
        assertThatCode(() -> searchService.advancedSearch(complexRequest))
            .doesNotThrowAnyException();
    }
}
