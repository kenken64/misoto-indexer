package sg.edu.nus.iss.codebase.indexer.service.impl;

import org.springframework.ai.document.Document;
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
public class FileIndexingServiceImpl implements FileIndexingService {    private final VectorStore vectorStore;
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
    private String indexingDirectory = "src";    @Autowired
    public FileIndexingServiceImpl(
            VectorStore vectorStore,
            @Qualifier("virtualThreadExecutor") Executor virtualThreadExecutor,
            IndexingConfiguration config,
            FileCacheRepository cacheRepository,
            DocumentFactoryManager documentFactoryManager) {
        
        this.vectorStore = vectorStore;
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
        long currentDuration = getCurrentIndexingDuration();
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
                
                if (!indexingInProgress) break;
                
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
            }            // Track file type statistics
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
                // Store in vector database
                vectorStore.add(documents);
                
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
                    allFiles.add(path.toFile());                } else {
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

    private long getCurrentIndexingDuration() {
        if (startTime.get() == 0) return 0;
        return System.currentTimeMillis() - startTime.get();
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
}
