package sg.edu.nus.iss.codebase.indexer.service;

/**
 * IndexingService - Implements Hybrid Indexing with Vector Embeddings
 * 
 * EMBEDDING PIPELINE FLOW:
 * ========================
 * 
 * 📄 Raw Text (from source files)
 *     ↓
 * 🤖 nomic-embed-text (Ollama embedding model - 768 dimensions)  
 *     ↓
 * 📊 Vector Representation (768-dimensional float array)
 *     ↓
 * ☁️ Qdrant Cloud (vector database storage with metadata)
 * 
 * IMPLEMENTATION DETAILS:
 * =====================
 * 1. Raw Text Extraction: createDocumentsFromFile() reads file content
 * 2. Embedding Generation: vectorStore.add() → nomic-embed-text → vectors
 * 3. Vector Storage: Qdrant stores vectors with cosine similarity indexing
 * 4. Search: Query text → embedding → similarity search in Qdrant
 * 
 * HYBRID APPROACH:
 * ===============
 * - Priority indexing: Critical files (Controllers, Services) indexed first
 * - Background indexing: Remaining files processed asynchronously  
 * - Fallback search: File-based search when vector search unavailable
 * - Progressive results: Search available during indexing process
 */

import org.springframework.ai.document.Document;
import sg.edu.nus.iss.codebase.indexer.config.DynamicVectorStoreFactory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.CreateCollection;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Collections.CollectionInfo;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Service
@EnableAsync
public class IndexingService {
    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private QdrantClient qdrantClient;

    @Autowired
    private DynamicVectorStoreFactory vectorStoreFactory;

    @Autowired
    @Qualifier("indexingExecutor")
    private Executor indexingExecutor;
    @Autowired
    @Qualifier("virtualThreadExecutor")
    private Executor virtualThreadExecutor; // Collection name will be set dynamically based on directory being indexed
    private String collectionName = "codebase-index";

    // Progress tracking
    private final AtomicInteger totalFiles = new AtomicInteger(0);
    private final AtomicInteger indexedFiles = new AtomicInteger(0);
    private final AtomicLong startTime = new AtomicLong();
    private final Set<String> indexedFilePaths = ConcurrentHashMap.newKeySet();
    private volatile boolean indexingComplete = false;
    private volatile boolean indexingInProgress = false;

    // Enhanced metrics tracking
    private final AtomicInteger activeVirtualThreads = new AtomicInteger(0);
    private final AtomicInteger peakVirtualThreads = new AtomicInteger(0);
    private final AtomicLong totalTasksExecuted = new AtomicLong(0);
    private final AtomicInteger failedFiles = new AtomicInteger(0);
    private final AtomicInteger skippedFiles = new AtomicInteger(0);
    private final Map<String, AtomicInteger> fileTypeStatistics = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> skippedFileExtensions = new ConcurrentHashMap<>();

    // Configurable indexing directory
    private String indexingDirectory = "src"; // default to src

    // File priority mappings
    private static final Map<String, Integer> FILE_PRIORITIES = Map.of(
            "Controller.java", 1, // REST controllers (highest priority)
            "Service.java", 2, // Business logic services
            "Repository.java", 3, // Data access
            "Config.java", 4, // Configuration classes
            "Application.java", 5, // Main application classes
            ".java", 6, // Other Java files
            ".xml", 7, // Configuration files
            ".properties", 8, // Properties files
            ".md", 9 // Documentation
    );
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            // Java ecosystem
            ".java", ".xml", ".properties", ".yml", ".yaml", ".json", ".md", ".txt",

            // Template and documentation
            ".st", ".adoc",

            // JVM languages
            ".kt", ".scala",

            // Database
            ".sql", ".cql",
            // Web technologies
            ".html", ".css", ".js", ".ts", ".jsp", ".asp", ".aspx", ".php",
            // System and configuration
            ".conf", ".cmd", ".sh", ".ps1",

            // Programming languages
            ".py", ".c", ".cpp", ".cs", ".rb", ".vb", ".go", ".swift",
            ".lua", ".pl", ".r",

