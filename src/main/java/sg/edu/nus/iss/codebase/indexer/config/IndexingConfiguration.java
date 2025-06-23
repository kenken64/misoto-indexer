package sg.edu.nus.iss.codebase.indexer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Configuration class for indexing settings
 * Implements Configuration Pattern to externalize settings
 */
@Component
@ConfigurationProperties(prefix = "indexer")
public class IndexingConfiguration {

    /**
     * Supported file extensions for indexing
     */
    private Set<String> supportedExtensions = Set.of(
        // Java ecosystem
        ".java", ".xml", ".properties", ".yml", ".yaml", ".json", ".md", ".txt",
        
        // Template and documentation
        ".st", ".adoc",
        
        // JVM languages
        ".kt", ".scala",
        
        // Database
        ".sql", ".cql",
        
        // Web technologies
        ".html", ".css", ".js", ".jsp", ".asp", ".aspx", ".php",
        
        // System and configuration
        ".conf", ".cmd", ".sh",
        
        // Programming languages
        ".py", ".c", ".cpp", ".cs", ".rb", ".vb", ".go", ".swift", 
        ".lua", ".pl", ".r",
        
        // Document formats
        ".pdf"
    );

    /**
     * File priority mappings for priority-based indexing
     */
    private Map<String, Integer> filePriorities = Map.of(
        "Controller.java", 1,    // REST controllers (highest priority)
        "Service.java", 2,       // Business logic services
        "Repository.java", 3,    // Data access
        "Config.java", 4,        // Configuration classes
        "Application.java", 5,   // Main application classes
        ".java", 6,              // Other Java files
        ".xml", 7,               // Configuration files
        ".properties", 8,        // Properties files
        ".md", 9                 // Documentation
    );

    /**
     * Directories to exclude from indexing
     */
    private Set<String> excludedDirectories = Set.of(
        "target", ".git", "node_modules", ".idea", ".vscode", "build", "dist"
    );

    /**
     * Processing configuration
     */
    private ProcessingConfig processing = new ProcessingConfig();

    /**
     * Cache configuration
     */
    private CacheConfig cache = new CacheConfig();

    // Getters and setters
    public Set<String> getSupportedExtensions() {
        return supportedExtensions;
    }

    public void setSupportedExtensions(Set<String> supportedExtensions) {
        this.supportedExtensions = supportedExtensions;
    }

    public Map<String, Integer> getFilePriorities() {
        return filePriorities;
    }

    public void setFilePriorities(Map<String, Integer> filePriorities) {
        this.filePriorities = filePriorities;
    }

    public Set<String> getExcludedDirectories() {
        return excludedDirectories;
    }

    public void setExcludedDirectories(Set<String> excludedDirectories) {
        this.excludedDirectories = excludedDirectories;
    }

    public ProcessingConfig getProcessing() {
        return processing;
    }

    public void setProcessing(ProcessingConfig processing) {
        this.processing = processing;
    }

    public CacheConfig getCache() {
        return cache;
    }

    public void setCache(CacheConfig cache) {
        this.cache = cache;
    }

    /**
     * Processing configuration nested class
     */
    public static class ProcessingConfig {
        private int batchSize = 20;
        private int maxFileSize = 1024 * 1024; // 1MB
        private int chunkSize = 3000;
        private int chunkOverlap = 500;
        private int maxDepth = 10;

        // Getters and setters
        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public int getMaxFileSize() {
            return maxFileSize;
        }

        public void setMaxFileSize(int maxFileSize) {
            this.maxFileSize = maxFileSize;
        }

        public int getChunkSize() {
            return chunkSize;
        }

        public void setChunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
        }

        public int getChunkOverlap() {
            return chunkOverlap;
        }

        public void setChunkOverlap(int chunkOverlap) {
            this.chunkOverlap = chunkOverlap;
        }

        public int getMaxDepth() {
            return maxDepth;
        }

        public void setMaxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
        }
    }

    /**
     * Cache configuration nested class
     */
    public static class CacheConfig {
        private String cacheFileName = ".indexed_files_cache.txt";
        private boolean enabled = true;
        private long maxCacheAge = 7 * 24 * 60 * 60 * 1000L; // 7 days

        // Getters and setters
        public String getCacheFileName() {
            return cacheFileName;
        }

        public void setCacheFileName(String cacheFileName) {
            this.cacheFileName = cacheFileName;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getMaxCacheAge() {
            return maxCacheAge;
        }

        public void setMaxCacheAge(long maxCacheAge) {
            this.maxCacheAge = maxCacheAge;
        }
    }
}
