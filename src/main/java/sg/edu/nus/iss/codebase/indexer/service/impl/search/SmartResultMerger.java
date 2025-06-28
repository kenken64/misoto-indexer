package sg.edu.nus.iss.codebase.indexer.service.impl.search;

import org.springframework.stereotype.Component;
import sg.edu.nus.iss.codebase.indexer.service.HybridSearchService;
import sg.edu.nus.iss.codebase.indexer.service.FileSearchService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Smart result merging and re-ranking strategy that combines vector and file search results
 * with sophisticated scoring algorithms
 */
@Component
public class SmartResultMerger {
    
    private static final double VECTOR_WEIGHT = 0.6;
    private static final double FILE_WEIGHT = 0.4;
    private static final double DIVERSITY_BONUS = 0.1;
    private static final double FRESHNESS_WEIGHT = 0.05;

    /**
     * Merge and re-rank results from different search strategies
     */
    public List<HybridSearchService.SearchResult> mergeAndRank(
            String query,
            List<HybridSearchService.SearchResult> vectorResults,
            List<FileSearchService.SearchResult> fileResults) {
        
        Map<String, CombinedResult> combinedResults = new HashMap<>();
        
        // Process vector search results
        processVectorResults(vectorResults, combinedResults, query);
        
        // Process file search results  
        processFileResults(fileResults, combinedResults, query);
        
        // Calculate final scores and create merged results
        List<HybridSearchService.SearchResult> mergedResults = 
            combinedResults.values().stream()
                .map(this::createFinalResult)
                .sorted((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()))
                .collect(Collectors.toList());
        
        // Apply diversity re-ranking
        return applyDiversityReranking(mergedResults);
    }

    private void processVectorResults(List<HybridSearchService.SearchResult> vectorResults, 
                                    Map<String, CombinedResult> combinedResults, String query) {
        for (int i = 0; i < vectorResults.size(); i++) {
            HybridSearchService.SearchResult result = vectorResults.get(i);
            String key = normalizeFilePath(result.getFilePath());
            
            CombinedResult combined = combinedResults.computeIfAbsent(key, 
                k -> new CombinedResult(result.getFileName(), result.getFilePath()));
            
            // Position-based scoring (earlier results get higher scores)
            double positionScore = 1.0 - (i * 0.1);
            double vectorScore = result.getRelevanceScore() * positionScore;
            
            combined.vectorScore = vectorScore;
            combined.vectorResult = result;
            combined.hasVectorMatch = true;
            
            // Calculate query-content alignment
            combined.queryAlignment = calculateQueryAlignment(result.getContent(), query);
        }
    }

    private void processFileResults(List<FileSearchService.SearchResult> fileResults,
                                  Map<String, CombinedResult> combinedResults, String query) {
        for (int i = 0; i < fileResults.size(); i++) {
            FileSearchService.SearchResult result = fileResults.get(i);
            String key = normalizeFilePath(result.getFilePath());
            
            CombinedResult combined = combinedResults.computeIfAbsent(key,
                k -> new CombinedResult(result.getFileName(), result.getFilePath()));
            
            // Position-based scoring
            double positionScore = 1.0 - (i * 0.05); // Less penalty for file results
            double fileScore = result.getRelevanceScore() * positionScore;
            
            combined.fileScore = fileScore;
            combined.fileResult = result;
            combined.hasFileMatch = true;
            
            // Update query alignment if not set
            if (combined.queryAlignment == 0.0) {
                combined.queryAlignment = calculateQueryAlignment(result.getContent(), query);
            }
        }
    }

    private HybridSearchService.SearchResult createFinalResult(CombinedResult combined) {
        // Calculate weighted final score
        double finalScore = 0.0;
        
        if (combined.hasVectorMatch && combined.hasFileMatch) {
            // Both matches - combine scores with confidence boost
            finalScore = (combined.vectorScore * VECTOR_WEIGHT) + 
                        (combined.fileScore * FILE_WEIGHT) + 
                        0.2; // Confidence boost for dual matches
        } else if (combined.hasVectorMatch) {
            finalScore = combined.vectorScore * 0.8; // Slight penalty for single match
        } else if (combined.hasFileMatch) {
            finalScore = combined.fileScore * 0.9; // Less penalty for file-only matches
        }
        
        // Apply query alignment bonus
        finalScore += combined.queryAlignment * 0.15;
        
        // Apply file type importance
        finalScore += getFileTypeImportance(combined.fileName) * 0.1;
        
        // Create result from the best available source
        HybridSearchService.SearchResult baseResult = combined.vectorResult != null ? 
            combined.vectorResult : convertFileToSearchResult(combined.fileResult);
        
        return new HybridSearchService.SearchResult(
            baseResult.getFileName(),
            baseResult.getFilePath(),
            baseResult.getContent(),
            Math.min(1.0, finalScore), // Cap at 1.0
            determineSearchType(combined),
            baseResult.getMetadata(),
            baseResult.getLineMatches()
        );
    }

