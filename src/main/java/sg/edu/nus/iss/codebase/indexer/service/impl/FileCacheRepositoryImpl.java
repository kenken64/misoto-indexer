package sg.edu.nus.iss.codebase.indexer.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import sg.edu.nus.iss.codebase.indexer.config.IndexingConfiguration;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileCacheRepository;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of FileCacheRepository
 * Manages persistent file cache for indexing optimization
 */
@Repository
public class FileCacheRepositoryImpl implements FileCacheRepository {

    private final IndexingConfiguration config;
    private final Set<String> indexedFilePaths = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> fileModificationTimes = new ConcurrentHashMap<>();

    @Autowired
    public FileCacheRepositoryImpl(IndexingConfiguration config) {
        this.config = config;
    }

    @Override
    public boolean needsReindexing(File file) {
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

    @Override
    public void saveIndexedFile(String filePath) {
        try {
            Path file = Paths.get(filePath);
            long modTime = Files.getLastModifiedTime(file).toMillis();
            
            indexedFilePaths.add(filePath);
            fileModificationTimes.put(filePath, modTime);
            
            // Persist to cache file
            if (config.getCache().isEnabled()) {
                String cacheEntry = "INDEXED:" + filePath + "|" + modTime + System.lineSeparator();
                Files.writeString(Paths.get(config.getCache().getCacheFileName()), 
                    cacheEntry, 
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        } catch (Exception e) {
            // Log error but don't fail the indexing process
            System.err.println("⚠️ Could not save file to cache: " + e.getMessage());
        }
    }

    @Override
    public Set<String> getIndexedFilePaths() {
        return new HashSet<>(indexedFilePaths);
    }

    @Override
    public void loadCache() {
        if (!config.getCache().isEnabled()) {
            return;
        }

        try {
            Path cacheFile = Paths.get(config.getCache().getCacheFileName());
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
                    removeDeletedFiles(deletedFiles);
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

    @Override
    public void clearCache() {
        indexedFilePaths.clear();
        fileModificationTimes.clear();
        
        if (config.getCache().isEnabled()) {
            try {
                Files.deleteIfExists(Paths.get(config.getCache().getCacheFileName()));
            } catch (Exception e) {
                System.err.println("⚠️ Could not clear cache file: " + e.getMessage());
            }
        }
    }

    @Override
    public void removeDeletedFiles(List<String> deletedFiles) {
        if (deletedFiles.isEmpty()) {
            return;
        }
        
        deletedFiles.forEach(filePath -> {
            indexedFilePaths.remove(filePath);
            fileModificationTimes.remove(filePath);
        });
        
        System.out.println("⚠️ " + deletedFiles.size() + " deleted files removed from cache");
        // TODO: Implement vector store cleanup for deleted files
    }

    @Override
    public int getCacheSize() {
        return indexedFilePaths.size();
    }

    /**
     * Rebuild the cache file with valid entries only
     */
    private void rebuildCacheFile(List<String> validEntries) {
        if (!config.getCache().isEnabled()) {
            return;
        }
        
        try {
            Path cacheFile = Paths.get(config.getCache().getCacheFileName());
            Files.deleteIfExists(cacheFile);
            if (!validEntries.isEmpty()) {
                Files.write(cacheFile, validEntries, StandardOpenOption.CREATE);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Could not rebuild cache file: " + e.getMessage());
        }
    }
}
