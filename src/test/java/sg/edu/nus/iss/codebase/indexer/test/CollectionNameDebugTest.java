package sg.edu.nus.iss.codebase.indexer.test;

import sg.edu.nus.iss.codebase.indexer.config.IndexingConfiguration;

/**
 * Test collection name generation for the specific dssi-day3-ollama directory
 */
public class CollectionNameDebugTest {
    
    public static void main(String[] args) {
        System.out.println("Testing Collection Name Generation for dssi-day3-ollama");
        System.out.println("======================================================");
        
        // Test the specific problematic directory
        String[] testDirectories = {
            "d:\\Projects\\misoto-indexer\\codebase\\dssi-day3-ollama",
            "D:\\Projects\\misoto-indexer\\codebase\\dssi-day3-ollama",
            "d:/Projects/misoto-indexer/codebase/dssi-day3-ollama",
            "./codebase/dssi-day3-ollama",
            "codebase/dssi-day3-ollama"
        };
        
        for (String directory : testDirectories) {
            String collectionName = generateCollectionName(directory);
            String cacheFileName = new IndexingConfiguration().getCache().generateCacheFileName(directory);
            
            System.out.printf("Directory: %s%n", directory);
            System.out.printf("Collection: %s%n", collectionName);
            System.out.printf("Cache File: %s%n", cacheFileName);
            System.out.println("Expected Collection: codebase-index-dssi-day3-ollama");
            System.out.println("Match: " + (collectionName.equals("codebase-index-dssi-day3-ollama") ? "✅ YES" : "❌ NO"));
            System.out.println("-----------------------------------------------------");
        }
    }
    
    /**
     * Replicate the exact collection name generation logic from FileIndexingServiceImpl
     */
    private static String generateCollectionName(String directory) {
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
}
