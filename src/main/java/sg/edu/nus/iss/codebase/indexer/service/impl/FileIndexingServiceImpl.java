package sg.edu.nus.iss.codebase.indexer.service.impl;

import org.springframework.ai.document.Document;
import sg.edu.nus.iss.codebase.indexer.config.DynamicVectorStoreFactory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.codebase.indexer.config.IndexingConfiguration;
import sg.edu.nus.iss.codebase.indexer.model.IndexingStatus;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileCacheRepository;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileIndexingService;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.IndexingStatusObserver;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.CreateCollection;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Collections.CollectionInfo;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * Refactored file indexing service using proper design patterns
 * Implements Service Layer Pattern with Observer Pattern for status updates
 */
@Service
public class FileIndexingServiceImpl implements FileIndexingService {
    private final VectorStore vectorStore;
    private final DynamicVectorStoreFactory vectorStoreFactory;
    private final QdrantClient qdrantClient;
    private final Executor virtualThreadExecutor;
    private final IndexingConfiguration config;
    private final FileCacheRepository cacheRepository;
    private final DocumentFactoryManager documentFactoryManager;

    // Observer pattern for status updates
    private final List<IndexingStatusObserver> statusObservers = new CopyOnWriteArrayList<>();

    // Progress tracking
    private final AtomicInteger totalFiles = new AtomicInteger(0);
    private final AtomicInteger indexedFiles = new AtomicInteger(0);
    private final AtomicLong startTime = new AtomicLong();
    private volatile boolean indexingComplete = false;
    private volatile boolean indexingInProgress = false;
    private volatile boolean indexingPaused = false;

    // Enhanced metrics tracking
    private final AtomicInteger activeVirtualThreads = new AtomicInteger(0);
    private final AtomicInteger peakVirtualThreads = new AtomicInteger(0);
    private final AtomicLong totalTasksExecuted = new AtomicLong(0);
    private final AtomicInteger failedFiles = new AtomicInteger(0);
    private final AtomicInteger skippedFiles = new AtomicInteger(0);
    private final Map<String, AtomicInteger> fileTypeStatistics = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> skippedFileExtensions = new ConcurrentHashMap<>();

    // Configurable indexing directory
    private String indexingDirectory = "src";

    @Autowired
    public FileIndexingServiceImpl(
            VectorStore vectorStore,
            DynamicVectorStoreFactory vectorStoreFactory,
            QdrantClient qdrantClient,
            @Qualifier("virtualThreadExecutor") Executor virtualThreadExecutor,
            IndexingConfiguration config,
            FileCacheRepository cacheRepository,
            DocumentFactoryManager documentFactoryManager) {

        this.vectorStore = vectorStore;
        this.vectorStoreFactory = vectorStoreFactory;
        this.qdrantClient = qdrantClient;
        this.virtualThreadExecutor = virtualThreadExecutor;
        this.config = config;
        this.cacheRepository = cacheRepository;
        this.documentFactoryManager = documentFactoryManager;
    }

    @Override
    public CompletableFuture<Void> startIndexing(String directory) {
        if (indexingInProgress) {
            System.out.println("⚠️ Indexing already in progress");
            return CompletableFuture.completedFuture(null);
        }

        setIndexingDirectory(directory);
        indexingInProgress = true;
        indexingPaused = false;
        startTime.set(System.currentTimeMillis());

        // Load cache before starting
        cacheRepository.loadCache();

        // Notify observers that indexing started
        notifyStatusUpdate();
        // Start with priority files first, then continue with remaining files
        return indexPriorityFilesAsync()
                .thenCompose(unused -> indexRemainingFilesAsync())
                .whenComplete((unused, throwable) -> {
                    indexingInProgress = false;
                    indexingComplete = true;
                    if (throwable != null) {
                        notifyIndexingError(new Exception(throwable), "Error during indexing");
                    } else {
                        notifyIndexingComplete();
                    }
                });
    }

    @Override
    public void stopIndexing() {
        indexingInProgress = false;
        indexingPaused = false;
        System.out.println("🛑 Indexing stopped");
        notifyStatusUpdate();
    }

    @Override
    public void pauseIndexing() {
        indexingPaused = true;
        System.out.println("⏸️ Indexing paused");
        notifyStatusUpdate();
    }

    @Override
    public void resumeIndexing() {
        indexingPaused = false;
        System.out.println("▶️ Indexing resumed");
        notifyStatusUpdate();
    }

