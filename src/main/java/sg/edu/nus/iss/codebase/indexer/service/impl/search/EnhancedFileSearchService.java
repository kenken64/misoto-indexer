package sg.edu.nus.iss.codebase.indexer.service.impl.search;

import org.springframework.stereotype.Service;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Enhanced file search service with TF-IDF scoring, fuzzy matching, and advanced ranking
 */
@Service
public class EnhancedFileSearchService {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
        ".java", ".xml", ".properties", ".yml", ".yaml", ".json", ".md", ".txt", 
        ".py", ".js", ".ts", ".go", ".rs", ".cpp", ".c", ".h", ".kt", ".scala"
    );
    
    private static final Set<String> STOP_WORDS = Set.of(
        "the", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by",
        "from", "is", "are", "was", "were", "be", "been", "have", "has", "had", "do",
        "does", "did", "will", "would", "could", "should", "may", "might", "can",
        "this", "that", "these", "those", "i", "you", "he", "she", "it", "we", "they"
    );
    
    private String searchDirectory = ".";
    private Map<String, Double> documentFrequencies = new HashMap<>();
    private int totalDocuments = 0;

    public void setSearchDirectory(String directory) {
        this.searchDirectory = directory;
        buildDocumentFrequencies(); // Pre-compute IDF values
    }

    /**
     * Enhanced search with TF-IDF scoring and fuzzy matching
     */
    public List<SearchResult> searchInFiles(String query) {
        List<SearchResult> results = new ArrayList<>();
        
        try {
            System.out.println("🔍 Performing enhanced file-based search...");
            
            // Process query
            QueryAnalysis queryAnalysis = analyzeQuery(query);
            
            // Scan all supported files
            try (Stream<Path> paths = Files.walk(Paths.get(searchDirectory))) {
                paths.filter(Files::isRegularFile)
                     .filter(this::isSupportedFile)
                     .filter(this::isNotInExcludedDirectory)
                     .forEach(path -> {
                         try {
                             SearchResult result = searchInFileAdvanced(path.toFile(), queryAnalysis);
                             if (result != null && result.getRelevanceScore() > 0.1) {
                                 results.add(result);
                             }
                         } catch (Exception e) {
                             // Skip files that can't be read
                         }
                     });
            }
            
            // Sort by relevance score (descending)
            results.sort((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()));
            
            // Apply diversity filter to avoid too many results from same file type
            List<SearchResult> diverseResults = applyDiversityFilter(results);
            
            return diverseResults.stream().limit(25).collect(Collectors.toList());
            
        } catch (Exception e) {
            System.err.println("❌ Error in enhanced file search: " + e.getMessage());
            return List.of();
        }
    }

    private SearchResult searchInFileAdvanced(File file, QueryAnalysis queryAnalysis) {
        try {
            if (file.length() > 2 * 1024 * 1024) { // Skip files larger than 2MB
                return null;
            }
            
            String content = Files.readString(file.toPath());
            String[] lines = content.split("\n");
            
            // Calculate TF-IDF score
            double tfIdfScore = calculateTfIdfScore(content, queryAnalysis.terms);
            
            // Calculate positional score (terms appearing early get higher score)
            double positionalScore = calculatePositionalScore(content, queryAnalysis.terms);
            
            // Calculate semantic score (method names, class names, etc.)
            double semanticScore = calculateSemanticScore(content, queryAnalysis.originalQuery, file);
            
            // Calculate fuzzy match score
            double fuzzyScore = calculateFuzzyMatchScore(content, queryAnalysis.originalQuery);
            
            // Calculate file type importance
            double fileTypeScore = getFileTypeScore(file);
            
            // Combine scores with weights
            double finalScore = 
                (tfIdfScore * 0.4) + 
                (positionalScore * 0.2) + 
                (semanticScore * 0.25) + 
                (fuzzyScore * 0.1) + 
                (fileTypeScore * 0.05);
            
            if (finalScore > 0.1) {
                List<LineMatch> lineMatches = findAdvancedLineMatches(lines, queryAnalysis);
                String snippet = extractBestSnippet(content, queryAnalysis);
                
                return new SearchResult(
                    file.getName(),
                    file.getAbsolutePath(),
                    snippet,
                    finalScore,
                    "enhanced-file-search",
                    lineMatches
                );
            }
            
        } catch (Exception e) {
            // Skip files that can't be processed
        }
        
        return null;
    }

    private QueryAnalysis analyzeQuery(String query) {
        QueryAnalysis analysis = new QueryAnalysis();
        analysis.originalQuery = query;
        
        // Extract terms
        analysis.terms = Arrays.stream(query.toLowerCase().split("[\\s,;.!?()\\[\\]{}\"']+"))
                              .filter(term -> term.length() > 2)
                              .filter(term -> !STOP_WORDS.contains(term))
                              .distinct()
                              .collect(Collectors.toList());
        
        // Detect query patterns
        analysis.isMethodQuery = query.matches(".*\\w+\\s*\\(.*\\).*");
        analysis.isClassQuery = query.matches(".*\\b[A-Z]\\w*\\b.*");
        analysis.isAnnotationQuery = query.contains("@");
        analysis.isImportQuery = query.toLowerCase().contains("import");
        
        return analysis;
    }

    private double calculateTfIdfScore(String content, List<String> terms) {
        String contentLower = content.toLowerCase();
        double score = 0.0;
        
        for (String term : terms) {
            // Calculate TF (Term Frequency)
            long termCount = contentLower.split(Pattern.quote(term), -1).length - 1;
            double tf = termCount / (double) contentLower.split("\\s+").length;
            
            // Calculate IDF (Inverse Document Frequency)
            double idf = calculateIdf(term);
            
            // TF-IDF score
            score += tf * idf;
        }
        
        return score;
    }

    private double calculateIdf(String term) {
        double docFreq = documentFrequencies.getOrDefault(term, 1.0);
        return Math.log((double) totalDocuments / docFreq);
    }

    private double calculatePositionalScore(String content, List<String> terms) {
        String contentLower = content.toLowerCase();
        double score = 0.0;
        
        for (String term : terms) {
            int firstOccurrence = contentLower.indexOf(term);
            if (firstOccurrence != -1) {
                // Higher score for terms appearing earlier
                double position = firstOccurrence / (double) contentLower.length();
                score += 1.0 - (position * 0.5); // Max penalty is 50%
            }
        }
        
        return score / terms.size();
    }

    private double calculateSemanticScore(String content, String query, File file) {
        double score = 0.0;
        String contentLower = content.toLowerCase();
        String queryLower = query.toLowerCase();
        
        // Method declarations
        if (contentLower.matches(".*\\b(public|private|protected)\\s+.*\\s+" + 
                                Pattern.quote(queryLower) + "\\s*\\(.*")) {
            score += 2.0;
        }
        
        // Class/Interface declarations
        if (contentLower.matches(".*\\b(class|interface|enum)\\s+.*" + 
                                Pattern.quote(queryLower) + ".*")) {
            score += 1.8;
        }
        
        // Annotations
        if (contentLower.contains("@" + queryLower)) {
            score += 1.5;
        }
        
        // Comments (lower weight but still relevant)
        if (contentLower.matches(".*//.*" + Pattern.quote(queryLower) + ".*") ||
            contentLower.matches(".*/\\*.*" + Pattern.quote(queryLower) + ".*\\*/.*")) {
            score += 0.8;
        }
        
        // Variable/field declarations
        if (contentLower.matches(".*\\b(private|public|protected|static|final)\\s+\\w+\\s+" + 
                                Pattern.quote(queryLower) + "\\b.*")) {
            score += 1.2;
        }
        
        return score;
    }

    private double calculateFuzzyMatchScore(String content, String query) {
        String contentLower = content.toLowerCase();
        String queryLower = query.toLowerCase();
        
        // Simple fuzzy matching using edit distance
        double bestMatch = 0.0;
        String[] words = contentLower.split("\\s+");
        
        for (String word : words) {
            if (Math.abs(word.length() - queryLower.length()) <= 2) {
                double similarity = calculateStringSimilarity(word, queryLower);
                if (similarity > bestMatch) {
                    bestMatch = similarity;
                }
            }
        }
        
        return bestMatch;
    }

    private double calculateStringSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;
        
        return (maxLen - editDistance(s1, s2)) / (double) maxLen;
    }

    private int editDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                        dp[i-1][j-1] + (s1.charAt(i-1) == s2.charAt(j-1) ? 0 : 1),
                        Math.min(dp[i-1][j] + 1, dp[i][j-1] + 1)
                    );
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }

    private double getFileTypeScore(File file) {
        String fileName = file.getName().toLowerCase();
        
        // Higher scores for important file types
        if (fileName.contains("controller")) return 1.0;
        if (fileName.contains("service")) return 0.9;
        if (fileName.contains("repository") || fileName.contains("dao")) return 0.8;
        if (fileName.contains("config")) return 0.7;
        if (fileName.contains("test")) return 0.3; // Lower priority for test files
        if (fileName.endsWith(".java")) return 0.6;
        if (fileName.endsWith(".py")) return 0.6;
        if (fileName.endsWith(".xml") || fileName.endsWith(".properties")) return 0.4;
        
        return 0.5;
    }

    private List<LineMatch> findAdvancedLineMatches(String[] lines, QueryAnalysis queryAnalysis) {
        List<LineMatch> matches = new ArrayList<>();
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String lineLower = line.toLowerCase();
            
            // Check for exact matches
            for (String term : queryAnalysis.terms) {
                if (lineLower.contains(term)) {
                    matches.add(new LineMatch(i + 1, line.trim(), term, calculateLineRelevance(line, queryAnalysis)));
                }
            }
            
            // Check for fuzzy matches
            for (String term : queryAnalysis.terms) {
                String[] words = lineLower.split("\\s+");
                for (String word : words) {
                    if (calculateStringSimilarity(word, term) > 0.8) {
                        matches.add(new LineMatch(i + 1, line.trim(), word, 0.8));
                    }
                }
            }
        }
        
        return matches.stream()
                     .sorted((a, b) -> Double.compare(b.getRelevance(), a.getRelevance()))
                     .limit(15)
                     .collect(Collectors.toList());
    }

    private double calculateLineRelevance(String line, QueryAnalysis queryAnalysis) {
        double relevance = 1.0;
        String lineLower = line.toLowerCase();
        
        // Higher relevance for method signatures
        if (lineLower.contains("public") || lineLower.contains("private") || 
            lineLower.contains("protected")) {
            relevance += 0.5;
        }
        
        // Higher relevance for class declarations
        if (lineLower.contains("class") || lineLower.contains("interface")) {
            relevance += 0.4;
        }
        
        // Lower relevance for comments
        if (lineLower.trim().startsWith("//") || lineLower.trim().startsWith("*")) {
            relevance *= 0.7;
        }
        
        return relevance;
    }

    private String extractBestSnippet(String content, QueryAnalysis queryAnalysis) {
        String[] lines = content.split("\n");
        int bestLine = 0;
        double bestScore = 0.0;
        
        for (int i = 0; i < lines.length; i++) {
            double score = calculateLineRelevance(lines[i], queryAnalysis);
            if (score > bestScore) {
                bestScore = score;
                bestLine = i;
            }
        }
        
        // Extract context around best line
        int start = Math.max(0, bestLine - 3);
        int end = Math.min(lines.length, bestLine + 4);
        
        StringBuilder snippet = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (i == bestLine) {
                snippet.append(">>> ").append(lines[i].trim()).append("\n");
            } else {
                snippet.append("    ").append(lines[i].trim()).append("\n");
            }
        }
        
        return snippet.toString().trim();
    }

    private List<SearchResult> applyDiversityFilter(List<SearchResult> results) {
        Map<String, Integer> extensionCounts = new HashMap<>();
        List<SearchResult> diverseResults = new ArrayList<>();
        
        for (SearchResult result : results) {
            String extension = getFileExtension(result.getFileName());
            int count = extensionCounts.getOrDefault(extension, 0);
            
            // Limit results per file extension to promote diversity
            if (count < 8) {
                diverseResults.add(result);
                extensionCounts.put(extension, count + 1);
            }
        }
        
        return diverseResults;
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot) : "";
    }

    private void buildDocumentFrequencies() {
        documentFrequencies.clear();
        totalDocuments = 0;
        
        try (Stream<Path> paths = Files.walk(Paths.get(searchDirectory))) {
            paths.filter(Files::isRegularFile)
                 .filter(this::isSupportedFile)
                 .filter(this::isNotInExcludedDirectory)
                 .forEach(path -> {
                     try {
                         String content = Files.readString(path);
                         Set<String> uniqueTerms = extractUniqueTerms(content);
                         uniqueTerms.forEach(term -> 
                             documentFrequencies.merge(term, 1.0, Double::sum));
                         totalDocuments++;
                     } catch (Exception e) {
                         // Skip files that can't be read
                     }
                 });
        } catch (Exception e) {
            System.err.println("Warning: Could not build document frequencies: " + e.getMessage());
        }
    }

    private Set<String> extractUniqueTerms(String content) {
        return Arrays.stream(content.toLowerCase().split("[\\s,;.!?()\\[\\]{}\"']+"))
                    .filter(term -> term.length() > 2)
                    .filter(term -> !STOP_WORDS.contains(term))
                    .collect(Collectors.toSet());
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
               !pathStr.contains(".vscode") &&
               !pathStr.contains("codebase\\spring-ai") &&
               !pathStr.contains("codebase/spring-ai");
    }

    // Inner classes
    private static class QueryAnalysis {
        String originalQuery;
        List<String> terms;
        boolean isMethodQuery;
        boolean isClassQuery;
        boolean isAnnotationQuery;
        boolean isImportQuery;
    }

    public static class SearchResult {
        private final String fileName;
        private final String filePath;
        private final String content;
        private final double relevanceScore;
        private final String searchType;
        private final List<LineMatch> lineMatches;

        public SearchResult(String fileName, String filePath, String content, 
                          double relevanceScore, String searchType, List<LineMatch> lineMatches) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.content = content;
            this.relevanceScore = relevanceScore;
            this.searchType = searchType;
            this.lineMatches = lineMatches != null ? lineMatches : new ArrayList<>();
        }

        public String getFileName() { return fileName; }
        public String getFilePath() { return filePath; }
        public String getContent() { return content; }
        public double getRelevanceScore() { return relevanceScore; }
        public String getSearchType() { return searchType; }
        public List<LineMatch> getLineMatches() { return lineMatches; }
    }

    public static class LineMatch {
        private final int lineNumber;
        private final String lineContent;
        private final String matchedTerm;
        private final double relevance;

        public LineMatch(int lineNumber, String lineContent, String matchedTerm, double relevance) {
            this.lineNumber = lineNumber;
            this.lineContent = lineContent;
            this.matchedTerm = matchedTerm;
            this.relevance = relevance;
        }

        public int getLineNumber() { return lineNumber; }
        public String getLineContent() { return lineContent; }
        public String getMatchedTerm() { return matchedTerm; }
        public double getRelevance() { return relevance; }
    }
}
