package sg.edu.nus.iss.codebase.indexer.model;

import java.util.Map;

/**
 * Model representing indexing status and metrics
 * Implements Data Transfer Object pattern
 */
public class IndexingStatus {
    
    private final int totalFiles;
    private final int indexedFiles;
    private final int failedFiles;
    private final int skippedFiles;
    private final boolean indexingInProgress;
    private final boolean indexingComplete;
    private final long startTime;
    private final long currentDuration;
    private final double indexingSpeed;
    private final int activeThreads;
    private final int peakThreads;
    private final long totalTasksExecuted;
    private final Map<String, Integer> fileTypeStatistics;
    private final Map<String, Integer> skippedFileExtensions;
    private final String currentDirectory;

    private IndexingStatus(Builder builder) {
        this.totalFiles = builder.totalFiles;
        this.indexedFiles = builder.indexedFiles;
        this.failedFiles = builder.failedFiles;
        this.skippedFiles = builder.skippedFiles;
        this.indexingInProgress = builder.indexingInProgress;
        this.indexingComplete = builder.indexingComplete;
        this.startTime = builder.startTime;
        this.currentDuration = builder.currentDuration;
        this.indexingSpeed = builder.indexingSpeed;
        this.activeThreads = builder.activeThreads;
        this.peakThreads = builder.peakThreads;
        this.totalTasksExecuted = builder.totalTasksExecuted;
        this.fileTypeStatistics = Map.copyOf(builder.fileTypeStatistics);
        this.skippedFileExtensions = Map.copyOf(builder.skippedFileExtensions);
        this.currentDirectory = builder.currentDirectory;
    }

    // Getters
    public int getTotalFiles() { return totalFiles; }
    public int getIndexedFiles() { return indexedFiles; }
    public int getFailedFiles() { return failedFiles; }
    public int getSkippedFiles() { return skippedFiles; }
    public boolean isIndexingInProgress() { return indexingInProgress; }
    public boolean isIndexingComplete() { return indexingComplete; }
    public long getStartTime() { return startTime; }
    public long getCurrentDuration() { return currentDuration; }
    public double getIndexingSpeed() { return indexingSpeed; }
    public int getActiveThreads() { return activeThreads; }
    public int getPeakThreads() { return peakThreads; }
    public long getTotalTasksExecuted() { return totalTasksExecuted; }
    public Map<String, Integer> getFileTypeStatistics() { return fileTypeStatistics; }
    public Map<String, Integer> getSkippedFileExtensions() { return skippedFileExtensions; }
    public String getCurrentDirectory() { return currentDirectory; }

    /**
     * Calculate indexing progress percentage
     */
    public double getProgress() {
        return totalFiles > 0 ? (indexedFiles * 100.0 / totalFiles) : 0.0;
    }

    /**
     * Estimate remaining time in milliseconds
     */
    public long getEstimatedRemainingTime() {
        if (indexedFiles == 0 || indexingSpeed == 0) return 0;
        int remainingFiles = totalFiles - indexedFiles;
        return (long) (remainingFiles / indexingSpeed * 1000);
    }

    /**
     * Builder pattern implementation for IndexingStatus
     */
    public static class Builder {
        private int totalFiles;
        private int indexedFiles;
        private int failedFiles;
        private int skippedFiles;
        private boolean indexingInProgress;
        private boolean indexingComplete;
        private long startTime;
        private long currentDuration;
        private double indexingSpeed;
        private int activeThreads;
        private int peakThreads;
        private long totalTasksExecuted;
        private Map<String, Integer> fileTypeStatistics = Map.of();
        private Map<String, Integer> skippedFileExtensions = Map.of();
        private String currentDirectory;

        public Builder totalFiles(int totalFiles) {
            this.totalFiles = totalFiles;
            return this;
        }

        public Builder indexedFiles(int indexedFiles) {
            this.indexedFiles = indexedFiles;
            return this;
        }

        public Builder failedFiles(int failedFiles) {
            this.failedFiles = failedFiles;
            return this;
        }

        public Builder skippedFiles(int skippedFiles) {
            this.skippedFiles = skippedFiles;
            return this;
        }

        public Builder indexingInProgress(boolean indexingInProgress) {
            this.indexingInProgress = indexingInProgress;
            return this;
        }

        public Builder indexingComplete(boolean indexingComplete) {
            this.indexingComplete = indexingComplete;
            return this;
        }

        public Builder startTime(long startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder currentDuration(long currentDuration) {
            this.currentDuration = currentDuration;
            return this;
        }

        public Builder indexingSpeed(double indexingSpeed) {
            this.indexingSpeed = indexingSpeed;
            return this;
        }

        public Builder activeThreads(int activeThreads) {
            this.activeThreads = activeThreads;
            return this;
        }

        public Builder peakThreads(int peakThreads) {
            this.peakThreads = peakThreads;
            return this;
        }

        public Builder totalTasksExecuted(long totalTasksExecuted) {
            this.totalTasksExecuted = totalTasksExecuted;
            return this;
        }

        public Builder fileTypeStatistics(Map<String, Integer> fileTypeStatistics) {
            this.fileTypeStatistics = fileTypeStatistics;
            return this;
        }

        public Builder skippedFileExtensions(Map<String, Integer> skippedFileExtensions) {
            this.skippedFileExtensions = skippedFileExtensions;
            return this;
        }

        public Builder currentDirectory(String currentDirectory) {
            this.currentDirectory = currentDirectory;
            return this;
        }

        public IndexingStatus build() {
            return new IndexingStatus(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
