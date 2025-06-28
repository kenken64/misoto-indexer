package sg.edu.nus.iss.codebase.indexer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import sg.edu.nus.iss.codebase.indexer.config.DynamicVectorStoreFactory;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileIndexingService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Stress test for the search functionality
 * Tests performance under high load, concurrent access, and various query patterns
 */
@ExtendWith(MockitoExtension.class)
class SearchStressTest {

    @Mock
    private FileIndexingService indexingService;

    @Mock
    private SearchService searchService;

    @Mock
    private FileSearchService fileSearchService;

    @Mock
    private ChatModel chatModel;

    @Mock
    private DynamicVectorStoreFactory vectorStoreFactory;

    @Mock
    private VectorStore vectorStore;

    private HybridSearchService hybridSearchService;

    @TempDir
    Path tempDir;

    // Test parameters
    private static final int CONCURRENT_THREADS = 20;
    private static final int QUERIES_PER_THREAD = 50;
    private static final int TOTAL_QUERIES = CONCURRENT_THREADS * QUERIES_PER_THREAD;
    private static final long MAX_RESPONSE_TIME_MS = 5000; // 5 seconds max response time
    private static final double MIN_SUCCESS_RATE = 0.95; // 95% success rate required

    // Test data
    private List<String> testQueries;
    private List<Document> mockDocuments;
    private List<FileSearchService.SearchResult> mockFileResults;

    @BeforeEach
    void setUp() throws IOException {
        // The HybridSearchService uses @Autowired injection, so we need to use field injection
        hybridSearchService = new HybridSearchService();
        
        // Use reflection to inject mocks (since it uses field injection)
        try {
            java.lang.reflect.Field indexingServiceField = HybridSearchService.class.getDeclaredField("indexingService");
            indexingServiceField.setAccessible(true);
            indexingServiceField.set(hybridSearchService, indexingService);
            
            java.lang.reflect.Field fileSearchServiceField = HybridSearchService.class.getDeclaredField("fileSearchService");
            fileSearchServiceField.setAccessible(true);
            fileSearchServiceField.set(hybridSearchService, fileSearchService);
            
            java.lang.reflect.Field chatModelField = HybridSearchService.class.getDeclaredField("chatModel");
            chatModelField.setAccessible(true);
            chatModelField.set(hybridSearchService, chatModel);
            
            java.lang.reflect.Field vectorStoreFactoryField = HybridSearchService.class.getDeclaredField("vectorStoreFactory");
            vectorStoreFactoryField.setAccessible(true);
            vectorStoreFactoryField.set(hybridSearchService, vectorStoreFactory);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mocks", e);
        }

        setupMockData();
        setupMockBehavior();
    }

    private void setupMockData() throws IOException {
        // Create diverse test queries
        testQueries = Arrays.asList(
            "public class",
            "private method",
            "String getName",
            "void setProperty",
            "List<Object>",
            "Map<String, Integer>",
            "@Override",
            "@Autowired",
            "try catch",
            "if else",
            "for loop",
            "while condition",
            "switch case",
            "return value",
            "throw exception",
            "interface implementation",
            "abstract class",
            "static final",
            "synchronized method",
            "volatile field",
            "HttpServletRequest",
            "ResponseEntity",
            "Service annotation",
            "Repository pattern",
            "Controller endpoint",
            "JPA entity",
            "database connection",
            "SQL query",
            "REST API",
            "JSON response",
            "XML configuration",
            "Spring Boot",
            "dependency injection",
            "unit test",
            "integration test",
            "mock object",
            "assert equals",
            "test method",
            "before each",
            "after all",
            "exception handling"
        );

        // Create mock documents with realistic content
        mockDocuments = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("fileName", "TestFile" + i + ".java");
            metadata.put("filePath", "/test/path/TestFile" + i + ".java");
            metadata.put("fileType", ".java");
            metadata.put("priority", String.valueOf(i % 5 + 1));

            String content = generateMockJavaContent(i);
            mockDocuments.add(new Document(content, metadata));
        }