            // Document formats
            ".pdf");// Persistence for indexed files
    private static final String INDEXED_FILES_CACHE = ".indexed_files_cache.txt";
    private final Map<String, Long> fileModificationTimes = new ConcurrentHashMap<>();

    @PostConstruct
    public void initializeIndexing() {
        System.out.println("🚀 Starting hybrid indexing system...");
        // Skip collection initialization - will be done when directory is set
        // initializeQdrantCollection();
    }

    private void initializeQdrantCollection() {
        // Temporarily suppress all gRPC and Qdrant logging during initialization
        suppressLogging();
        try {
            System.out.println("🔧 Checking Qdrant collection: " + collectionName);

            // Check if collection exists
            boolean collectionExists = checkCollectionExists(collectionName);

            if (!collectionExists) {
                System.out.println("🆕 Creating Qdrant collection: " + collectionName);
                createCollection(collectionName);
                System.out.println("✅ Qdrant collection created successfully");
            } else {
                System.out.println("✅ Qdrant collection ready");
            }

        } catch (Exception e) {
            System.err.println("⚠️ Warning: Could not initialize Qdrant collection: " + e.getMessage());
            System.err.println("💡 Make sure Ollama is running with nomic-embed-text model");
        } finally {
            // Restore normal logging
            restoreLogging();
        }
    }

    private void suppressLogging() {
        // Suppress gRPC and Qdrant loggers programmatically
        setLoggerLevel("io.grpc", Level.OFF);
        setLoggerLevel("io.qdrant", Level.OFF);
        setLoggerLevel("io.netty", Level.OFF);
        setLoggerLevel("grpc", Level.OFF);
    }

    private void restoreLogging() {
        // Restore to WARN level (as per application.properties)
        setLoggerLevel("io.grpc", Level.WARN);
        setLoggerLevel("io.qdrant", Level.WARN);
        setLoggerLevel("io.netty", Level.WARN);
        setLoggerLevel("grpc", Level.WARN);
    }

    private void setLoggerLevel(String loggerName, Level level) {
        try {
            Logger logger = (Logger) LoggerFactory.getLogger(loggerName);
            logger.setLevel(level);
        } catch (Exception e) {
            // Ignore any errors setting log levels
        }
    }

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
            // Log other types of errors
            System.err.println("⚠️ Warning: Error checking collection existence: " + e.getMessage());
            return false;
        } catch (Exception e) {
            // Check for collection not found in any exception
            String errorMessage = e.getMessage();
            if (errorMessage != null &&
                    (errorMessage.contains("NOT_FOUND") || errorMessage.contains("doesn't exist"))) {
                return false;
            }
            // Log unexpected exceptions
            System.err.println("⚠️ Warning: Unexpected error checking collection existence: " + e.getMessage());
            return false;
        }
    }

    private void deleteCollection(String targetCollectionName) {
        try {
            qdrantClient.deleteCollectionAsync(targetCollectionName).get();
            System.out.println("🗑️ Deleted collection: " + targetCollectionName);
        } catch (Exception e) {
            System.err.println("⚠️ Warning: Error deleting collection " + targetCollectionName + ": " + e.getMessage());
        }
    }

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
                .build();

        // Execute collection creation
        qdrantClient.createCollectionAsync(createCollection).get();
    }

    /**
     * Set the directory to index
     */
    public void setIndexingDirectory(String directory) {
        this.indexingDirectory = directory;
        System.out.println("📁 Indexing directory set to: " + directory);

        // If indexing is not in progress, restart it with new directory
        if (!indexingInProgress) {
            // Reset counters and statistics
            resetIndexingStatistics();

            // Load previously indexed files to avoid re-indexing
            loadIndexedFilesCache();

            startHybridIndexing();
        }
    }

    private void resetIndexingStatistics() {
        totalFiles.set(0);
        indexedFiles.set(0);
        indexedFilePaths.clear();
        fileModificationTimes.clear();
        indexingComplete = false;
        // Reset enhanced metrics
        activeVirtualThreads.set(0);
        peakVirtualThreads.set(0);
        totalTasksExecuted.set(0);
        failedFiles.set(0);
        skippedFiles.set(0);
        fileTypeStatistics.clear();
        skippedFileExtensions.clear();

        // Clear the cache when resetting (usually when changing directory)
        clearIndexedFilesCache();
    }

    public void startHybridIndexing() {
        if (indexingInProgress) {
            System.out.println("⚠️ Indexing already in progress");
            return;
        }

        indexingInProgress = true;
        startTime.set(System.currentTimeMillis());

        // Start with priority files first
        indexPriorityFilesAsync();

        // Then continue with remaining files in background
        indexRemainingFilesAsync();
    }

    @Async("indexingExecutor")
    public CompletableFuture<Void> indexPriorityFilesAsync() {
        try {
            System.out.println("📋 Phase 1: Indexing priority files in background...");

            List<File> priorityFiles = getPriorityFiles();
            List<File> newPriorityFiles = priorityFiles.stream()
                    .filter(this::needsReindexing)
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
        }

        return CompletableFuture.completedFuture(null);
    }

    @Async("virtualThreadExecutor")
    public CompletableFuture<Void> indexRemainingFilesAsync() {
        try {
            // Wait a bit to let priority files finish
            Thread.sleep(1000);

            System.out.println("📋 Phase 2: Indexing remaining files in background...");
            List<File> allFiles = getAllCodebaseFiles();
            List<File> remainingFiles = allFiles.stream()
                    .filter(this::needsReindexing)
                    .toList();

            totalFiles.addAndGet(remainingFiles.size());

            if (remainingFiles.isEmpty()) {
                System.out.println("✅ All files already indexed! Indexing complete.");
                indexingComplete = true;
                indexingInProgress = false;
                return CompletableFuture.completedFuture(null);
            }

            System.out.println("🚀 Processing " + remainingFiles.size() + " new/modified files in background");

            // Process files in batches using virtual threads for optimal I/O performance
            int batchSize = 20; // Process files in batches to avoid overwhelming the system
            for (int i = 0; i < remainingFiles.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, remainingFiles.size());
                List<File> batch = remainingFiles.subList(i, endIndex);
                // Create virtual threads for each file in the batch
                List<CompletableFuture<Void>> batchFutures = batch.stream()
                        .filter(this::needsReindexing)
                        .map(file -> CompletableFuture.runAsync(() -> indexFile(file), virtualThreadExecutor))
                        .toList();
                // Wait for batch to complete before processing next batch
                CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0])).join();

                // Batch completed - no console output for progress
            }

            indexingComplete = true;
            indexingInProgress = false;

            long duration = (System.currentTimeMillis() - startTime.get()) / 1000;
            System.out.println("🎉 Complete indexing finished! " +
                    indexedFiles.get() + " files indexed in " + duration + "s");

        } catch (Exception e) {
            System.err.println("❌ Error in background indexing: " + e.getMessage());
            indexingInProgress = false;
        }

        return CompletableFuture.completedFuture(null);
    }

    private List<File> getPriorityFiles() {
        List<File> priorityFiles = new ArrayList<>();

        try {
            // Check if the configured indexing directory exists
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

            // Also check current directory for other important files
            try (Stream<Path> paths = Files.walk(Paths.get("."), 2)) { // Max depth 2
                List<File> candidates = paths
                        .filter(Files::isRegularFile)
                        .filter(this::isSupportedFile)
                        .filter(this::isNotInExcludedDirectory)
                        .map(Path::toFile)
                        .toList();

                // Add high priority files from root directory
                candidates.stream()
                        .filter(file -> getFilePriority(file) <= 5)
                        .filter(file -> !priorityFiles.contains(file))
                        .forEach(priorityFiles::add);
            }

            // Sort by priority
            priorityFiles.sort((f1, f2) -> {
                int p1 = getFilePriority(f1);
                int p2 = getFilePriority(f2);
                return Integer.compare(p1, p2);
            });

        } catch (Exception e) {
            System.err.println("❌ Error finding priority files: " + e.getMessage());
            e.printStackTrace();
        }

        return priorityFiles;
    }

    private List<File> getAllCodebaseFiles() {
        List<File> allFiles = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(Paths.get(indexingDirectory))) {
            List<Path> allPaths = paths
                    .filter(Files::isRegularFile)
                    .filter(this::isNotInExcludedDirectory)
                    .toList();

            // Track skipped file extensions
            for (Path path : allPaths) {
                if (isSupportedFile(path)) {
                    allFiles.add(path.toFile());
                } else {
                    // Track skipped file extensions
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
        String fileName = path.getFileName().toString().toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    private boolean isNotInExcludedDirectory(Path path) {
        String pathStr = path.toString().toLowerCase();
        return !pathStr.contains("target") &&
                !pathStr.contains(".git") &&
                !pathStr.contains("node_modules") &&
                !pathStr.contains(".idea") &&
                !pathStr.contains(".vscode");
    }

    private int getFilePriority(File file) {
        String fileName = file.getName();

        // Check for specific patterns first
        for (Map.Entry<String, Integer> entry : FILE_PRIORITIES.entrySet()) {
            if (fileName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // Default priority
        return 10;
    }

    private void indexFile(File file) {
        // Track virtual thread usage
        int currentThreads = activeVirtualThreads.incrementAndGet();
        totalTasksExecuted.incrementAndGet();

        // Update peak thread count
        int peak = peakVirtualThreads.get();
        while (currentThreads > peak && !peakVirtualThreads.compareAndSet(peak, currentThreads)) {
            peak = peakVirtualThreads.get();
        }

        try {
            if (file.length() > 1024 * 1024) { // Skip files larger than 1MB
                skippedFiles.incrementAndGet();
                // Log to background only
                return;
            }

            // Track file type statistics
            String fileType = getFileExtension(file);
            AtomicInteger count = fileTypeStatistics.get(fileType);
            if (count == null) {
                count = new AtomicInteger(0);
                fileTypeStatistics.put(fileType, count);
            }
            count.incrementAndGet();
            // STEP 1: Raw Text Extraction
            List<Document> documents = createDocumentsFromFile(file);
            if (!documents.isEmpty()) {
                try {
                    // STEP 2-4: Text → nomic-embed-text → Vector → Qdrant
                    // Use dynamic VectorStore with the correct collection name for this directory
                    VectorStore dynamicVectorStore = vectorStoreFactory.createVectorStore(collectionName);

                    // This happens inside vectorStore.add() which:
                    // 1. Takes raw text from Document
                    // 2. Sends to nomic-embed-text model for embedding
                    // 3. Converts to vector representation
                    // 4. Stores in Qdrant with metadata in the correct collection
                    dynamicVectorStore.add(documents);

                    indexedFilePaths.add(file.getAbsolutePath());
                    indexedFiles.incrementAndGet();

                    // Save to persistent cache to avoid re-indexing
                    saveIndexedFile(file.getAbsolutePath());
                } catch (Exception embeddingError) {
                    // Handle specific embedding/encoding errors
                    String errorMessage = embeddingError.getMessage();
                    if (errorMessage != null && (errorMessage.contains("Encoding special tokens") ||
                            errorMessage.contains("token") ||
                            errorMessage.contains("encoding") ||
                            errorMessage.contains("special"))) {

                        System.out.println("⚠️ Failed to index " + file.getName() + ": " + errorMessage);
                        // Mark as skipped due to encoding issues
                        skippedFiles.incrementAndGet();

                        // Track files that couldn't be embedded using the already declared fileType
                        // variable
                        AtomicInteger skipCount = skippedFileExtensions.get(fileType);
                        if (skipCount == null) {
                            skipCount = new AtomicInteger(0);
                            skippedFileExtensions.put(fileType, skipCount);
                        }
                        skipCount.incrementAndGet();
                    } else {
                        // Re-throw other types of errors
                        throw embeddingError;
                    }
                }

                // Progress tracking for internal use only - no console output
            } else {
                skippedFiles.incrementAndGet();
            }

        } catch (Exception e) {
            failedFiles.incrementAndGet();
            // Log errors to background only - don't spam console
        } finally {
            // Decrement active thread count
            activeVirtualThreads.decrementAndGet();
        }
    }

    /**
     * STEP 1: Extract Raw Text from files
     * Creates Document objects containing raw text content and metadata
     * This is the first step in the flow: Raw Text → Embedding Model → Vector →
     * Qdrant
     */
    private List<Document> createDocumentsFromFile(File file) {
        try {
            if (file.getName().endsWith(".java") ||
                    file.getName().endsWith(".xml") ||
                    file.getName().endsWith(".properties") ||
                    file.getName().endsWith(".md")) {

                // STEP 1a: Read raw text content from file
                String content = Files.readString(file.toPath());
                // STEP 1b: Create metadata for the document
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("filename", file.getName());
                metadata.put("filepath", file.getAbsolutePath());
                metadata.put("filetype", getFileExtension(file));
                metadata.put("priority", String.valueOf(getFilePriority(file)));
                metadata.put("size", String.valueOf(file.length()));
                metadata.put("lastModified", String.valueOf(file.lastModified()));
                metadata.put("lastModifiedDate", new java.util.Date(file.lastModified()).toString());
                metadata.put("collectionName", collectionName);
                metadata.put("indexedAt", new java.util.Date().toString());

                // STEP 1c: Split large files into manageable chunks
                // Each chunk will be processed through: Text → nomic-embed-text → Vector →
                // Qdrant
                List<Document> documents = new ArrayList<>();
                if (content.length() > 4000) {
                    List<String> chunks = splitIntoChunks(content, 3000, 500); // 3000 chars with 500 overlap
                    for (int i = 0; i < chunks.size(); i++) {
                        Map<String, Object> chunkMetadata = new HashMap<>(metadata);
                        chunkMetadata.put("chunk", String.valueOf(i + 1));
                        chunkMetadata.put("total_chunks", String.valueOf(chunks.size()));
                        // Create Document with raw text - ready for embedding pipeline
                        documents.add(new Document(chunks.get(i), chunkMetadata));
                    }
                } else {
                    // Create Document with raw text - ready for embedding pipeline
                    documents.add(new Document(content, metadata));
                }

                return documents;
            }

        } catch (Exception e) {
            System.err.println("❌ Error creating document for " + file.getName() + ": " + e.getMessage());
        }

        return List.of();
    }

    private List<String> splitIntoChunks(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        
        // For Python files, prioritize function definitions and decorators
        if (isPythonFile(text)) {
            chunks.addAll(createPythonAwareChunks(text, chunkSize, overlap));
        } else {
            chunks.addAll(createStandardChunks(text, chunkSize, overlap));
        }

        return chunks;
    }
    
    private boolean isPythonFile(String text) {
        // Check for Python-specific patterns
        return text.contains("@app.route") || 
               text.contains("def ") || 
               text.contains("import ") ||
               text.contains("from ") ||
               text.contains("class ") ||
               text.contains("if __name__ == '__main__':");
    }
    
    private List<String> createPythonAwareChunks(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        String[] lines = text.split("\n");
        
        List<Integer> importantLineIndices = new ArrayList<>();
        
        // Find important lines (decorators, function definitions, class definitions)
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("@") || 
                line.startsWith("def ") || 
                line.startsWith("class ") ||
                line.contains("@app.route") ||
                line.contains("@app.errorhandler") ||
                line.startsWith("if __name__")) {
                importantLineIndices.add(i);
            }
        }
        
        // Create chunks around important lines
        for (int importantIndex : importantLineIndices) {
            int start = Math.max(0, importantIndex - 5); // 5 lines before
            int end = Math.min(lines.length, importantIndex + 15); // 15 lines after
            
            StringBuilder chunk = new StringBuilder();
            for (int i = start; i < end; i++) {
                chunk.append(lines[i]).append("\n");
            }
            
            String chunkText = chunk.toString().trim();
            if (!chunkText.isEmpty() && chunkText.length() > 50) { // Minimum meaningful size
                chunks.add(chunkText);
            }
        }
        
        // If no important lines found or chunks too small, fall back to standard chunking
        if (chunks.isEmpty()) {
            chunks.addAll(createStandardChunks(text, chunkSize, overlap));
        }
        
        return chunks;
    }
    
    private List<String> createStandardChunks(String text, int chunkSize, int overlap) {
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

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(lastDot) : "";
    }

    // Status methods
    public boolean isIndexingComplete() {
        return indexingComplete;
    }

    public boolean isIndexingInProgress() {
        return indexingInProgress;
    }

    public int getIndexedFileCount() {
        return indexedFiles.get();
    }

    public int getTotalFileCount() {
        return totalFiles.get();
    }

    public double getIndexingProgress() {
        int total = totalFiles.get();
        return total > 0 ? (indexedFiles.get() * 100.0 / total) : 0.0;
    }

    public Set<String> getIndexedFiles() {
        return new HashSet<>(indexedFilePaths);
    }

    // Enhanced metrics methods
    public long getCurrentIndexingDuration() {
        if (startTime.get() == 0)
            return 0;
        return System.currentTimeMillis() - startTime.get();
    }

    public long getTotalIndexingDuration() {
        if (startTime.get() == 0)
            return 0;
        if (indexingInProgress) {
            return getCurrentIndexingDuration();
        }
        // If completed, we need to track the end time
        return getCurrentIndexingDuration();
    }

    public long getEstimatedTotalDuration() {
        if (indexedFiles.get() == 0)
            return 0;

        long currentDuration = getCurrentIndexingDuration();
        int totalFilesToIndex = totalFiles.get();
        int filesIndexed = indexedFiles.get();

        if (filesIndexed == 0)
            return 0;

        // Estimate based on current progress
        return (currentDuration * totalFilesToIndex) / filesIndexed;
    }

    public double getIndexingSpeed() {
        long duration = getCurrentIndexingDuration();
        if (duration == 0)
            return 0.0;

        return (indexedFiles.get() * 1000.0) / duration; // files per second
    }

    public int getActiveVirtualThreads() {
        return activeVirtualThreads.get();
    }

    public int getPeakVirtualThreads() {
        return peakVirtualThreads.get();
    }

    public long getTotalTasksExecuted() {
        return totalTasksExecuted.get();
    }

    public int getFailedFileCount() {
        return failedFiles.get();
    }

    public int getSkippedFileCount() {
        return skippedFiles.get();
    }

    public Map<String, Integer> getFileTypeStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        fileTypeStatistics.forEach((type, count) -> stats.put(type, count.get()));
        return stats;
    }

    public Map<String, Integer> getSkippedFileExtensions() {
        Map<String, Integer> stats = new HashMap<>();
        skippedFileExtensions.forEach((extension, count) -> stats.put(extension, count.get()));
        return stats;
    }

    /**
     * Load previously indexed files from cache to avoid re-indexing
     * Also validates files still exist and haven't been modified
     */
    private void loadIndexedFilesCache() {
        try {
            Path cacheFile = Paths.get(INDEXED_FILES_CACHE);
            if (Files.exists(cacheFile)) {
                List<String> cachedFiles = Files.readAllLines(cacheFile);
                List<String> validCacheEntries = new ArrayList<>();
                List<String> deletedFiles = new ArrayList<>();
                int modifiedFiles = 0;

                for (String line : cachedFiles) {
                    if (line.trim().startsWith("INDEXED:")) {
                        String[] parts = line.split("\\|");
                        String filePath = parts[0].substring("INDEXED:".length()).trim();
                        long cachedModTime = parts.length > 1 ? Long.parseLong(parts[1]) : 0;

                        Path file = Paths.get(filePath);
                        if (Files.exists(file)) {
                            try {
                                long currentModTime = Files.getLastModifiedTime(file).toMillis();
                                if (currentModTime == cachedModTime) {
                                    // File unchanged - keep in cache
                                    indexedFilePaths.add(filePath);
                                    fileModificationTimes.put(filePath, currentModTime);
                                    indexedFiles.incrementAndGet();
                                    validCacheEntries.add(line);
                                } else {
                                    // File modified - needs re-indexing
                                    modifiedFiles++;
                                }
                            } catch (Exception e) {
                                // Error reading file modification time - assume modified
                                modifiedFiles++;
                            }
                        } else {
                            // File deleted - remove from cache
                            deletedFiles.add(filePath);
                        }
                    }
                }

                // Handle deleted files
                if (!deletedFiles.isEmpty()) {
                    removeDeletedFilesFromVectorStore(deletedFiles);
                }

                // Update cache file with only valid entries
                if (!deletedFiles.isEmpty() || modifiedFiles > 0) {
                    rebuildCacheFile(validCacheEntries);
                }

                System.out.println("📋 Cache loaded: " + indexedFilePaths.size() + " valid files, " +
                        deletedFiles.size() + " deleted, " + modifiedFiles + " modified");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Could not load indexed files cache: " + e.getMessage());
        }
    }

    /**
     * Save an indexed file to cache with modification time
     */
    private void saveIndexedFile(String filePath) {
        try {
            Path file = Paths.get(filePath);
            long modTime = Files.getLastModifiedTime(file).toMillis();
            fileModificationTimes.put(filePath, modTime);

            String cacheEntry = "INDEXED:" + filePath + "|" + modTime + System.lineSeparator();
            Files.writeString(Paths.get(INDEXED_FILES_CACHE),
                    cacheEntry,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            // Ignore cache write errors
        }
    }

    /**
     * Clear the indexed files cache when changing directory
     */
    private void clearIndexedFilesCache() {
        try {
            Files.deleteIfExists(Paths.get(INDEXED_FILES_CACHE));
        } catch (Exception e) {
            // Ignore cache deletion errors
        }
    }

    /**
     * Rebuild the cache file with valid entries only
     */
    private void rebuildCacheFile(List<String> validEntries) {
        try {
            Files.deleteIfExists(Paths.get(INDEXED_FILES_CACHE));
            if (!validEntries.isEmpty()) {
                Files.write(Paths.get(INDEXED_FILES_CACHE), validEntries,
                        StandardOpenOption.CREATE);
            }
        } catch (Exception e) {
            // Ignore cache rebuild errors
        }
    }

    /**
     * Check if a file needs re-indexing (new or modified)
     */
    private boolean needsReindexing(File file) {
        String filePath = file.getAbsolutePath();

        // If not in cache, needs indexing
        if (!indexedFilePaths.contains(filePath)) {
            return true;
        }

        // Check if file has been modified
        try {
            long currentModTime = Files.getLastModifiedTime(file.toPath()).toMillis();
            Long cachedModTime = fileModificationTimes.get(filePath);

            if (cachedModTime == null || currentModTime != cachedModTime) {
                // File modified - remove from cache and re-index
                indexedFilePaths.remove(filePath);
                fileModificationTimes.remove(filePath);
                return true;
            }
        } catch (Exception e) {
            // Error reading modification time - assume needs re-indexing
            return true;
        }

        return false;
    }

    /**
     * Remove deleted files from vector store
     */
    private void removeDeletedFilesFromVectorStore(List<String> deletedFiles) {
        if (deletedFiles.isEmpty()) {
            return;
        }

        try {
            // Note: Qdrant doesn't have a direct API to delete by metadata
            // This would require implementing a custom deletion strategy
            // For now, we just remove from our cache - the vector store will contain
            // orphaned entries
            System.out.println("⚠️ " + deletedFiles.size() + " deleted files removed from cache");
            // TODO: Implement vector store cleanup for deleted files
        } catch (Exception e) {
            System.err.println("⚠️ Error removing deleted files from vector store: " + e.getMessage());
        }
    }

    /**
     * Restart the indexing process
     */
    public void restartIndexing() {
        try {
            System.out.println("🔄 Restarting indexing process...");

            // Step 1: Delete and recreate the Qdrant collection to remove all old vector
            // data
            deleteAndRecreateCollection();

            // Step 2: Reset state
            indexingInProgress = false;
            indexingComplete = false;
            indexedFiles.set(0);
            startTime.set(System.currentTimeMillis());

            // Step 3: Clear local cache and file modification times
            indexedFilePaths.clear();
            fileModificationTimes.clear();

            // Step 4: Clear the cache file on disk
            clearIndexedFilesCache();

            // Step 5: Reset all counters and statistics
            failedFiles.set(0);
            skippedFiles.set(0);
            fileTypeStatistics.clear();
            skippedFileExtensions.clear();

            // Step 6: Start fresh indexing with clean collection
            startHybridIndexing();

            System.out.println("✅ Collection cleared and indexing restarted successfully");

        } catch (Exception e) {
            System.err.println("❌ Failed to restart indexing: " + e.getMessage());
            throw new RuntimeException("Failed to restart indexing", e);
        }
    }

    /**
     * Clear cache and reindex all files
     */
    public void clearCacheAndReindex() {
        try {
            System.out.println("🗑️ Clearing cache and starting fresh indexing...");

            // Step 1: Delete and recreate the Qdrant collection to remove all old vector
            // data
            deleteAndRecreateCollection();

            // Step 2: Clear the local cached file set
            indexedFilePaths.clear();
            fileModificationTimes.clear();

            // Step 3: Clear the cache file on disk
            clearIndexedFilesCache();

            // Step 4: Reset all counters
            indexedFiles.set(0);
            failedFiles.set(0);
            skippedFiles.set(0);
            fileTypeStatistics.clear();
            skippedFileExtensions.clear();

            // Step 5: Reset timing
            startTime.set(System.currentTimeMillis());

            // Step 6: Reset status flags
            indexingInProgress = false;
            indexingComplete = false;

            // Step 7: Start fresh indexing with clean collection
            startHybridIndexing();

            System.out.println("✅ Collection cleared, cache cleared, and reindexing started");

        } catch (Exception e) {
            System.err.println("❌ Failed to clear cache and reindex: " + e.getMessage());
            throw new RuntimeException("Failed to clear cache and reindex", e);
        }
    }

    /**
     * Get the current indexing directory
     */
    public String getCurrentIndexingDirectory() {
        return indexingDirectory != null ? indexingDirectory : "Not set";
    }

    /**
     * Set the Qdrant collection name dynamically
     * 
     * @param collectionName the name of the collection to use
     */
    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
        System.out.println("📦 Collection name set to: " + collectionName);

        // Initialize both our dynamic collection AND the default VectorStore collection
        initializeBothCollections();
    }

    /**
     * Initialize the dynamic collection based on the directory being indexed
     */
    private void initializeBothCollections() {
        suppressLogging();

        try {
            // Initialize our dynamically named collection
            System.out.println("🔧 Checking dynamic collection: " + collectionName);
            if (!checkCollectionExists(collectionName)) {
                System.out.println("🆕 Creating dynamic collection: " + collectionName);
                createCollection(collectionName);
                System.out.println("✅ Dynamic collection created: " + collectionName);
            } else {
                System.out.println("✅ Dynamic collection ready: " + collectionName);
            }

        } catch (Exception e) {
            System.err.println("⚠️ Warning: Error during collection initialization: " + e.getMessage());
        } finally {
            restoreLogging();
        }
    }

    /**
     * Clean up existing collections with wrong dimensions and initialize the
     * correct collection
     */
    private void cleanupAndInitializeCollection() {
        suppressLogging();

        try {
            // First, clean up any collections that might have wrong dimensions
            cleanupWrongDimensionCollections();

            // Then initialize our collection
            initializeQdrantCollection();

        } catch (Exception e) {
            System.err.println("⚠️ Warning: Error during collection cleanup and initialization: " + e.getMessage());
        } finally {
            restoreLogging();
        }
    }

    /**
     * Clean up collections that have wrong vector dimensions
     */
    private void cleanupWrongDimensionCollections() {
        try {
            // List of potential collection names that might exist with wrong dimensions
            String[] potentialCollections = {
                    "codebase-index",
                    "codebase-index-ollama",
                    "codebase-index-spring-ai",
                    collectionName // Also check our current collection name
            };

            for (String collName : potentialCollections) {
                try {
                    CollectionInfo info = qdrantClient.getCollectionInfoAsync(collName).get();
                    if (info != null) {
                        var vectorConfig = info.getConfig().getParams().getVectorsConfig();
                        if (vectorConfig.hasParams()) {
                            long dimensions = vectorConfig.getParams().getSize();
                            if (dimensions != 768) {
                                System.out.println("🗑️ Deleting collection '" + collName + "' with wrong dimensions: "
                                        + dimensions);
                                qdrantClient.deleteCollectionAsync(collName).get();
                            }
                        }
                    }
                } catch (Exception e) {
                    // Collection doesn't exist or other error - ignore
                    String errorMessage = e.getMessage();
                    if (errorMessage == null ||
                            (!errorMessage.contains("NOT_FOUND") && !errorMessage.contains("doesn't exist"))) {
                        System.err.println("⚠️ Warning checking collection '" + collName + "': " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Warning during collection cleanup: " + e.getMessage());
        }
    }

    /**
     * Set both indexing directory and collection name for the codebase directory
     * 
     * @param directory the directory to index
     */
    public void setIndexingDirectoryWithCollection(String directory) {
        setIndexingDirectory(directory);

        // Generate collection name based on the actual directory being indexed
        String collectionName = generateCollectionName(directory);
        setCollectionName(collectionName);
    }

    /**
     * Generate collection name based on directory path
     * Examples:
     * - d:\Projects\misoto-indexer\codebase\spring-ai → codebase-index-spring-ai
     * - d:\Projects\misoto-indexer\codebase\ollama → codebase-index-ollama
     * - d:\Projects\misoto-indexer\src → codebase-index-src
     * - /home/user/project → codebase-index-project
     */
    private String generateCollectionName(String directory) {
        try {
            // Normalize path separators
            String normalizedDir = directory.replace('\\', '/');

            // Extract the last directory name
            String[] parts = normalizedDir.split("/");
            String lastDir = parts[parts.length - 1];

            // If it's within a codebase directory, use the subdirectory name
            if (directory.contains("codebase") && parts.length >= 2) {
                // Find the index of "codebase" in the path
                for (int i = 0; i < parts.length; i++) {
                    if ("codebase".equals(parts[i]) && i + 1 < parts.length) {
                        // Use the directory after "codebase"
                        lastDir = parts[i + 1];
                        break;
                    }
                }
            }

            // Clean up the directory name (remove special characters, lowercase)
            String cleanName = lastDir.replaceAll("[^a-zA-Z0-9\\-_]", "-")
                    .replaceAll("-+", "-")
                    .toLowerCase()
                    .replaceAll("^-|-$", ""); // Remove leading/trailing dashes

            return "codebase-index-" + cleanName;

        } catch (Exception e) {
            System.err.println("⚠️ Error generating collection name for " + directory + ": " + e.getMessage());
            // Fallback to default
            return "codebase-index";
        }
    }

    /**
     * Get the current collection name being used for indexing
     */
    public String getCurrentCollectionName() {
        return collectionName;
    }

    /**
     * Delete and recreate the Qdrant collection to ensure clean vector data
     */
    private void deleteAndRecreateCollection() {
        suppressLogging();

        try {
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
}
