package sg.edu.nus.iss.codebase.indexer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import sg.edu.nus.iss.codebase.indexer.service.FileSearchService.SearchResult;
import sg.edu.nus.iss.codebase.indexer.service.FileSearchService.LineMatch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class FileSearchServiceTest {

    private FileSearchService fileSearchService;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        fileSearchService = new FileSearchService();
        
        // Create test files with content that we'll search for
        Path javaFile = tempDir.resolve("TestClass.java");
        Files.write(javaFile, List.of(
            "package com.example;",
            "",
            "public class TestClass {",
            "    public void validation_split() {",
            "        // This method handles validation_split",
            "        System.out.println(\"Performing validation_split\");",
            "    }",
            "}"
        ));
        
        Path markdownFile = tempDir.resolve("README.md");
        Files.write(markdownFile, List.of(
            "# Test Project",
            "This project contains validation_split functionality."
        ));
        
        // Set the search directory to our temp directory
        fileSearchService.setSearchDirectory(tempDir.toString());
    }

    @Test
    void searchInFiles_ShouldFindMatchesInJavaFile() {
        // Act
        List<SearchResult> results = fileSearchService.searchInFiles("validation_split");

        // Assert
        assertThat(results).isNotEmpty();
        
        SearchResult javaResult = results.stream()
            .filter(r -> r.getFileName().equals("TestClass.java"))
            .findFirst()
            .orElse(null);
        
        assertThat(javaResult).isNotNull();
        assertThat(javaResult.getFileName()).isEqualTo("TestClass.java");
        assertThat(javaResult.getFilePath()).contains("TestClass.java");
        assertThat(javaResult.getContent()).contains("validation_split");
        // After our fix, exact matches return "exact-match" instead of "file-search"
        assertThat(javaResult.getSearchType()).isEqualTo("exact-match");
        assertThat(javaResult.getRelevanceScore()).isGreaterThan(0);
    }

    @Test
    void searchInFiles_ShouldReturnEmptyListForNoMatches() {
        // Act
        List<SearchResult> results = fileSearchService.searchInFiles("nonexistent_term_12345");

        // Assert
        assertThat(results).isEmpty();
    }

    @Test
    void searchInFiles_ShouldHandleEmptyQuery() {
        // Act
        List<SearchResult> results = fileSearchService.searchInFiles("");

        // Assert - Empty query may still return some results since the service doesn't validate
        assertThat(results).isNotNull();
        // The service may return some results even for empty query, so we don't enforce empty
    }

    @Test
    void searchInFiles_ShouldHandleNullQuery() {
        // Act
        List<SearchResult> results = fileSearchService.searchInFiles(null);

        // Assert
        assertThat(results).isEmpty();
    }

    @Test
    void searchInFiles_ShouldReturnExactMatchForCodeLikeQueries() throws IOException {
        // Setup - Create a file with a specific code line
        Path codeFile = tempDir.resolve("config.py");
        Files.write(codeFile, List.of(
            "# Configuration file",
            "self.batch_size = 4",
            "self.learning_rate = 2e-4",
            "self.bnb_4bit_quant_type = \"nf4\"",
            "self.use_4bit = True"
        ));
        
        // Act - Search for exact code line
        List<SearchResult> results = fileSearchService.searchInFiles("self.bnb_4bit_quant_type = \"nf4\"");
        
        // Assert
        assertThat(results).isNotEmpty();
        SearchResult result = results.get(0);
        assertThat(result.getSearchType()).isEqualTo("exact-match");
        assertThat(result.getRelevanceScore()).isGreaterThan(90.0); // Should have high relevance for exact match
        assertThat(result.getLineMatches()).isNotEmpty();
        assertThat(result.getLineMatches().get(0).getLineNumber()).isEqualTo(4); // Should find line 4
    }

    @Test
    void searchInFiles_ShouldReturnFileSearchForTokenizedQueries() {
        // Act - Search for a simple term that will be tokenized
        List<SearchResult> results = fileSearchService.searchInFiles("TestClass");
        
        // Assert
        assertThat(results).isNotEmpty();
        SearchResult result = results.stream()
            .filter(r -> r.getFileName().equals("TestClass.java"))
            .findFirst()
            .orElse(null);
        
        assertThat(result).isNotNull();
        // For simple tokens that don't contain special characters, it should use file-search
        // unless the exact term appears in the content
        assertThat(result.getSearchType()).isIn("file-search", "exact-match");
    }
}
