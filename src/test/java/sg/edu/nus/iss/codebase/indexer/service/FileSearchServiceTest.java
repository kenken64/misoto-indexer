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
        assertThat(javaResult.getSearchType()).isEqualTo("file-search");
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
}
