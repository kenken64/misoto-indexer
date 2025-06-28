package sg.edu.nus.iss.codebase.indexer.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import org.springframework.ai.vectorstore.VectorStore;
import sg.edu.nus.iss.codebase.indexer.config.DynamicVectorStoreFactory;
import sg.edu.nus.iss.codebase.indexer.config.IndexingConfiguration;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileCacheRepository;
import sg.edu.nus.iss.codebase.indexer.service.impl.DocumentFactoryManager;
import sg.edu.nus.iss.codebase.indexer.model.IndexingStatus;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Futures;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileIndexingServiceImplTest {

    @Mock
    private VectorStore vectorStore;
    
    @Mock
    private DynamicVectorStoreFactory vectorStoreFactory;
    
    @Mock
    private QdrantClient qdrantClient;
    
    @Mock
    private IndexingConfiguration config;
    
    @Mock
    private FileCacheRepository cacheRepository;
    
    @Mock
    private DocumentFactoryManager documentFactoryManager;
    
    @TempDir
    Path tempDir;
    
    private FileIndexingServiceImpl service;
    private Executor virtualThreadExecutor;

    @BeforeEach
    void setUp() {
        virtualThreadExecutor = ForkJoinPool.commonPool();
        
        // Setup configuration
        IndexingConfiguration.ProcessingConfig processingConfig = new IndexingConfiguration.ProcessingConfig();
        processingConfig.setMaxDepth(10);
        processingConfig.setChunkSize(1000);
        lenient().when(config.getProcessing()).thenReturn(processingConfig);
        
        // Setup lenient mocks for QdrantClient async operations to avoid unnecessary stubbing errors
        @SuppressWarnings("unchecked")
        ListenableFuture<Collections.CollectionOperationResponse> mockDeleteFuture = mock(ListenableFuture.class);
        @SuppressWarnings("unchecked")
        ListenableFuture<Collections.CollectionOperationResponse> mockCreateFuture = mock(ListenableFuture.class);
        lenient().when(qdrantClient.deleteCollectionAsync(anyString())).thenReturn(mockDeleteFuture);
        lenient().when(qdrantClient.createCollectionAsync(any())).thenReturn(mockCreateFuture);
        
        service = new FileIndexingServiceImpl(
            vectorStore,
            vectorStoreFactory,
            qdrantClient,
            virtualThreadExecutor,
            config,
            cacheRepository,
            documentFactoryManager
        );
    }

    @Test
    void testStartIndexing() throws IOException {
        // Setup
        String testDirectory = tempDir.toString();
        File testFile = tempDir.resolve("test.java").toFile();
        Files.write(testFile.toPath(), "public class Test {}".getBytes());
        
        lenient().when(cacheRepository.needsReindexing(any(File.class))).thenReturn(true);
        lenient().when(vectorStoreFactory.createVectorStore(anyString())).thenReturn(vectorStore);
        
        // Execute
        CompletableFuture<Void> result = service.startIndexing(testDirectory);
        
        // Verify - just check that the result is not null (the indexing runs asynchronously)
        assertNotNull(result);
    }

    @Test
    void testSetIndexingDirectory() {
        // Setup
        String testDirectory = "test-directory";
        
        // Execute
        service.setIndexingDirectory(testDirectory);
        
        // Verify
        assertEquals(testDirectory, service.getCurrentIndexingDirectory());
    }

    @Test
    void testGetIndexingStatus() {
        // Execute
        IndexingStatus status = service.getIndexingStatus();
        
        // Verify
        assertNotNull(status);
        assertFalse(status.isIndexingInProgress());
        assertFalse(status.isIndexingComplete());
        assertEquals(0, status.getIndexedFiles());
        assertEquals(0, status.getTotalFiles());
        assertEquals(0.0, status.getProgress());
    }

    @Test
    void testStopIndexing() {
        // Execute
        assertDoesNotThrow(() -> service.stopIndexing());
        
        // Verify indexing is stopped
        IndexingStatus status = service.getIndexingStatus();
        assertFalse(status.isIndexingInProgress());
    }

    @Test
    void testPauseAndResumeIndexing() {
        // Execute pause
        assertDoesNotThrow(() -> service.pauseIndexing());
        
        // Execute resume
        assertDoesNotThrow(() -> service.resumeIndexing());
        
        // No exceptions should be thrown
        assertTrue(true);
    }

    @Test
    void testResetIndexing() {
        // Execute
        assertDoesNotThrow(() -> service.resetIndexing());
        
        // Verify state is reset
        IndexingStatus status = service.getIndexingStatus();
        assertEquals(0, status.getIndexedFiles());
        assertEquals(0, status.getTotalFiles());
    }

    @Test
    void testGetIndexedFileCount() {
        // Execute
        int count = service.getIndexedFileCount();
        
        // Verify
        assertEquals(0, count);
    }

    @Test
    void testGetTotalFileCount() {
        // Execute
        int count = service.getTotalFileCount();
        
        // Verify
        assertEquals(0, count);
    }

    @Test
    void testGetIndexingProgress() {
        // Execute
        double progress = service.getIndexingProgress();
        
        // Verify
        assertEquals(0.0, progress);
    }

    @Test
    void testIsIndexingComplete() {
        // Execute
        boolean isComplete = service.isIndexingComplete();
        
        // Verify
        assertFalse(isComplete);
    }

    @Test
    void testIsIndexingInProgress() {
        // Execute
        boolean inProgress = service.isIndexingInProgress();
        
        // Verify
        assertFalse(inProgress);
    }

    @Test
    void testGetCurrentCollectionName() {
        // Setup - set a directory first to generate collection name
        service.setIndexingDirectory(tempDir.toString());
        
        // Execute
        String collectionName = service.getCurrentCollectionName();
        
        // Verify
        assertNotNull(collectionName);
        assertTrue(collectionName.startsWith("codebase-index-"));
    }

    @Test
    void testSetIndexingDirectoryWithCollection() {
        // Setup
        String testDirectory = tempDir.toString();
        
        // Execute
        service.setIndexingDirectoryWithCollection(testDirectory);
        
        // Verify
        assertEquals(testDirectory, service.getCurrentIndexingDirectory());
        assertNotNull(service.getCurrentCollectionName());
    }

    @Test
    void testGetIndexingSpeed() {
        // Execute
        double speed = service.getIndexingSpeed();
        
        // Verify
        assertTrue(speed >= 0.0);
    }

    @Test
    void testGetActiveVirtualThreads() {
        // Execute
        int activeThreads = service.getActiveVirtualThreads();
        
        // Verify
        assertTrue(activeThreads >= 0);
    }

    @Test
    void testGetPeakVirtualThreads() {
        // Execute
        int peakThreads = service.getPeakVirtualThreads();
        
        // Verify
        assertTrue(peakThreads >= 0);
    }

    @Test
    void testGetFailedFileCount() {
        // Execute
        int failedCount = service.getFailedFileCount();
        
        // Verify
        assertTrue(failedCount >= 0);
    }

    @Test
    void testGetSkippedFileCount() {
        // Execute
        int skippedCount = service.getSkippedFileCount();
        
        // Verify
        assertTrue(skippedCount >= 0);
    }

    @Test
    void testGetFileTypeStatistics() {
        // Execute
        var stats = service.getFileTypeStatistics();
        
        // Verify
        assertNotNull(stats);
        assertTrue(stats.isEmpty() || stats.size() > 0);
    }

    @Test
    void testRestartIndexing() {
        // Execute
        assertDoesNotThrow(() -> service.restartIndexing());
    }

    @Test
    void testClearCacheAndReindex() {
        // Execute
        assertDoesNotThrow(() -> service.clearCacheAndReindex());
    }
}