    @Override
    public IndexingStatus getIndexingStatus() {
        long currentDuration = getIndexingDuration();
        double speed = currentDuration > 0 ? (indexedFiles.get() * 1000.0) / currentDuration : 0.0;

        Map<String, Integer> fileStats = new HashMap<>();
        fileTypeStatistics.forEach((type, count) -> fileStats.put(type, count.get()));

        Map<String, Integer> skippedStats = new HashMap<>();
        skippedFileExtensions.forEach((ext, count) -> skippedStats.put(ext, count.get()));

        return IndexingStatus.builder()
                .totalFiles(totalFiles.get())
                .indexedFiles(indexedFiles.get())
                .failedFiles(failedFiles.get())
                .skippedFiles(skippedFiles.get())
                .indexingInProgress(indexingInProgress && !indexingPaused)
                .indexingComplete(indexingComplete)
                .startTime(startTime.get())
                .currentDuration(currentDuration)
                .indexingSpeed(speed)
                .activeThreads(activeVirtualThreads.get())
                .peakThreads(peakVirtualThreads.get())
                .totalTasksExecuted(totalTasksExecuted.get())
                .fileTypeStatistics(fileStats)
                .skippedFileExtensions(skippedStats)
                .currentDirectory(indexingDirectory)
                .build();
    }

    @Override
    public void setIndexingDirectory(String directory) {
        this.indexingDirectory = directory;
        System.out.println("📁 Indexing directory set to: " + directory);

        if (!indexingInProgress) {
            resetIndexing();
        }
    }

    @Override
    public void resetIndexing() {
        totalFiles.set(0);
        indexedFiles.set(0);
        indexingComplete = false;
        activeVirtualThreads.set(0);
        peakVirtualThreads.set(0);
        totalTasksExecuted.set(0);
        failedFiles.set(0);
        skippedFiles.set(0);
        fileTypeStatistics.clear();
        skippedFileExtensions.clear();

        cacheRepository.clearCache();
        System.out.println("🔄 Indexing statistics reset");
        notifyStatusUpdate();
    }

    @Override
    public void addStatusObserver(IndexingStatusObserver observer) {
        statusObservers.add(observer);
    }

    @Override
    public void removeStatusObserver(IndexingStatusObserver observer) {
        statusObservers.remove(observer);
    }

