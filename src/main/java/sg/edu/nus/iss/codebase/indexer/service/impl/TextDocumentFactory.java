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
            ".kt", ".scala", ".sql", ".html", ".css", ".js", ".ts", ".php", ".py", ".c",
            ".cpp", ".cs", ".rb", ".go", ".swift", ".lua", ".pl", ".r", ".sh", ".cmd", ".ps1");

    @Autowired
    public TextDocumentFactory(IndexingConfiguration config) {
        this.config = config;
    }

    @Override
    public List<Document> createDocuments(File file) {
        try {
            // Read raw text content from file
            String rawContent = Files.readString(file.toPath());

            // Sanitize content to remove problematic characters for embedding models
            String content = sanitizeContent(rawContent);
            // Skip files that are too short after sanitization
            if (content.trim().length() < 20) {
                return List.of(); // Return empty list without message
            }

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
                    // CRITICAL FIX: Ensure text field is explicitly set in metadata
                    // This addresses issue where content was in doc_content but not in text field
                    chunkMetadata.put("text", chunks.get(i));
                    chunkMetadata.put("doc_content", chunks.get(i)); // Keep as backup
                    documents.add(new Document(chunks.get(i), chunkMetadata));
                }
            } else {
                // CRITICAL FIX: Ensure text field is explicitly set in metadata
                // This addresses issue where content was in doc_content but not in text field  
                metadata.put("text", content);
                metadata.put("doc_content", content); // Keep as backup
                documents.add(new Document(content, metadata));
            }

            return documents;

        } catch (Exception e) {
            System.err.println("❌ Error creating document for " + file.getName() + ": " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Sanitize content to make it safe for embedding models
     */
    private String sanitizeContent(String content) {
        if (content == null)
            return "";

        // Remove or replace problematic characters that can cause encoding issues
        String sanitized = content
                // Remove null bytes and other control characters except tabs, newlines, and
                // carriage returns
                .replaceAll("[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F-\u009F]", "")
                // Remove unicode special characters that cause encoding issues
                .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")
                // Remove specific problematic unicode ranges
                .replaceAll("[\uFEFF\uFFFE\uFFFF]", "") // BOM and other special chars
                // Remove unusual unicode characters that often cause encoding issues
                .replaceAll("[\u200B-\u200F\u2028-\u202F\u205F-\u206F]", "") // Zero-width and other space chars
                // Remove special tokens that might confuse embedding models
                .replaceAll("<\\|[^>]*\\|>", "") // Remove special tokens like <|special|>
                .replaceAll("\\[\\[TOKEN[^\\]]*\\]\\]", "") // Remove [[TOKEN...]] patterns
                .replaceAll("<\\/?\\w+[^>]*>", " ") // Remove HTML/XML tags
                // Replace multiple consecutive whitespace with single space (but preserve
                // newlines)
                .replaceAll("[ \t]+", " ")
                // Remove excessive newlines (more than 3 consecutive)
                .replaceAll("\n{4,}", "\n\n\n")
                // Remove very long sequences of the same character (potential binary data)
                .replaceAll("(.)\\1{50,}", "$1$1$1...")
                // Remove lines that are just symbols or special characters
                .lines()
                .filter(line -> {
                    String trimmed = line.trim();
                    // Keep lines that have at least some alphanumeric content or are comments
                    return trimmed.isEmpty() ||
                            trimmed.matches(".*[a-zA-Z0-9].*") ||
                            trimmed.startsWith("//") || trimmed.startsWith("#") ||
                            trimmed.startsWith("*") || trimmed.startsWith("<!--");
                })
                .map(line -> {
                    // Further sanitize each line
                    String cleaned = line.trim();
                    // Remove lines with too many non-ASCII characters (potential encoding issues)
                    long nonAsciiCount = cleaned.chars().filter(c -> c > 127).count();
                    if (nonAsciiCount > cleaned.length() * 0.3) { // More than 30% non-ASCII
                        return ""; // Skip this line
                    }
                    return cleaned;
                })
                .filter(line -> !line.isEmpty())
                .collect(java.util.stream.Collectors.joining("\n"))
                .trim();

        // Ensure content is not too long for embedding model
        if (sanitized.length() > 35000) { // Further reduced from 40000
            sanitized = sanitized.substring(0, 35000) + "\n... [content truncated for embedding]";
        }

        // Additional check for minimum meaningful content
        if (sanitized.length() < 25) { // Increased from 20
            return "";
        }

        // Final check: remove any remaining problematic patterns
        sanitized = sanitized
                // Remove sequences that look like binary data
                .replaceAll("[\\x00-\\x1F]{5,}", " ")
                // Remove base64-like long strings that might cause issues
                .replaceAll("\\b[A-Za-z0-9+/]{100,}=*\\b", "[base64-data]");

        return sanitized;
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
        metadata.put("lastModifiedDate", new java.util.Date(file.lastModified()).toString());
        metadata.put("indexedAt", new java.util.Date().toString());
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

        // Sort entries by key length in descending order to prioritize more specific patterns
        List<Map.Entry<String, Integer>> sortedPriorities = config.getFilePriorities().entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getKey().length(), e1.getKey().length()))
                .collect(java.util.stream.Collectors.toList());

        for (Map.Entry<String, Integer> entry : sortedPriorities) {
            if (fileName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // Default priority
        return 10;
    }
}
