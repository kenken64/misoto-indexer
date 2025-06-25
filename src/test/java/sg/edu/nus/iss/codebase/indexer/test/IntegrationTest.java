package sg.edu.nus.iss.codebase.indexer.test;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import sg.edu.nus.iss.codebase.indexer.IndexerApplication;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileIndexingService;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Integration test to verify dynamic cache file functionality
 */
public class IntegrationTest {
    
    public static void main(String[] args) {
        System.out.println("Starting Integration Test for Dynamic Cache Files");
        System.out.println("=================================================");
        
        try {
            // Start Spring Boot application
            System.setProperty("spring.profiles.active", "test");
            ConfigurableApplicationContext context = SpringApplication.run(IndexerApplication.class, args);
            
            // Get the FileIndexingService bean
            FileIndexingService fileIndexingService = context.getBean(FileIndexingService.class);
            
            // Test 1: spring-ai directory
            System.out.println("\n[TEST 1] Testing spring-ai directory indexing");
            String springAiDir = "d:\\Projects\\misoto-indexer\\codebase\\spring-ai";
            fileIndexingService.setIndexingDirectoryWithCollection(springAiDir);
            
            // Wait a moment for processing
            Thread.sleep(3000);
            
            // Check if cache file was created
            String expectedCacheFile = ".indexed_spring-ai_files_cache.txt";
            if (Files.exists(Paths.get(expectedCacheFile))) {
                System.out.println("✅ Cache file created: " + expectedCacheFile);
            } else {
                System.out.println("❌ Cache file NOT found: " + expectedCacheFile);
            }
            
            // Test 2: ollama directory
            System.out.println("\n[TEST 2] Testing ollama directory indexing");
            String ollamaDir = "d:\\Projects\\misoto-indexer\\codebase\\ollama";
            fileIndexingService.setIndexingDirectoryWithCollection(ollamaDir);
            
            // Wait a moment for processing
            Thread.sleep(3000);
            
            // Check if cache file was created
            String expectedCacheFile2 = ".indexed_ollama_files_cache.txt";
            if (Files.exists(Paths.get(expectedCacheFile2))) {
                System.out.println("✅ Cache file created: " + expectedCacheFile2);
            } else {
                System.out.println("❌ Cache file NOT found: " + expectedCacheFile2);
            }
            
            System.out.println("\n[SUMMARY] Integration test completed");
            
            // Close the application context
            context.close();
            
        } catch (Exception e) {
            System.err.println("Integration test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
