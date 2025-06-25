package sg.edu.nus.iss.codebase.indexer.service.interfaces;

import sg.edu.nus.iss.codebase.indexer.model.IndexingStatus;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for file indexing operations
 * Implements Service Layer Pattern
 */
public interface FileIndexingService {
    
    /**
     * Start the indexing process for a directory
     * @param directory The directory to index
     * @return CompletableFuture that completes when indexing is done
     */
    CompletableFuture<Void> startIndexing(String directory);
    
    /**
     * Stop the current indexing process
     */
    void stopIndexing();
    
    /**
     * Pause the indexing process
     */
    void pauseIndexing();
    
    /**
     * Resume the indexing process
     */
    void resumeIndexing();
    
    /**
     * Get the current indexing status
     * @return Current indexing status
     */
    IndexingStatus getIndexingStatus();
      /**
     * Set the indexing directory
     * @param directory The directory to index
     */
    void setIndexingDirectory(String directory);
    
    /**
     * Set the indexing directory and collection name based on the directory
     * @param directory The directory to index
     */
    void setIndexingDirectoryWithCollection(String directory);
    
    /**
     * Set the Qdrant collection name
     * @param collectionName The collection name to use
     */
    void setCollectionName(String collectionName);
    
    /**
     * Reset indexing statistics and cache
     */
    void resetIndexing();
    
    /**
     * Register an observer for indexing status updates
     * @param observer The observer to register
     */
    void addStatusObserver(IndexingStatusObserver observer);
    
    /**
     * Unregister an observer
     * @param observer The observer to unregister
     */
    void removeStatusObserver(IndexingStatusObserver observer);
      // Additional metrics methods for detailed status display
    long getCurrentIndexingDuration();
    long getEstimatedTotalDuration();
    long getTotalIndexingDuration();
    double getIndexingSpeed();
    int getActiveVirtualThreads();
    int getPeakVirtualThreads();
    long getTotalTasksExecuted();
    int getFailedFileCount();
    int getSkippedFileCount();
    java.util.Map<String, Integer> getFileTypeStatistics();
    java.util.Map<String, Integer> getSkippedFileExtensions();
    
    // Additional methods needed by HybridSearchService
    boolean isIndexingComplete();
    boolean isIndexingInProgress();
    int getIndexedFileCount();
    int getTotalFileCount();
    double getIndexingProgress();
    void restartIndexing();    void clearCacheAndReindex();
    
    /**
     * Get the current collection name
     * @return The current collection name
     */
    String getCurrentCollectionName();
    
    /**
     * Get the current indexing directory
     * @return The current indexing directory
     */
    String getCurrentIndexingDirectory();
}
