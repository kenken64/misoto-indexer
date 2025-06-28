package sg.edu.nus.iss.codebase.indexer.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import sg.edu.nus.iss.codebase.indexer.config.IndexingConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TextDocumentFactoryTest {

    @Mock
    private IndexingConfiguration config;

    @InjectMocks
    private TextDocumentFactory textDocumentFactory;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        IndexingConfiguration.ProcessingConfig processingConfig = new IndexingConfiguration.ProcessingConfig();
        processingConfig.setChunkSize(1000);
        processingConfig.setChunkOverlap(100);
        
        lenient().when(config.getProcessing()).thenReturn(processingConfig);
        lenient().when(config.getFilePriorities()).thenReturn(Map.of(
            "Application.java", 1, // Main application classes (highest priority)
            "Test.java", 2, // Test files
            "Service.java", 3, // Business logic services  
            "Repository.java", 4, // Data access
            "Controller.java", 5, // REST controllers
            "Config.java", 6, // Configuration classes
            ".xml", 7, // Configuration files
            ".properties", 8, // Properties files
            ".md", 9 // Documentation
        ));
    }

    @Test
    void createDocuments_ShouldCreateDocumentFromJavaFile() throws IOException {
        // Arrange
        Path javaFile = tempDir.resolve("TestClass.java");
        String content = """
            package com.example;
            
            public class TestClass {
                private String name;
                
                public TestClass(String name) {
                    this.name = name;
                }
                
                public String getName() {
                    return name;
                }
            }
            """;
        Files.writeString(javaFile, content);

        // Act
        List<Document> documents = textDocumentFactory.createDocuments(javaFile.toFile());

        // Assert
        assertThat(documents).hasSize(1);
        Document document = documents.get(0);
        assertThat(document.getFormattedContent()).contains("public class TestClass");
        assertThat(document.getFormattedContent()).contains("private String name");
        assertThat(document.getMetadata()).containsKey("filepath");
        assertThat(document.getMetadata()).containsKey("filename");
        assertThat(document.getMetadata()).containsKey("filetype");
        assertThat(document.getMetadata()).containsKey("size");
        assertThat(document.getMetadata()).containsKey("priority");
    }

    @Test
    void createDocuments_ShouldCreateDocumentFromPythonFile() throws IOException {
        // Arrange
        Path pythonFile = tempDir.resolve("test_script.py");
        String content = """
            def hello_world():
                print("Hello, World!")
            
            if __name__ == "__main__":
                hello_world()
            """;
        Files.writeString(pythonFile, content);

        // Act
        List<Document> documents = textDocumentFactory.createDocuments(pythonFile.toFile());

        // Assert
        assertThat(documents).hasSize(1);
        Document document = documents.get(0);
        assertThat(document.getFormattedContent()).contains("def hello_world()");
        assertThat(document.getFormattedContent()).contains("print(\"Hello, World!\")");
        assertThat(document.getMetadata()).containsKey("filepath");
        assertThat(document.getMetadata()).containsKey("filename");
        assertThat(document.getMetadata()).containsKey("filetype");
        assertThat(document.getMetadata()).containsKey("size");
    }

    @Test
    void createDocuments_ShouldCreateDocumentFromMarkdownFile() throws IOException {
        // Arrange
        Path markdownFile = tempDir.resolve("README.md");
        String content = """
            # Project Title
            
            This is a sample project.
            
            ## Features
            
            - Feature 1
            - Feature 2
            
            ## Installation
            
            Run the following command:
            
            ```bash
            npm install
            ```
            """;
        Files.writeString(markdownFile, content);

        // Act
        List<Document> documents = textDocumentFactory.createDocuments(markdownFile.toFile());

        // Assert
        assertThat(documents).hasSize(1);
        Document document = documents.get(0);
        assertThat(document.getFormattedContent()).contains("# Project Title");
        assertThat(document.getFormattedContent()).contains("## Features");
        assertThat(document.getFormattedContent()).contains("npm install");
        assertThat(document.getMetadata()).containsKey("filepath");
        assertThat(document.getMetadata()).containsKey("filename");
        assertThat(document.getMetadata()).containsKey("filetype");
        assertThat(document.getMetadata()).containsKey("size");
    }

    @Test
    void createDocuments_ShouldCreateDocumentFromTextFile() throws IOException {
        // Arrange
        Path textFile = tempDir.resolve("notes.txt");
        String content = """
            These are my notes.
            Line 1
            Line 2
            Line 3
            """;
        Files.writeString(textFile, content);

        // Act
        List<Document> documents = textDocumentFactory.createDocuments(textFile.toFile());

        // Assert
        assertThat(documents).hasSize(1);
        Document document = documents.get(0);
        assertThat(document.getFormattedContent()).contains("These are my notes");
        assertThat(document.getFormattedContent()).contains("Line 1");
        assertThat(document.getMetadata()).containsKey("filepath");
        assertThat(document.getMetadata()).containsKey("filename");
        assertThat(document.getMetadata()).containsKey("filetype");
        assertThat(document.getMetadata()).containsKey("size");
    }

    @Test
    void createDocuments_ShouldCreateMultipleDocumentsForLargeFile() throws IOException {
        // Arrange
        Path largeFile = tempDir.resolve("large_file.java");
        StringBuilder content = new StringBuilder();
        content.append("package com.example;\n\n");
        content.append("public class LargeClass {\n");
        
        // Create content larger than chunk size
        for (int i = 0; i < 100; i++) {
            content.append("    public void method").append(i).append("() {\n");
            content.append("        // This is method ").append(i).append("\n");
            content.append("        System.out.println(\"Method ").append(i).append("\");\n");
            content.append("    }\n\n");
        }
        content.append("}\n");
        
        Files.writeString(largeFile, content.toString());

        // Act
        List<Document> documents = textDocumentFactory.createDocuments(largeFile.toFile());

        // Assert
        assertThat(documents).hasSizeGreaterThan(1);
        
        // All documents should have metadata
        for (Document document : documents) {
            assertThat(document.getMetadata()).containsKey("filepath");
            assertThat(document.getMetadata()).containsKey("filename");
            assertThat(document.getMetadata()).containsKey("filetype");
            assertThat(document.getMetadata()).containsKey("size");
        }
        
        // Content should be properly chunked
        String fullContent = String.join("", documents.stream()
            .map(Document::getFormattedContent)
            .toArray(String[]::new));
        assertThat(fullContent).contains("public class LargeClass");
        assertThat(fullContent).contains("Method 0");
        assertThat(fullContent).contains("Method 99");
    }

    @Test
    void createDocuments_ShouldHandleEmptyFile() throws IOException {
        // Arrange
        Path emptyFile = tempDir.resolve("empty.txt");
        Files.writeString(emptyFile, "");

        // Act
        List<Document> documents = textDocumentFactory.createDocuments(emptyFile.toFile());

        // Assert - Empty files are rejected due to minimum content length requirement
        assertThat(documents).hasSize(0);
    }

    @Test
    void createDocuments_ShouldSetCorrectPriorityForMainFiles() throws IOException {
        // Arrange
        Path mainFile = tempDir.resolve("MainApplication.java");
        String content = """
            package com.example;
            
            public class MainApplication {
                public static void main(String[] args) {
                    System.out.println("Hello World");
                }
            }
            """;
        Files.writeString(mainFile, content);

        // Act
        List<Document> documents = textDocumentFactory.createDocuments(mainFile.toFile());

        // Assert
        assertThat(documents).hasSize(1);
        Document document = documents.get(0);
        assertThat(document.getMetadata().get("priority")).isEqualTo("1");
    }

    @Test
    void createDocuments_ShouldSetCorrectPriorityForServiceFiles() throws IOException {
        // Arrange
        Path serviceFile = tempDir.resolve("UserService.java");
        String content = """
            package com.example.service;
            
            @Service
            public class UserService {
                public User findById(Long id) {
                    return userRepository.findById(id);
                }
            }
            """;
        Files.writeString(serviceFile, content);

        // Act
        List<Document> documents = textDocumentFactory.createDocuments(serviceFile.toFile());

        // Assert
        assertThat(documents).hasSize(1);
        Document document = documents.get(0);
        assertThat(document.getMetadata().get("priority")).isEqualTo("3");
    }

    @Test
    void createDocuments_ShouldSetCorrectPriorityForTestFiles() throws IOException {
        // Arrange
        Path testFile = tempDir.resolve("UserServiceTest.java");
        String content = """
            package com.example.service;
            
            @ExtendWith(MockitoExtension.class)
            class UserServiceTest {
                @Test
                void shouldFindUserById() {
                    // test implementation
                }
            }
            """;
        Files.writeString(testFile, content);

        // Act
        List<Document> documents = textDocumentFactory.createDocuments(testFile.toFile());

        // Assert
        assertThat(documents).hasSize(1);
        Document document = documents.get(0);
        assertThat(document.getMetadata().get("priority")).isEqualTo("2"); // Test files have priority 2
    }

    @Test
    void createDocuments_ShouldSetDefaultPriorityForOtherFiles() throws IOException {
        // Arrange
        Path otherFile = tempDir.resolve("RandomClass.java");
        String content = """
            package com.example;
            
            public class RandomClass {
                private String data;
            }
            """;
        Files.writeString(otherFile, content);

        // Act
        List<Document> documents = textDocumentFactory.createDocuments(otherFile.toFile());

        // Assert
        assertThat(documents).hasSize(1);
        Document document = documents.get(0);
        assertThat(document.getMetadata().get("priority")).isEqualTo("10"); // Default priority
    }

    @Test
    void createDocuments_ShouldReturnEmptyListForNonExistentFile() {
        // Arrange
        Path nonExistentFile = tempDir.resolve("non_existent.txt");

        // Act
        List<Document> documents = textDocumentFactory.createDocuments(nonExistentFile.toFile());

        // Assert
        assertThat(documents).isEmpty();
    }

    @Test
    void createDocuments_ShouldProcessSpecialCharacters() throws IOException {
        // Arrange
        Path specialFile = tempDir.resolve("special_chars.txt");
        String content = "Special characters: éñüñ 中文 русский العربية 🚀\nThis is a longer text to ensure it meets the minimum length requirement for document creation.";
        Files.writeString(specialFile, content);

        // Act
        List<Document> documents = textDocumentFactory.createDocuments(specialFile.toFile());

        // Assert
        assertThat(documents).hasSize(1);
        Document document = documents.get(0);
        // Note: Special characters may be sanitized for embedding compatibility
        assertThat(document.getFormattedContent()).contains("This is a longer text");
        assertThat(document.getFormattedContent()).contains("longer text to ensure");
    }

    @Test
    void createDocuments_ShouldSanitizeContent() throws IOException {
        // Arrange
        Path binaryFile = tempDir.resolve("binary_content.txt");
        String content = "Normal text with enough content to meet minimum length requirements\u0000\u0001\u0002ABC and more content to ensure processing.";
        Files.writeString(binaryFile, content);

        // Act
        List<Document> documents = textDocumentFactory.createDocuments(binaryFile.toFile());

        // Assert
        assertThat(documents).hasSize(1);
        Document document = documents.get(0);
        assertThat(document.getFormattedContent()).contains("ABC"); // Should extract readable text
        assertThat(document.getFormattedContent()).doesNotContain("\u0000"); // Should sanitize null bytes
    }

    @Test
    void supports_ShouldReturnTrueForSupportedExtensions() {
        assertThat(textDocumentFactory.supports(tempDir.resolve("test.java").toFile())).isTrue();
        assertThat(textDocumentFactory.supports(tempDir.resolve("test.py").toFile())).isTrue();
        assertThat(textDocumentFactory.supports(tempDir.resolve("test.js").toFile())).isTrue();
        assertThat(textDocumentFactory.supports(tempDir.resolve("test.ts").toFile())).isTrue();
        assertThat(textDocumentFactory.supports(tempDir.resolve("test.html").toFile())).isTrue();
        assertThat(textDocumentFactory.supports(tempDir.resolve("test.css").toFile())).isTrue();
        assertThat(textDocumentFactory.supports(tempDir.resolve("test.md").toFile())).isTrue();
        assertThat(textDocumentFactory.supports(tempDir.resolve("test.txt").toFile())).isTrue();
        assertThat(textDocumentFactory.supports(tempDir.resolve("test.xml").toFile())).isTrue();
        assertThat(textDocumentFactory.supports(tempDir.resolve("test.json").toFile())).isTrue();
        assertThat(textDocumentFactory.supports(tempDir.resolve("test.yml").toFile())).isTrue();
        assertThat(textDocumentFactory.supports(tempDir.resolve("test.yaml").toFile())).isTrue();
        assertThat(textDocumentFactory.supports(tempDir.resolve("test.properties").toFile())).isTrue();
        assertThat(textDocumentFactory.supports(tempDir.resolve("test.sql").toFile())).isTrue();
        assertThat(textDocumentFactory.supports(tempDir.resolve("test.sh").toFile())).isTrue();
        assertThat(textDocumentFactory.supports(tempDir.resolve("test.go").toFile())).isTrue();
        assertThat(textDocumentFactory.supports(tempDir.resolve("test.cpp").toFile())).isTrue();
        assertThat(textDocumentFactory.supports(tempDir.resolve("test.c").toFile())).isTrue();
    }

    @Test
    void supports_ShouldReturnFalseForUnsupportedExtensions() {
        assertThat(textDocumentFactory.supports(tempDir.resolve("test.pdf").toFile())).isFalse();
        assertThat(textDocumentFactory.supports(tempDir.resolve("test.docx").toFile())).isFalse();
        assertThat(textDocumentFactory.supports(tempDir.resolve("test.xlsx").toFile())).isFalse();
        assertThat(textDocumentFactory.supports(tempDir.resolve("test.png").toFile())).isFalse();
        assertThat(textDocumentFactory.supports(tempDir.resolve("test.jpg").toFile())).isFalse();
        assertThat(textDocumentFactory.supports(tempDir.resolve("test.mp4").toFile())).isFalse();
        assertThat(textDocumentFactory.supports(tempDir.resolve("test.zip").toFile())).isFalse();
    }

    @Test
    void supports_ShouldReturnFalseForFilesWithoutExtension() {
        // TextDocumentFactory only supports files with known extensions
        assertThat(textDocumentFactory.supports(tempDir.resolve("Dockerfile").toFile())).isFalse();
        assertThat(textDocumentFactory.supports(tempDir.resolve("Makefile").toFile())).isFalse();
        assertThat(textDocumentFactory.supports(tempDir.resolve("README").toFile())).isFalse();
    }
}
