package sg.edu.nus.iss.codebase.indexer.service.interfaces;

import org.springframework.ai.document.Document;
import java.io.File;
import java.util.List;

/**
 * Factory interface for creating documents from files
 * Implements Factory Pattern
 */
public interface DocumentFactory {

    /**
     * Create documents from a file
     * 
     * @param file The file to process
     * @return List of documents created from the file
     */
    List<Document> createDocuments(File file);

    /**
     * Check if this factory supports the given file
     * 
     * @param file The file to check
     * @return true if this factory can process the file
     */
    boolean supports(File file);

    /**
     * Get the file types supported by this factory
     * 
     * @return Array of supported file extensions
     */
    String[] getSupportedExtensions();
}