        // Create mock file search results
        mockFileResults = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            mockFileResults.add(new FileSearchService.SearchResult(
                "FileResult" + i + ".java",
                "/file/path/FileResult" + i + ".java",
                "Mock file content for result " + i,
                0.8 + (i % 20) * 0.01, // Varying scores
                "file-search"
            ));
        }

        // Create test files in temp directory
        createTestFiles();
    }

    private String generateMockJavaContent(int index) {
        return String.format("""
            package com.example.test;
            
            import java.util.*;
            import org.springframework.stereotype.Service;
            
            /**
             * Test class %d for stress testing search functionality
             */
            @Service
            public class TestClass%d {
                
                private String name = "TestClass%d";
                private int value = %d;
                private List<String> items = new ArrayList<>();
                
                public String getName() {
                    return this.name;
                }
                
                public void setName(String name) {
                    this.name = name;
                }
                
                public int getValue() {
                    return this.value;
                }
                
                public void setValue(int value) {
                    this.value = value;
                }
                
                @Override
                public String toString() {
                    return "TestClass%d{name='" + name + "', value=" + value + "}";
                }
                
                public void processItems() {
                    for (String item : items) {
                        System.out.println("Processing: " + item);
                    }
                }
            }
            """, index, index, index, index, index);
    }

    private void createTestFiles() throws IOException {
        for (int i = 0; i < 20; i++) {
            Path testFile = tempDir.resolve("StressTestFile" + i + ".java");
            Files.writeString(testFile, generateMockJavaContent(i));
        }
    }

    private void setupMockBehavior() {
        // Setup indexing service mocks
        lenient().when(indexingService.getCurrentCollectionName()).thenReturn("stress-test-collection");
        lenient().when(indexingService.getCurrentIndexingDirectory()).thenReturn(tempDir.toString());
        lenient().when(indexingService.isIndexingComplete()).thenReturn(true);
        lenient().when(indexingService.getIndexedFileCount()).thenReturn(100);

        // Setup vector store factory
        lenient().when(vectorStoreFactory.createVectorStore(anyString())).thenReturn(vectorStore);

        // Setup vector store behavior with realistic response times
        lenient().when(vectorStore.similaritySearch(any(SearchRequest.class)))
            .thenAnswer(invocationOnMock -> {
                // Simulate processing time (50-200ms)
                Thread.sleep(50 + new Random().nextInt(150));
                
                // Return random subset of mock documents
                List<Document> results = new ArrayList<>();
                Random random = new Random();
                int numResults = 3 + random.nextInt(8); // 3-10 results
                
                for (int i = 0; i < numResults && i < mockDocuments.size(); i++) {
                    results.add(mockDocuments.get(random.nextInt(mockDocuments.size())));
                }
                
                return results;
            });

        // Setup file search service with realistic behavior
        lenient().when(fileSearchService.searchInFiles(anyString()))
            .thenAnswer(invocationOnMock -> {
                // Simulate file search time (100-300ms)
                Thread.sleep(100 + new Random().nextInt(200));
                
                // Return random subset of file results
                List<FileSearchService.SearchResult> results = new ArrayList<>();
                Random random = new Random();
                int numResults = 2 + random.nextInt(6); // 2-7 results
                
                for (int i = 0; i < numResults && i < mockFileResults.size(); i++) {
                    results.add(mockFileResults.get(random.nextInt(mockFileResults.size())));
                }
                
                return results;
            });
    }

    @Test
    void stressTest_ConcurrentSearchRequests() throws InterruptedException {
        System.out.println("🚀 Starting concurrent search stress test...");
        System.out.println("📊 Test parameters:");
        System.out.println("   - Concurrent threads: " + CONCURRENT_THREADS);
        System.out.println("   - Queries per thread: " + QUERIES_PER_THREAD);
        System.out.println("   - Total queries: " + TOTAL_QUERIES);
        System.out.println("   - Max response time: " + MAX_RESPONSE_TIME_MS + "ms");

        // Metrics tracking
        AtomicInteger successfulQueries = new AtomicInteger(0);
        AtomicInteger failedQueries = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);
        AtomicLong maxResponseTime = new AtomicLong(0);
        AtomicLong minResponseTime = new AtomicLong(Long.MAX_VALUE);
        ConcurrentHashMap<String, AtomicInteger> errorTypes = new ConcurrentHashMap<>();

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);
        long startTime = System.currentTimeMillis();

        // Submit concurrent search tasks
        for (int threadId = 0; threadId < CONCURRENT_THREADS; threadId++) {
            final int finalThreadId = threadId;
            executor.submit(() -> {
                try {
                    performSearchOperations(finalThreadId, successfulQueries, failedQueries, 
                                          totalResponseTime, maxResponseTime, minResponseTime, errorTypes);
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all threads to complete
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        long totalTestTime = System.currentTimeMillis() - startTime;

        executor.shutdown();
        
        // Calculate metrics
        int successful = successfulQueries.get();
        int failed = failedQueries.get();
        double successRate = (double) successful / (successful + failed);
        double avgResponseTime = successful > 0 ? (double) totalResponseTime.get() / successful : 0;
        double throughput = (double) successful / (totalTestTime / 1000.0);

        // Print results
        System.out.println("\n📈 Stress Test Results:");
        System.out.println("   ✅ Successful queries: " + successful);
        System.out.println("   ❌ Failed queries: " + failed);
        System.out.println("   📊 Success rate: " + String.format("%.2f%%", successRate * 100));
        System.out.println("   ⏱️ Average response time: " + String.format("%.2fms", avgResponseTime));
        System.out.println("   ⚡ Max response time: " + maxResponseTime.get() + "ms");
        System.out.println("   🏃 Min response time: " + minResponseTime.get() + "ms");
        System.out.println("   🚀 Throughput: " + String.format("%.2f queries/sec", throughput));
        System.out.println("   ⏰ Total test time: " + totalTestTime + "ms");
        System.out.println("   ✅ Test completed: " + completed);

        if (!errorTypes.isEmpty()) {
            System.out.println("   🚨 Error breakdown:");
            errorTypes.forEach((error, count) -> 
                System.out.println("      - " + error + ": " + count.get()));
        }

        // Assertions
        assertThat(completed).as("Test should complete within timeout").isTrue();
        assertThat(successRate).as("Success rate should be at least " + (MIN_SUCCESS_RATE * 100) + "%")
                               .isGreaterThanOrEqualTo(MIN_SUCCESS_RATE);
        assertThat(maxResponseTime.get()).as("Max response time should be under " + MAX_RESPONSE_TIME_MS + "ms")
                                         .isLessThanOrEqualTo(MAX_RESPONSE_TIME_MS);
        assertThat(successful).as("Should have successful queries").isGreaterThan(0);
    }

    private void performSearchOperations(int threadId, AtomicInteger successfulQueries, 
                                       AtomicInteger failedQueries, AtomicLong totalResponseTime, 
                                       AtomicLong maxResponseTime, AtomicLong minResponseTime,
                                       ConcurrentHashMap<String, AtomicInteger> errorTypes) {
        Random random = new Random(threadId); // Seeded for reproducibility
        
        for (int i = 0; i < QUERIES_PER_THREAD; i++) {
            String query = testQueries.get(random.nextInt(testQueries.size()));
            int maxResults = 5 + random.nextInt(10); // 5-14 results
            
            long queryStart = System.currentTimeMillis();
            
            try {
                HybridSearchService.HybridSearchResult result = 
                    hybridSearchService.performHybridSearch(query, maxResults);
                
                long responseTime = System.currentTimeMillis() - queryStart;
                
                // Update metrics
                successfulQueries.incrementAndGet();
                totalResponseTime.addAndGet(responseTime);
                
                // Update min/max response times
                updateMinMax(responseTime, maxResponseTime, minResponseTime);
                
                // Validate results
                assertThat(result).isNotNull();
                assertThat(result.getTotalResults()).isLessThanOrEqualTo(maxResults);
                
                // Validate result structure
                for (HybridSearchService.SearchResult searchResult : result.getVectorResults()) {
                    assertThat(searchResult.getFileName()).isNotNull();
                    assertThat(searchResult.getFilePath()).isNotNull();
                    assertThat(searchResult.getScore()).isBetween(0.0, 1.0);
                }
                
                for (FileSearchService.SearchResult fileResult : result.getFileResults()) {
                    assertThat(fileResult.getFileName()).isNotNull();
                    assertThat(fileResult.getFilePath()).isNotNull();
                    assertThat(fileResult.getRelevanceScore()).isBetween(0.0, 1.0);
                }
                
            } catch (Exception e) {
                failedQueries.incrementAndGet();
                String errorType = e.getClass().getSimpleName();
                errorTypes.computeIfAbsent(errorType, unused -> new AtomicInteger(0)).incrementAndGet();
                
                // Don't fail the test immediately - collect all errors
                System.err.println("Thread " + threadId + " query " + i + " failed: " + e.getMessage());
            }
        }
    }

    private void updateMinMax(long responseTime, AtomicLong maxResponseTime, AtomicLong minResponseTime) {
        // Update max
        long currentMax = maxResponseTime.get();
        while (responseTime > currentMax && !maxResponseTime.compareAndSet(currentMax, responseTime)) {
            currentMax = maxResponseTime.get();
        }
        
        // Update min
        long currentMin = minResponseTime.get();
        while (responseTime < currentMin && !minResponseTime.compareAndSet(currentMin, responseTime)) {
            currentMin = minResponseTime.get();
        }
    }

    @Test
    void stressTest_LargeQueryVolume() throws InterruptedException {
        System.out.println("🔥 Starting large query volume stress test...");
        
        int LARGE_QUERY_COUNT = 500;
        AtomicInteger completed = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();
        
        ExecutorService executor = Executors.newFixedThreadPool(10);
        
        for (int i = 0; i < LARGE_QUERY_COUNT; i++) {
            final int queryId = i;
            executor.submit(() -> {
                try {
                    String query = testQueries.get(queryId % testQueries.size());
                    HybridSearchService.HybridSearchResult result = 
                        hybridSearchService.performHybridSearch(query, 10);
                    
                    assertThat(result).isNotNull();
                    completed.incrementAndGet();
                    
                } catch (Exception e) {
                    errors.incrementAndGet();
                    System.err.println("Query " + queryId + " failed: " + e.getMessage());
                }
            });
        }
        
        executor.shutdown();
        boolean finished = executor.awaitTermination(120, TimeUnit.SECONDS);
        long totalTime = System.currentTimeMillis() - startTime;
        
        System.out.println("📊 Large Volume Test Results:");
        System.out.println("   🎯 Target queries: " + LARGE_QUERY_COUNT);
        System.out.println("   ✅ Completed: " + completed.get());
        System.out.println("   ❌ Errors: " + errors.get());
        System.out.println("   ⏰ Total time: " + totalTime + "ms");
        System.out.println("   🚀 Average rate: " + String.format("%.2f queries/sec", 
                           (double) completed.get() / (totalTime / 1000.0)));
        
        assertThat(finished).isTrue();
        assertThat(completed.get()).isGreaterThan((int)(LARGE_QUERY_COUNT * 0.9)); // 90% completion
    }

    @Test
    void stressTest_VariousQueryPatterns() {
        System.out.println("🎭 Testing various query patterns...");
        
        List<String> patterns = Arrays.asList(
            "", // Empty query
            "a", // Single character
            "very long query string with many words that might cause issues",
            "special!@#$%^&*()characters",
            "unicode characters: éñüñ 中文 русский",
            "numbers 123 456 789",
            "mixed CASE and lowercase",
            "query with\nnewlines\tand\ttabs",
            "query    with    multiple    spaces",
            "CamelCaseQuery",
            "snake_case_query",
            "kebab-case-query",
            "query.with.dots",
            "query/with/slashes",
            "query\\with\\backslashes"
        );
        
        int successCount = 0;
        int totalTests = patterns.size();
        
        for (String pattern : patterns) {
            try {
                HybridSearchService.HybridSearchResult result = 
                    hybridSearchService.performHybridSearch(pattern, 5);
                
                assertThat(result).isNotNull();
                successCount++;
                
            } catch (Exception e) {
                System.err.println("Pattern '" + pattern + "' failed: " + e.getMessage());
            }
        }
        
        double successRate = (double) successCount / totalTests;
        System.out.println("🎯 Pattern test success rate: " + String.format("%.2f%%", successRate * 100));
        
        // Should handle at least 80% of patterns gracefully
        assertThat(successRate).isGreaterThanOrEqualTo(0.8);
    }

    @Test
    void stressTest_MemoryUsage() throws InterruptedException {
        System.out.println("💾 Testing memory usage under load...");
        
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Perform many searches to test memory usage
        ExecutorService executor = Executors.newFixedThreadPool(5);
        int MEMORY_TEST_QUERIES = 200;
        CountDownLatch latch = new CountDownLatch(MEMORY_TEST_QUERIES);
        
        for (int i = 0; i < MEMORY_TEST_QUERIES; i++) {
            final int queryId = i;
            executor.submit(() -> {
                try {
                    String query = testQueries.get(queryId % testQueries.size());
                    hybridSearchService.performHybridSearch(query, 15);
                    
                    // Hold references briefly to test memory management
                    Thread.sleep(10);
                    
                } catch (Exception e) {
                    // Ignore errors for memory test
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        
        // Force garbage collection and measure memory
        System.gc();
        Thread.sleep(1000);
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;
        
        System.out.println("📊 Memory Usage Results:");
        System.out.println("   🔄 Initial memory: " + formatBytes(initialMemory));
        System.out.println("   📈 Final memory: " + formatBytes(finalMemory));
        System.out.println("   ➕ Memory increase: " + formatBytes(memoryIncrease));
        
        // Memory increase should be reasonable (less than 100MB for this test)
        assertThat(memoryIncrease).isLessThan(100 * 1024 * 1024);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }
}
