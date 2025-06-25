package sg.edu.nus.iss.codebase.indexer.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class QuickDebugTool {
    public static void main(String[] args) {
        System.out.println("=== QUICK DEBUG INFORMATION ===");
        
        // Check current working directory
        System.out.println("Current working directory: " + System.getProperty("user.dir"));
        
        // Check for cache files
        String[] possibleCacheFiles = {
            ".indexed_files_cache.txt",
            ".indexed_dssi-day3-ollama_files_cache.txt",
            ".indexed_codebase-dssi-day3-ollama_files_cache.txt"
        };
        
        System.out.println("\n=== CACHE FILES ===");
        for (String cacheFile : possibleCacheFiles) {
            Path cachePath = Paths.get(cacheFile);
            if (Files.exists(cachePath)) {
                System.out.println("✅ Found: " + cacheFile);
                try {
                    long size = Files.size(cachePath);
                    System.out.println("   Size: " + size + " bytes");
                    if (size > 0) {
                        // Read first few lines
                        System.out.println("   First lines:");
                        Files.lines(cachePath).limit(3).forEach(line -> 
                            System.out.println("   " + line));
                    }
                } catch (Exception e) {
                    System.out.println("   Error reading file: " + e.getMessage());
                }
            } else {
                System.out.println("❌ Not found: " + cacheFile);
            }
        }
        
        // Check for dssi-day3-ollama directory
        System.out.println("\n=== DIRECTORY CHECK ===");
        String[] possibleDirs = {
            "codebase/dssi-day3-ollama",
            "./codebase/dssi-day3-ollama",
            "D:\\Projects\\misoto-indexer\\codebase\\dssi-day3-ollama"
        };
        
        for (String dir : possibleDirs) {
            Path dirPath = Paths.get(dir);
            if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
                System.out.println("✅ Found directory: " + dir);
                System.out.println("   Absolute path: " + dirPath.toAbsolutePath());
                
                // Check for text_to_sql_train.py
                Path targetFile = dirPath.resolve("text_to_sql_train.py");
                if (Files.exists(targetFile)) {
                    System.out.println("   ✅ Found text_to_sql_train.py");
                    try {
                        long size = Files.size(targetFile);
                        System.out.println("   File size: " + size + " bytes");
                    } catch (Exception e) {
                        System.out.println("   Error reading file size: " + e.getMessage());
                    }
                } else {
                    System.out.println("   ❌ text_to_sql_train.py not found");
                }
            } else {
                System.out.println("❌ Directory not found: " + dir);
            }
        }
    }
}
