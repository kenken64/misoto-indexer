package sg.edu.nus.iss.codebase.indexer.service.interfaces;

import sg.edu.nus.iss.codebase.indexer.model.IndexingStatus;

/**
 * Observer interface for indexing status updates
 * Implements Observer Pattern
 */
public interface IndexingStatusObserver {
    
    /**
     * Called when indexing status is updated
     * @param status Current indexing status
     */
    void onStatusUpdate(IndexingStatus status);
    
    /**
     * Called when indexing is completed
     * @param finalStatus Final indexing status
     */
    void onIndexingComplete(IndexingStatus finalStatus);
    
    /**
     * Called when an error occurs during indexing
     * @param error The error that occurred
     * @param context Additional context about the error
     */
    void onIndexingError(Exception error, String context);
}
