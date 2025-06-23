package sg.edu.nus.iss.codebase.indexer.service.impl;

import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sg.edu.nus.iss.codebase.indexer.config.IndexingConfiguration;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.DocumentFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

/**
 * Factory for creating documents from text-based files
 * Implements Factory Pattern for text file processing
 */
@Component
public class TextDocumentFactory implements DocumentFactory {

    private final IndexingConfiguration config;
    private static final Set<String> SUPPORTED_TEXT_EXTENSIONS = Set.of(
        ".java", ".xml", ".properties", ".yml", ".yaml", ".json", ".md", ".txt",
        ".kt", ".scala", ".sql", ".html", ".css", ".js", ".php", ".py", ".c", 
        ".cpp", ".cs", ".rb", ".go", ".swift", ".lua", ".pl", ".r", ".sh", ".cmd"
    );

    @Autowired
    public TextDocumentFactory(IndexingConfiguration config) {
        this.config = config;
    }

    @Override
    public List<Document> createDocuments(File file) {
        try {
            // Read raw text content from file
            String content = Files.readString(file.toPath());
            
            // Create metadata for the document
            Map<String, Object> metadata = createMetadata(file);
            
            // Split large files into manageable chunks
            List<Document> documents = new ArrayList<>();
            if (content.length() > config.getProcessing().getChunkSize()) {
                List<String> chunks = splitIntoChunks(content, 
                    config.getProcessing().getChunkSize(), 
                    config.getProcessing().getChunkOverlap());
                    
                for (int i = 0; i < chunks.size(); i++) {
                    Map<String, Object> chunkMetadata = new HashMap<>(metadata);
                    chunkMetadata.put("chunk", String.valueOf(i + 1));
                    chunkMetadata.put("total_chunks", String.valueOf(chunks.size()));
                    documents.add(new Document(chunks.get(i), chunkMetadata));
                }
            } else {
                documents.add(new Document(content, metadata));
            }
            
            return documents;
            
        } catch (Exception e) {
            System.err.println("❌ Error creating document for " + file.getName() + ": " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public boolean supports(File file) {
        String fileName = file.getName().toLowerCase();
        return SUPPORTED_TEXT_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    @Override
    public String[] getSupportedExtensions() {
        return SUPPORTED_TEXT_EXTENSIONS.toArray(new String[0]);
    }

    /**
     * Create metadata for the document
     */
    private Map<String, Object> createMetadata(File file) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("filename", file.getName());
        metadata.put("filepath", file.getAbsolutePath());
        metadata.put("filetype", getFileExtension(file));
        metadata.put("priority", String.valueOf(getFilePriority(file)));
        metadata.put("size", String.valueOf(file.length()));
        metadata.put("lastModified", String.valueOf(file.lastModified()));
        return metadata;
    }

    /**
     * Split content into chunks with overlap
     */
    private List<String> splitIntoChunks(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            
            // Try to break at natural boundaries (lines, sentences)
            if (end < text.length()) {
                int lastNewline = text.lastIndexOf('\n', end);
                if (lastNewline > start + chunkSize / 2) {
                    end = lastNewline;
                }
            }
            
            chunks.add(text.substring(start, end));
            start = Math.max(start + chunkSize - overlap, end);
        }
        
        return chunks;
    }

    /**
     * Get file extension
     */
    private String getFileExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(lastDot) : "";
    }

    /**
     * Calculate file priority based on configuration
     */
    private int getFilePriority(File file) {
        String fileName = file.getName();
        
        // Check for specific patterns first
        for (Map.Entry<String, Integer> entry : config.getFilePriorities().entrySet()) {
            if (fileName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        // Default priority
        return 10;
    }
}
