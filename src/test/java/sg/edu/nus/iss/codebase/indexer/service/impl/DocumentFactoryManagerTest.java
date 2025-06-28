package sg.edu.nus.iss.codebase.indexer.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.document.Document;

import sg.edu.nus.iss.codebase.indexer.service.interfaces.DocumentFactory;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentFactoryManagerTest {

    @Mock
    private DocumentFactory javaDocumentFactory;

    @Mock
    private DocumentFactory textDocumentFactory;

    @Mock
    private DocumentFactory xmlDocumentFactory;

    private DocumentFactoryManager documentFactoryManager;

    private File mockJavaFile;
    private File mockTextFile;
    private File mockUnsupportedFile;

    @BeforeEach
    void setUp() {
        List<DocumentFactory> factories = List.of(
            javaDocumentFactory,
            textDocumentFactory,
            xmlDocumentFactory
        );
        
        documentFactoryManager = new DocumentFactoryManager(factories);

        // Setup mock files
        mockJavaFile = mock(File.class);
        when(mockJavaFile.getName()).thenReturn("TestClass.java");
        when(mockJavaFile.getAbsolutePath()).thenReturn("/path/to/TestClass.java");

        mockTextFile = mock(File.class);
        when(mockTextFile.getName()).thenReturn("README.txt");
        when(mockTextFile.getAbsolutePath()).thenReturn("/path/to/README.txt");

        mockUnsupportedFile = mock(File.class);
        when(mockUnsupportedFile.getName()).thenReturn("binary.exe");
        when(mockUnsupportedFile.getAbsolutePath()).thenReturn("/path/to/binary.exe");

        // Setup factory support
        when(javaDocumentFactory.supports(mockJavaFile)).thenReturn(true);
        when(javaDocumentFactory.supports(mockTextFile)).thenReturn(false);
        when(javaDocumentFactory.supports(mockUnsupportedFile)).thenReturn(false);
        when(javaDocumentFactory.getSupportedExtensions()).thenReturn(new String[]{".java", ".kt", ".scala"});

        when(textDocumentFactory.supports(mockJavaFile)).thenReturn(false);
        when(textDocumentFactory.supports(mockTextFile)).thenReturn(true);
        when(textDocumentFactory.supports(mockUnsupportedFile)).thenReturn(false);
        when(textDocumentFactory.getSupportedExtensions()).thenReturn(new String[]{".txt", ".md", ".rst"});

        when(xmlDocumentFactory.supports(mockJavaFile)).thenReturn(false);
        when(xmlDocumentFactory.supports(mockTextFile)).thenReturn(false);
        when(xmlDocumentFactory.supports(mockUnsupportedFile)).thenReturn(false);
        when(xmlDocumentFactory.getSupportedExtensions()).thenReturn(new String[]{".xml", ".xsd", ".wsdl"});
    }

    @Test
    void createDocuments_ShouldUseCorrectFactory_ForJavaFile() {
        // Arrange
        Document expectedDocument = new Document("java content");
        when(javaDocumentFactory.createDocuments(mockJavaFile))
            .thenReturn(List.of(expectedDocument));

        // Act
        List<Document> result = documentFactoryManager.createDocuments(mockJavaFile);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(expectedDocument);
        verify(javaDocumentFactory).createDocuments(mockJavaFile);
        verify(textDocumentFactory, never()).createDocuments(any());
        verify(xmlDocumentFactory, never()).createDocuments(any());
    }

    @Test
    void createDocuments_ShouldUseCorrectFactory_ForTextFile() {
        // Arrange
        Document expectedDocument = new Document("text content");
        when(textDocumentFactory.createDocuments(mockTextFile))
            .thenReturn(List.of(expectedDocument));

        // Act
        List<Document> result = documentFactoryManager.createDocuments(mockTextFile);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(expectedDocument);
        verify(textDocumentFactory).createDocuments(mockTextFile);
        verify(javaDocumentFactory, never()).createDocuments(any());
        verify(xmlDocumentFactory, never()).createDocuments(any());
    }

    @Test
    void createDocuments_ShouldReturnEmptyList_ForUnsupportedFile() {
        // Act
        List<Document> result = documentFactoryManager.createDocuments(mockUnsupportedFile);

        // Assert
        assertThat(result).isEmpty();
        verify(javaDocumentFactory, never()).createDocuments(any());
        verify(textDocumentFactory, never()).createDocuments(any());
        verify(xmlDocumentFactory, never()).createDocuments(any());
    }

    @Test
    void createDocuments_ShouldHandleMultipleDocuments() {
        // Arrange
        Document doc1 = new Document("java content 1");
        Document doc2 = new Document("java content 2");
        when(javaDocumentFactory.createDocuments(mockJavaFile))
            .thenReturn(List.of(doc1, doc2));

        // Act
        List<Document> result = documentFactoryManager.createDocuments(mockJavaFile);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(doc1, doc2);
    }

    @Test
    void createDocuments_ShouldHandleFactoryException() {
        // Arrange
        when(javaDocumentFactory.createDocuments(mockJavaFile))
            .thenThrow(new RuntimeException("Factory error"));

        // Act & Assert
        assertThatThrownBy(() -> documentFactoryManager.createDocuments(mockJavaFile))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Factory error");
    }

    @Test
    void isSupported_ShouldReturnTrue_ForSupportedFile() {
        // Act & Assert
        assertThat(documentFactoryManager.isSupported(mockJavaFile)).isTrue();
        assertThat(documentFactoryManager.isSupported(mockTextFile)).isTrue();
    }

    @Test
    void isSupported_ShouldReturnFalse_ForUnsupportedFile() {
        // Act & Assert
        assertThat(documentFactoryManager.isSupported(mockUnsupportedFile)).isFalse();
    }

    @Test
    void isSupported_ShouldCheckAllFactories() {
        // Act
        documentFactoryManager.isSupported(mockJavaFile);

        // Assert
        verify(javaDocumentFactory).supports(mockJavaFile);
        // Should stop at first supporting factory, so others may not be called
    }

    @Test
    void getAllSupportedExtensions_ShouldReturnAllExtensions() {
        // Act
        String[] extensions = documentFactoryManager.getAllSupportedExtensions();

        // Assert
        assertThat(extensions).contains(".java", ".kt", ".scala", ".txt", ".md", ".rst", ".xml", ".xsd", ".wsdl");
        assertThat(extensions).hasSize(9); // No duplicates
    }

    @Test
    void getAllSupportedExtensions_ShouldHandleDuplicates() {
        // Arrange - add factory with duplicate extension
        DocumentFactory duplicateFactory = mock(DocumentFactory.class);
        when(duplicateFactory.getSupportedExtensions()).thenReturn(new String[]{".java", ".groovy"});
        
        List<DocumentFactory> factoriesWithDuplicate = List.of(
            javaDocumentFactory,
            textDocumentFactory,
            xmlDocumentFactory,
            duplicateFactory
        );
        
        DocumentFactoryManager managerWithDuplicate = new DocumentFactoryManager(factoriesWithDuplicate);

        // Act
        String[] extensions = managerWithDuplicate.getAllSupportedExtensions();

        // Assert
        long javaCount = java.util.Arrays.stream(extensions)
            .filter(ext -> ext.equals(".java"))
            .count();
        assertThat(javaCount).isEqualTo(1); // Should only appear once
        assertThat(extensions).contains(".groovy");
    }

    @Test
    void constructor_ShouldHandleEmptyFactoryList() {
        // Arrange
        List<DocumentFactory> emptyFactories = List.of();

        // Act
        DocumentFactoryManager emptyManager = new DocumentFactoryManager(emptyFactories);

        // Assert
        assertThat(emptyManager.isSupported(mockJavaFile)).isFalse();
        assertThat(emptyManager.createDocuments(mockJavaFile)).isEmpty();
        assertThat(emptyManager.getAllSupportedExtensions()).isEmpty();
    }

    @Test
    void constructor_ShouldHandleNullFile() {
        // Act & Assert
        assertThat(documentFactoryManager.isSupported(null)).isFalse();
        assertThat(documentFactoryManager.createDocuments(null)).isEmpty();
    }

    @Test
    void createDocuments_ShouldUseFirstMatchingFactory() {
        // Arrange - both factories support the same file
        when(javaDocumentFactory.supports(mockJavaFile)).thenReturn(true);
        when(textDocumentFactory.supports(mockJavaFile)).thenReturn(true);
        
        Document javaDocument = new Document("java content");
        when(javaDocumentFactory.createDocuments(mockJavaFile))
            .thenReturn(List.of(javaDocument));

        // Act
        List<Document> result = documentFactoryManager.createDocuments(mockJavaFile);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(javaDocument);
        verify(javaDocumentFactory).createDocuments(mockJavaFile);
        verify(textDocumentFactory, never()).createDocuments(mockJavaFile);
    }

    @Test
    void isSupported_ShouldReturnTrueOnFirstMatch() {
        // Arrange - first factory supports the file
        when(javaDocumentFactory.supports(mockJavaFile)).thenReturn(true);

        // Act
        boolean result = documentFactoryManager.isSupported(mockJavaFile);

        // Assert
        assertThat(result).isTrue();
        verify(javaDocumentFactory).supports(mockJavaFile);
        // Other factories may or may not be called since first one matches
    }

    @Test
    void createDocuments_ShouldHandleEmptyDocumentList() {
        // Arrange
        when(javaDocumentFactory.createDocuments(mockJavaFile))
            .thenReturn(List.of());

        // Act
        List<Document> result = documentFactoryManager.createDocuments(mockJavaFile);

        // Assert
        assertThat(result).isEmpty();
        verify(javaDocumentFactory).createDocuments(mockJavaFile);
    }

    @Test
    void getAllSupportedExtensions_ShouldHandleEmptyExtensions() {
        // Arrange
        DocumentFactory emptyExtensionsFactory = mock(DocumentFactory.class);
        when(emptyExtensionsFactory.getSupportedExtensions()).thenReturn(new String[]{});
        
        List<DocumentFactory> factoriesWithEmpty = List.of(
            javaDocumentFactory,
            emptyExtensionsFactory
        );
        
        DocumentFactoryManager managerWithEmpty = new DocumentFactoryManager(factoriesWithEmpty);

        // Act
        String[] extensions = managerWithEmpty.getAllSupportedExtensions();

        // Assert
        assertThat(extensions).contains(".java", ".kt", ".scala");
        // Should not fail even if one factory returns empty array
    }

    @Test
    void factoryOrder_ShouldMatter() {
        // Arrange - create manager with different order
        List<DocumentFactory> reorderedFactories = List.of(
            textDocumentFactory,
            javaDocumentFactory,
            xmlDocumentFactory
        );
        
        DocumentFactoryManager reorderedManager = new DocumentFactoryManager(reorderedFactories);
        
        // Both factories support the file, but text factory comes first
        when(textDocumentFactory.supports(mockJavaFile)).thenReturn(true);
        when(javaDocumentFactory.supports(mockJavaFile)).thenReturn(true);
        
        Document textDocument = new Document("text interpretation of java");
        when(textDocumentFactory.createDocuments(mockJavaFile))
            .thenReturn(List.of(textDocument));

        // Act
        List<Document> result = reorderedManager.createDocuments(mockJavaFile);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(textDocument);
        verify(textDocumentFactory).createDocuments(mockJavaFile);
        verify(javaDocumentFactory, never()).createDocuments(mockJavaFile);
    }
}
