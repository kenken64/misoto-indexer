package sg.edu.nus.iss.codebase.indexer.service.interfaces;

import java.io.File;
import java.util.Set;

/**
 * Repository interface for file cache management
 * Implements Repository Pattern
 */
public interface FileCacheRepository {
    
    /**
     * Check if a file needs reindexing
     * @param file The file to check
     * @return true if the file needs reindexing
     */
    boolean needsReindexing(File file);
    
    /**
     * Save a file as indexed with its modification time
     * @param filePath The path of the indexed file
     */
    void saveIndexedFile(String filePath);
    
    /**
     * Get all indexed file paths
     * @return Set of indexed file paths
     */
    Set<String> getIndexedFilePaths();
    
    /**
     * Load the cache from persistent storage
     */
    void loadCache();
    
    /**
     * Clear the entire cache
     */
    void clearCache();
    
    /**
     * Remove deleted files from cache
     * @param deletedFiles List of deleted file paths
     */
    void removeDeletedFiles(java.util.List<String> deletedFiles);
    
    /**
     * Get the number of cached files
     * @return Number of files in cache
     */
    int getCacheSize();
}
