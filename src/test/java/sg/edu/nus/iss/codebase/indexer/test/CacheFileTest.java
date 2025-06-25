package sg.edu.nus.iss.codebase.indexer.test;

import sg.edu.nus.iss.codebase.indexer.config.IndexingConfiguration;
import sg.edu.nus.iss.codebase.indexer.service.impl.FileCacheRepositoryImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Direct test for cache file functionality
 */
public class CacheFileTest {
    
    public static void main(String[] args) {
        System.out.println("Testing Cache File Functionality");
        System.out.println("=================================");
        
        try {
            // Test setup
            IndexingConfiguration config = new IndexingConfiguration();
            FileCacheRepositoryImpl cacheRepository = new FileCacheRepositoryImpl(config);
            
            // Test for spring-ai directory
            String testDir = "d:\\Projects\\misoto-indexer\\codebase\\spring-ai";
            String expectedCacheFile = config.getCache().generateCacheFileName(testDir);
            
            System.out.println("Test Directory: " + testDir);
            System.out.println("Expected Cache File: " + expectedCacheFile);
            
            // Set the cache file name dynamically
            cacheRepository.setCacheFileName(expectedCacheFile);
            
            // Test saving a file to cache
            String testFilePath = "d:\\Projects\\misoto-indexer\\test-file.txt";
            
            // Create a temporary test file
            Path testFile = Paths.get(testFilePath);
            if (!Files.exists(testFile)) {
                Files.createFile(testFile);
            }
            
            System.out.println("Saving test file to cache...");
            cacheRepository.saveIndexedFile(testFilePath);
            
            // Check if cache file was created
            Path cacheFilePath = Paths.get(expectedCacheFile);
            if (Files.exists(cacheFilePath)) {
                System.out.println("✅ Cache file created successfully: " + expectedCacheFile);
                
                // Load cache and verify
                cacheRepository.loadCache();
                if (cacheRepository.getIndexedFilePaths().contains(testFilePath)) {
                    System.out.println("✅ File was successfully saved and loaded from cache");
                } else {
                    System.out.println("❌ File was not found in loaded cache");
                }
                
                // Print cache contents
                System.out.println("Cache contents:");
                String content = Files.readString(cacheFilePath);
                System.out.println(content);
                
            } else {
                System.out.println("❌ Cache file was not created");
            }
            
            // Test for ollama directory
            System.out.println("\n--- Testing ollama directory ---");
            String testDir2 = "d:\\Projects\\misoto-indexer\\codebase\\ollama";
            String expectedCacheFile2 = config.getCache().generateCacheFileName(testDir2);
            
            System.out.println("Test Directory: " + testDir2);
            System.out.println("Expected Cache File: " + expectedCacheFile2);
            
            // Set the cache file name dynamically for ollama
            cacheRepository.setCacheFileName(expectedCacheFile2);
            
            System.out.println("Saving test file to ollama cache...");
            cacheRepository.saveIndexedFile(testFilePath);
            
            // Check if ollama cache file was created
            Path cacheFilePath2 = Paths.get(expectedCacheFile2);
            if (Files.exists(cacheFilePath2)) {
                System.out.println("✅ Ollama cache file created successfully: " + expectedCacheFile2);
            } else {
                System.out.println("❌ Ollama cache file was not created");
            }
            
            // Cleanup
            Files.deleteIfExists(testFile);
            Files.deleteIfExists(cacheFilePath);
            Files.deleteIfExists(cacheFilePath2);
            
            System.out.println("\n✅ Cache file functionality test completed successfully!");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
