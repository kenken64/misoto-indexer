package sg.edu.nus.iss.codebase.indexer.test;

import sg.edu.nus.iss.codebase.indexer.config.IndexingConfiguration;

/**
 * Simple test to verify cache file naming functionality
 */
public class CacheFileNameTest {
    
    public static void main(String[] args) {
        IndexingConfiguration config = new IndexingConfiguration();
        IndexingConfiguration.CacheConfig cacheConfig = config.getCache();
        
        // Test various directory scenarios
        String[] testDirectories = {
            "d:\\Projects\\misoto-indexer\\codebase\\spring-ai",
            "d:\\Projects\\misoto-indexer\\codebase\\ollama", 
            "d:\\Projects\\misoto-indexer\\src",
            "/home/user/project",
            "C:\\Users\\dev\\projects\\my-app"
        };
        
        System.out.println("Testing Cache File Name Generation:");
        System.out.println("=====================================");
        
        for (String directory : testDirectories) {
            String cacheFileName = cacheConfig.generateCacheFileName(directory);
            System.out.printf("Directory: %s%n", directory);
            System.out.printf("Cache File: %s%n", cacheFileName);
            System.out.println("-------------------------------------");
        }
    }
}
