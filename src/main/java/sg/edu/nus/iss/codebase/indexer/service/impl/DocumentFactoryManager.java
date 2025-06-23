package sg.edu.nus.iss.codebase.indexer.service.impl;

import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.DocumentFactory;

import java.io.File;
import java.util.List;

/**
 * Manager for document factories
 * Implements Factory Pattern with multiple concrete factories
 */
@Service
public class DocumentFactoryManager {

    private final List<DocumentFactory> factories;

    @Autowired
    public DocumentFactoryManager(List<DocumentFactory> factories) {
        this.factories = factories;
    }

    /**
     * Create documents using the appropriate factory
     */
    public List<Document> createDocuments(File file) {
        for (DocumentFactory factory : factories) {
            if (factory.supports(file)) {
                return factory.createDocuments(file);
            }
        }
        return List.of(); // No factory supports this file type
    }

    /**
     * Check if any factory supports the file
     */
    public boolean isSupported(File file) {
        return factories.stream().anyMatch(factory -> factory.supports(file));
    }

    /**
     * Get all supported extensions across all factories
     */
    public String[] getAllSupportedExtensions() {
        return factories.stream()
            .flatMap(factory -> java.util.Arrays.stream(factory.getSupportedExtensions()))
            .distinct()
            .toArray(String[]::new);
    }
}