    private double calculateQueryAlignment(String content, String query) {
        if (content == null || query == null) return 0.0;
        
        String contentLower = content.toLowerCase();
        String queryLower = query.toLowerCase();
        String[] queryTerms = queryLower.split("\\s+");
        
        double alignment = 0.0;
        
        // Check for exact phrase matches
        if (contentLower.contains(queryLower)) {
            alignment += 0.5;
        }
        
        // Check for individual term matches with proximity scoring
        List<Integer> termPositions = new ArrayList<>();
        for (String term : queryTerms) {
            int pos = contentLower.indexOf(term);
            if (pos != -1) {
                termPositions.add(pos);
                alignment += 0.1;
            }
        }
        
        // Proximity bonus - terms appearing close together get higher scores
        if (termPositions.size() > 1) {
            Collections.sort(termPositions);
            double avgDistance = 0.0;
            for (int i = 1; i < termPositions.size(); i++) {
                avgDistance += termPositions.get(i) - termPositions.get(i-1);
            }
            avgDistance /= (termPositions.size() - 1);
            
            // Closer terms get higher bonus
            double proximityBonus = Math.max(0.0, 0.3 - (avgDistance / 1000.0));
            alignment += proximityBonus;
        }
        
        return Math.min(1.0, alignment);
    }

    private double getFileTypeImportance(String fileName) {
        String name = fileName.toLowerCase();
        
        if (name.contains("controller")) return 1.0;
        if (name.contains("service")) return 0.9;
        if (name.contains("repository") || name.contains("dao")) return 0.8;
        if (name.contains("config")) return 0.7;
        if (name.contains("util")) return 0.6;
        if (name.contains("test")) return 0.3;
        if (name.endsWith(".java") || name.endsWith(".py")) return 0.5;
        if (name.endsWith(".xml") || name.endsWith(".properties")) return 0.4;
        
        return 0.5;
    }

    private String determineSearchType(CombinedResult combined) {
        if (combined.hasVectorMatch && combined.hasFileMatch) {
            return "hybrid-enhanced";
        } else if (combined.hasVectorMatch) {
            return "vector-only";
        } else {
            return "file-only";
        }
    }

    private HybridSearchService.SearchResult convertFileToSearchResult(
            FileSearchService.SearchResult fileResult) {
        return new HybridSearchService.SearchResult(
            fileResult.getFileName(),
            fileResult.getFilePath(),
            fileResult.getContent(),
            fileResult.getRelevanceScore(),
            fileResult.getSearchType(),
            new HashMap<>(), // Empty metadata
            fileResult.getLineMatches()
        );
    }

    private List<HybridSearchService.SearchResult> applyDiversityReranking(
            List<HybridSearchService.SearchResult> results) {
        
        if (results.size() <= 10) return results; // No need for diversity with small result sets
        
        List<HybridSearchService.SearchResult> diverseResults = new ArrayList<>();
        Set<String> seenFileTypes = new HashSet<>();
        Set<String> seenDirectories = new HashSet<>();
        
        // First pass - add top results ensuring diversity
        for (HybridSearchService.SearchResult result : results) {
            String fileType = getFileType(result.getFileName());
            String directory = getDirectory(result.getFilePath());
            
            boolean shouldAdd = diverseResults.size() < 5 || // Always add top 5
                               !seenFileTypes.contains(fileType) ||
                               !seenDirectories.contains(directory);
            
            if (shouldAdd) {
                diverseResults.add(result);
                seenFileTypes.add(fileType);
                seenDirectories.add(directory);
                
                if (diverseResults.size() >= 20) break; // Limit diversity-enhanced results
            }
        }
        
        // Second pass - fill remaining slots with best remaining results
        for (HybridSearchService.SearchResult result : results) {
            if (!diverseResults.contains(result) && diverseResults.size() < 25) {
                diverseResults.add(result);
            }
        }
        
        return diverseResults;
    }

    private String getFileType(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot) : "unknown";
    }

    private String getDirectory(String filePath) {
        int lastSlash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        if (lastSlash > 0) {
            String dir = filePath.substring(0, lastSlash);
            // Get parent directory for better grouping
            int secondLastSlash = Math.max(dir.lastIndexOf('/'), dir.lastIndexOf('\\'));
            return secondLastSlash > 0 ? dir.substring(secondLastSlash + 1) : dir;
        }
        return "root";
    }

    private String normalizeFilePath(String filePath) {
        return filePath.replace("\\", "/").toLowerCase();
    }

    /**
     * Internal class to hold combined result data during processing
     */
    private static class CombinedResult {
        final String fileName;
        final String filePath;
        double vectorScore = 0.0;
        double fileScore = 0.0;
        double queryAlignment = 0.0;
        boolean hasVectorMatch = false;
        boolean hasFileMatch = false;
        HybridSearchService.SearchResult vectorResult;
        FileSearchService.SearchResult fileResult;

        CombinedResult(String fileName, String filePath) {
            this.fileName = fileName;
            this.filePath = filePath;
        }
    }
}