    /**
     * Index priority files first for immediate search availability
     */
    @Async("indexingExecutor")
    protected CompletableFuture<Void> indexPriorityFilesAsync() {
        try {
            System.out.println("📋 Phase 1: Indexing priority files in background...");

            List<File> priorityFiles = getPriorityFiles();
            List<File> newPriorityFiles = priorityFiles.stream()
                    .filter(cacheRepository::needsReindexing)
                    .toList();

            totalFiles.addAndGet(newPriorityFiles.size());

            if (newPriorityFiles.isEmpty()) {
                System.out.println("✅ All priority files already indexed! Search is available.");
                return CompletableFuture.completedFuture(null);
            }

            // Use virtual threads for parallel processing of priority files
            List<CompletableFuture<Void>> futures = newPriorityFiles.stream()
                    .map(file -> CompletableFuture.runAsync(() -> indexFile(file), virtualThreadExecutor))
                    .toList();

            // Wait for all priority files to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            System.out.println("✅ Priority files indexed! Search is now available.");

        } catch (Exception e) {
            System.err.println("❌ Error indexing priority files: " + e.getMessage());
            notifyIndexingError(e, "Priority files indexing");
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Index remaining files in background
     */
    @Async("virtualThreadExecutor")
    protected CompletableFuture<Void> indexRemainingFilesAsync() {
        try {
            Thread.sleep(1000); // Wait for priority files to complete

            System.out.println("📋 Phase 2: Indexing remaining files in background...");

            List<File> allFiles = getAllCodebaseFiles();
            List<File> remainingFiles = allFiles.stream()
                    .filter(cacheRepository::needsReindexing)
                    .toList();

            totalFiles.addAndGet(remainingFiles.size());

            if (remainingFiles.isEmpty()) {
                System.out.println("✅ All files already indexed! Indexing complete.");
                return CompletableFuture.completedFuture(null);
            }

            System.out.println("🚀 Processing " + remainingFiles.size() + " new/modified files in background");

            // Process files in batches using virtual threads
            int batchSize = config.getProcessing().getBatchSize();
            for (int i = 0; i < remainingFiles.size() && indexingInProgress; i += batchSize) {
                if (indexingPaused) {
                    // Wait while paused
                    while (indexingPaused && indexingInProgress) {
                        Thread.sleep(100);
                    }
                }

                if (!indexingInProgress)
                    break;

                int endIndex = Math.min(i + batchSize, remainingFiles.size());
                List<File> batch = remainingFiles.subList(i, endIndex);

                List<CompletableFuture<Void>> batchFutures = batch.stream()
                        .filter(cacheRepository::needsReindexing)
                        .map(file -> CompletableFuture.runAsync(() -> indexFile(file), virtualThreadExecutor))
                        .toList();

                CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0])).join();

                // Periodically notify observers of progress
                if (i % (batchSize * 5) == 0) {
                    notifyStatusUpdate();
                }
            }

            long duration = (System.currentTimeMillis() - startTime.get()) / 1000;
            System.out.println("🎉 Complete indexing finished! " +
                    indexedFiles.get() + " files indexed in " + duration + "s");

        } catch (Exception e) {
            System.err.println("❌ Error in background indexing: " + e.getMessage());
            notifyIndexingError(e, "Background indexing");
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Index a single file
     */
    private void indexFile(File file) {
        if (!indexingInProgress || indexingPaused) {
            return;
        }

        // Track virtual thread usage
        int currentThreads = activeVirtualThreads.incrementAndGet();
        totalTasksExecuted.incrementAndGet();

        // Update peak thread count
        int peak = peakVirtualThreads.get();
        while (currentThreads > peak && !peakVirtualThreads.compareAndSet(peak, currentThreads)) {
            peak = peakVirtualThreads.get();
        }

        try {
            if (file.length() > config.getProcessing().getMaxFileSize()) {
                skippedFiles.incrementAndGet();
                return;
            } // Track file type statistics
            String fileType = getFileExtension(file);
            AtomicInteger count = fileTypeStatistics.get(fileType);
            if (count == null) {
                count = new AtomicInteger(0);
                fileTypeStatistics.put(fileType, count);
            }
            count.incrementAndGet();

            // Create documents using factory
            List<Document> documents = documentFactoryManager.createDocuments(file);
            if (!documents.isEmpty()) {
                // Use dynamic vector store with the correct collection name
                String collectionName = getCurrentCollectionName();

                // Ensure collection exists before indexing
                ensureCollectionExists(collectionName);

                VectorStore dynamicVectorStore = vectorStoreFactory.createVectorStore(collectionName);

                // Store in vector database with correct collection
                dynamicVectorStore.add(documents);

                indexedFiles.incrementAndGet();
                cacheRepository.saveIndexedFile(file.getAbsolutePath());
            } else {
                skippedFiles.incrementAndGet();
            }

        } catch (Exception e) {
            failedFiles.incrementAndGet();
            System.err.println("❌ Failed to index " + file.getName() + ": " + e.getMessage());
        } finally {
            activeVirtualThreads.decrementAndGet();
        }
    }

    // Helper methods for file processing
    private List<File> getPriorityFiles() {
        List<File> priorityFiles = new ArrayList<>();

        try {
            Path indexPath = Paths.get(indexingDirectory);
            if (Files.exists(indexPath)) {
                try (Stream<Path> paths = Files.walk(indexPath)) {
                    List<File> indexFiles = paths
                            .filter(Files::isRegularFile)
                            .filter(this::isSupportedFile)
                            .map(Path::toFile)
                            .toList();

                    priorityFiles.addAll(indexFiles.stream()
                            .filter(file -> getFilePriority(file) <= 5)
                            .toList());
                }
            }

            // Sort by priority
            priorityFiles.sort((f1, f2) -> {
                int p1 = getFilePriority(f1);
                int p2 = getFilePriority(f2);
                return Integer.compare(p1, p2);
            });

        } catch (Exception e) {
            System.err.println("❌ Error finding priority files: " + e.getMessage());
        }

        return priorityFiles;
    }

    private List<File> getAllCodebaseFiles() {
        List<File> allFiles = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(Paths.get(indexingDirectory), config.getProcessing().getMaxDepth())) {
            List<Path> allPaths = paths
                    .filter(Files::isRegularFile)
                    .filter(this::isNotInExcludedDirectory)
                    .toList();

            // Track skipped file extensions
            for (Path path : allPaths) {
                if (isSupportedFile(path)) {
                    allFiles.add(path.toFile());
                } else {
                    String extension = getFileExtension(path.toFile());
                    if (!extension.isEmpty()) {
                        AtomicInteger count = skippedFileExtensions.get(extension);
                        if (count == null) {
                            count = new AtomicInteger(0);
                            skippedFileExtensions.put(extension, count);
                        }
                        count.incrementAndGet();
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error scanning codebase in " + indexingDirectory + ": " + e.getMessage());
        }

        return allFiles;
    }

    private boolean isSupportedFile(Path path) {
        return documentFactoryManager.isSupported(path.toFile());
    }

    private boolean isNotInExcludedDirectory(Path path) {
        String pathStr = path.toString().toLowerCase();
        return config.getExcludedDirectories().stream()
                .noneMatch(pathStr::contains);
    }

    private int getFilePriority(File file) {
        String fileName = file.getName();

        for (Map.Entry<String, Integer> entry : config.getFilePriorities().entrySet()) {
            if (fileName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return 10; // Default priority
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(lastDot) : "";
    }

    private long getIndexingDuration() {
        if (startTime.get() == 0)
            return 0;
        return System.currentTimeMillis() - startTime.get();
    }

    // Public metrics methods for interface compliance
    @Override
    public long getCurrentIndexingDuration() {
        return getIndexingDuration();
    }

    @Override
    public long getEstimatedTotalDuration() {
        if (totalFiles.get() == 0 || indexedFiles.get() == 0)
            return 0;

        long currentDuration = getIndexingDuration();
        double progress = (double) indexedFiles.get() / totalFiles.get();

        if (progress > 0) {
            return (long) (currentDuration / progress);
        }
        return 0;
    }

    @Override
    public long getTotalIndexingDuration() {
        if (indexingComplete) {
            return getIndexingDuration();
        }
        return getIndexingDuration(); // Return current duration if still in progress
    }

    @Override
    public double getIndexingSpeed() {
        long duration = getIndexingDuration();
        if (duration == 0)
            return 0.0;

        return (indexedFiles.get() * 1000.0) / duration; // files per second
    }

    @Override
    public int getActiveVirtualThreads() {
        return activeVirtualThreads.get();
    }

    @Override
    public int getPeakVirtualThreads() {
        return peakVirtualThreads.get();
    }

    @Override
    public long getTotalTasksExecuted() {
        return totalTasksExecuted.get();
    }

    @Override
    public int getFailedFileCount() {
        return failedFiles.get();
    }

    @Override
    public int getSkippedFileCount() {
        return skippedFiles.get();
    }

    @Override
    public Map<String, Integer> getFileTypeStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        fileTypeStatistics.forEach((type, count) -> stats.put(type, count.get()));
        return stats;
    }

    @Override
    public Map<String, Integer> getSkippedFileExtensions() {
        Map<String, Integer> stats = new HashMap<>();
        skippedFileExtensions.forEach((extension, count) -> stats.put(extension, count.get()));
        return stats;
    }

    // Additional methods needed by HybridSearchService
    @Override
    public boolean isIndexingComplete() {
        return indexingComplete;
    }

    @Override
    public boolean isIndexingInProgress() {
        return indexingInProgress;
    }

    @Override
    public int getIndexedFileCount() {
        return indexedFiles.get();
    }

    @Override
    public int getTotalFileCount() {
        return totalFiles.get();
    }

    @Override
    public double getIndexingProgress() {
        if (totalFiles.get() == 0)
            return 0.0;
        return (double) indexedFiles.get() / totalFiles.get() * 100.0;
    }

    @Override
    public void restartIndexing() {
        try {
            System.out.println("🔄 Restarting indexing process...");

            // Step 1: Delete and recreate the Qdrant collection to remove all old vector
            // data
            deleteAndRecreateCollection();

            // Step 2: Reset state and start indexing again
            indexingComplete = false;
            indexingInProgress = false;
            indexedFiles.set(0);
            totalFiles.set(0);

            // Step 3: Clear statistics
            fileTypeStatistics.clear();
            skippedFileExtensions.clear();
            failedFiles.set(0);
            skippedFiles.set(0);

            // Step 4: Reset cache repository
            if (cacheRepository != null) {
                cacheRepository.clearCache();
            }

            // Step 5: Start indexing in current directory with clean collection
            startIndexing(indexingDirectory);

            System.out.println("✅ Collection cleared and indexing restarted successfully");

        } catch (Exception e) {
            System.err.println("❌ Failed to restart indexing: " + e.getMessage());
            throw new RuntimeException("Failed to restart indexing", e);
        }
    }

    @Override
    public void clearCacheAndReindex() {
        try {
            System.out.println("🗑️ Clearing cache and starting fresh indexing...");

            // Step 1: Delete and recreate the Qdrant collection to remove all old vector
            // data
            deleteAndRecreateCollection();

            // Step 2: Clear all caches and statistics
            fileTypeStatistics.clear();
            skippedFileExtensions.clear();
            failedFiles.set(0);
            skippedFiles.set(0);

            // Step 3: Reset cache repository
            if (cacheRepository != null) {
                cacheRepository.clearCache();
            }

            // Step 4: Restart indexing with clean collection
            restartIndexing();

            System.out.println("✅ Collection cleared, cache cleared, and reindexing started");

        } catch (Exception e) {
            System.err.println("❌ Failed to clear cache and reindex: " + e.getMessage());
            throw new RuntimeException("Failed to clear cache and reindex", e);
        }
    }

    @Override
    public String getCurrentCollectionName() {
        return generateCollectionName(indexingDirectory);
    }

    @Override
    public String getCurrentIndexingDirectory() {
        return indexingDirectory;
    }

    @Override
    public void setCollectionName(String collectionName) {
        // This is a no-op for this implementation since collection name is derived from
        // directory
        // Collection name is automatically generated from the indexing directory
    }

    @Override
    public void setIndexingDirectoryWithCollection(String directory) {
        setIndexingDirectory(directory);
        // Collection name is automatically generated from the directory
        // Start indexing automatically when directory is set
        System.out.println("🚀 Starting automatic indexing for directory: " + directory);
        try {
            startIndexing(directory);
        } catch (Exception e) {
            System.err.println("❌ Failed to start automatic indexing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String generateCollectionName(String directory) {
        if (directory == null) {
            return "codebase-index";
        }

        String normalizedDir = directory.replace("\\", "/");
        String dirName = normalizedDir;

        if (normalizedDir.contains("/")) {
            String[] parts = normalizedDir.split("/");
            dirName = parts[parts.length - 1];
        }

        return "codebase-index-" + dirName.replaceAll("[^a-zA-Z0-9-]", "-");
    }

    // Observer pattern implementation
    private void notifyStatusUpdate() {
        IndexingStatus status = getIndexingStatus();
        statusObservers.forEach(observer -> {
            try {
                observer.onStatusUpdate(status);
            } catch (Exception e) {
                System.err.println("Error notifying status observer: " + e.getMessage());
            }
        });
    }

    private void notifyIndexingComplete() {
        IndexingStatus status = getIndexingStatus();
        statusObservers.forEach(observer -> {
            try {
                observer.onIndexingComplete(status);
            } catch (Exception e) {
                System.err.println("Error notifying completion observer: " + e.getMessage());
            }
        });
    }

    private void notifyIndexingError(Exception error, String context) {
        statusObservers.forEach(observer -> {
            try {
                observer.onIndexingError(error, context);
            } catch (Exception e) {
                System.err.println("Error notifying error observer: " + e.getMessage());
            }
        });
    }

    /**
     * Ensure the collection exists in Qdrant, creating it if necessary
     */
    private void ensureCollectionExists(String collectionName) {
        try {
            if (!checkCollectionExists(collectionName)) {
                createCollection(collectionName);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Warning: Error ensuring collection exists: " + e.getMessage());
        }
    }

    /**
     * Check if a collection exists in Qdrant with correct dimensions
     */
    private boolean checkCollectionExists(String targetCollectionName) {
        try {
            CollectionInfo info = qdrantClient.getCollectionInfoAsync(targetCollectionName).get();
            if (info != null) {
                // Check if the collection has the right vector dimensions
                // nomic-embed-text produces 768-dimensional embeddings
                var vectorConfig = info.getConfig().getParams().getVectorsConfig();
                if (vectorConfig.hasParams()) {
                    long dimensions = vectorConfig.getParams().getSize();
                    if (dimensions != 768) {
                        System.out.println("⚠️ Collection " + targetCollectionName + " has wrong dimensions: "
                                + dimensions + " (expected 768)");
                        System.out.println("🗑️ Deleting and recreating collection with correct dimensions...");
                        deleteCollection(targetCollectionName);
                        return false; // Will trigger recreation
                    }
                }
                return true;
            }
            return false;
        } catch (java.util.concurrent.ExecutionException e) {
            // Check if the error message indicates collection doesn't exist
            String errorMessage = e.getMessage();
            if (errorMessage != null &&
                    (errorMessage.contains("NOT_FOUND") || errorMessage.contains("doesn't exist"))) {
                // Collection doesn't exist, this is expected
                return false;
            }
            // Log other types of errors (suppress expected ones)
            return false;
        } catch (Exception e) {
            // Check for collection not found in any exception
            String errorMessage = e.getMessage();
            if (errorMessage != null &&
                    (errorMessage.contains("NOT_FOUND") || errorMessage.contains("doesn't exist"))) {
                return false;
            }
            // Log unexpected exceptions (suppress expected ones)
            return false;
        }
    }

    /**
     * Delete a collection from Qdrant
     */
    private void deleteCollection(String targetCollectionName) {
        try {
            qdrantClient.deleteCollectionAsync(targetCollectionName).get();
            System.out.println("🗑️ Deleted collection: " + targetCollectionName);
        } catch (Exception e) {
            // Suppress expected collection not found errors
            if (e.getMessage() == null ||
                    !(e.getMessage().contains("NOT_FOUND") || e.getMessage().contains("doesn't exist"))) {
                System.err.println(
                        "⚠️ Warning: Error deleting collection " + targetCollectionName + ": " + e.getMessage());
            }
        }
    }

    /**
     * Create a new collection in Qdrant with correct vector dimensions
     */
    private void createCollection(String targetCollectionName) throws Exception {
        // Create vector parameters for embeddings
        // nomic-embed-text produces 768-dimensional embeddings
        VectorParams vectorParams = VectorParams.newBuilder()
                .setSize(768) // nomic-embed-text embedding dimension
                .setDistance(Distance.Cosine)
                .build();

        // Create collection request
        CreateCollection createCollection = CreateCollection.newBuilder()
                .setCollectionName(targetCollectionName)
                .setVectorsConfig(
                        io.qdrant.client.grpc.Collections.VectorsConfig.newBuilder()
                                .setParams(vectorParams)
                                .build())
                .build(); // Execute collection creation
        qdrantClient.createCollectionAsync(createCollection).get();
        System.out.println("✅ Created collection: " + targetCollectionName);
    }

    /**
     * Delete and recreate the Qdrant collection to ensure clean vector data
     */
    private void deleteAndRecreateCollection() {
        // Temporarily suppress logging
        suppressLogging();

        try {
            String collectionName = getCurrentCollectionName();
            System.out.println("🗑️ Deleting Qdrant collection: " + collectionName);

            // Step 1: Try to delete the existing collection
            try {
                qdrantClient.deleteCollectionAsync(collectionName).get();
                System.out.println("✅ Collection deleted: " + collectionName);
            } catch (Exception deleteError) {
                // Collection might not exist - that's fine
                String errorMessage = deleteError.getMessage();
                if (errorMessage != null &&
                        (errorMessage.contains("NOT_FOUND") || errorMessage.contains("doesn't exist"))) {
                    System.out.println("ℹ️ Collection didn't exist: " + collectionName);
                } else {
                    System.err.println("⚠️ Warning deleting collection: " + deleteError.getMessage());
                }
            }

            // Step 2: Wait a moment for deletion to complete
            Thread.sleep(1000);

            // Step 3: Create the collection fresh with correct dimensions
            System.out.println("🆕 Creating fresh Qdrant collection: " + collectionName);
            createCollection(collectionName);
            System.out.println("✅ Fresh collection created: " + collectionName);

        } catch (Exception e) {
            System.err.println("❌ Error during collection deletion/recreation: " + e.getMessage());
            throw new RuntimeException("Failed to delete and recreate collection", e);
        } finally {
            restoreLogging();
        }
    }

    /**
     * Suppress gRPC and Qdrant logging during collection operations
     */
    private void suppressLogging() {
        try {
            ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
                    .getLogger("io.grpc");
            logger.setLevel(ch.qos.logback.classic.Level.OFF);

            logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("io.qdrant");
            logger.setLevel(ch.qos.logback.classic.Level.OFF);

            logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("io.netty");
            logger.setLevel(ch.qos.logback.classic.Level.OFF);
        } catch (Exception e) {
            // Ignore logging configuration errors
        }
    }

    /**
     * Restore normal logging levels
     */
    private void restoreLogging() {
        try {
            ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
                    .getLogger("io.grpc");
            logger.setLevel(ch.qos.logback.classic.Level.WARN);

            logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("io.qdrant");
            logger.setLevel(ch.qos.logback.classic.Level.WARN);

            logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("io.netty");
            logger.setLevel(ch.qos.logback.classic.Level.WARN);
        } catch (Exception e) {
            // Ignore logging configuration errors
        }
    }
}
