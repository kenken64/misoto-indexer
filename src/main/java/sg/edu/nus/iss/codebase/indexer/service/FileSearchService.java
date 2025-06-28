package sg.edu.nus.iss.codebase.indexer.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileSearchService {
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            ".java", ".xml", ".properties", ".yml", ".yaml", ".json", ".md", ".txt", ".py", ".js", ".ts", ".go", ".rs",
            ".cpp", ".c", ".h");

    // Configurable search directory
    private String searchDirectory = "."; // default to current directory

    /**
     * Set the directory to search in
     */
    public void setSearchDirectory(String directory) {
        this.searchDirectory = directory;
    }

    /**
     * Fallback search using direct file content scanning
     */
    public List<SearchResult> searchInFiles(String query) {
        List<SearchResult> results = new ArrayList<>();

        try {
            System.out.println("🔍 Performing file-based fallback search...");

            // Create search patterns
            List<String> searchTerms = extractSearchTerms(query);

            // Scan all supported files
            try (Stream<Path> paths = Files.walk(Paths.get(searchDirectory))) {
                paths.filter(Files::isRegularFile)
                        .filter(this::isSupportedFile)
                        .filter(this::isNotInExcludedDirectory)
                        .forEach(path -> {
                            try {
                                SearchResult result = searchInFile(path.toFile(), searchTerms, query);
                                if (result != null && result.getRelevanceScore() > 0) {
                                    results.add(result);
                                }
                            } catch (Exception e) {
                                // Skip files that can't be read
                            }
                        });
            }

            // Sort by relevance score
            results.sort((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()));

            // Limit results
            return results.stream().limit(20).toList();

        } catch (Exception e) {
            System.err.println("❌ Error in file-based search: " + e.getMessage());
            return List.of();
        }
    }

    private SearchResult searchInFile(File file, List<String> searchTerms, String originalQuery) {
        try {
            if (file.length() > 1024 * 1024) { // Skip files larger than 1MB
                return null;
            }

            String content = Files.readString(file.toPath());
            String[] lines = content.split("\n");

            double relevanceScore = 0.0;
            List<String> matchedSnippets = new ArrayList<>();
            List<LineMatch> lineMatches = new ArrayList<>();

            // Check for exact phrase match
            if (content.toLowerCase().contains(originalQuery.toLowerCase())) {
                relevanceScore += 10.0;
                matchedSnippets.addAll(extractSnippets(content, originalQuery));
                lineMatches.addAll(findLineMatches(lines, originalQuery));
            }

            // Check for individual terms
            for (String term : searchTerms) {
                String termLower = term.toLowerCase();
                long count = countOccurrencesInLines(lines, termLower);
                if (count > 0) {
                    relevanceScore += count * getTermWeight(term, file);
                    matchedSnippets.addAll(extractSnippets(content, term));
                    lineMatches.addAll(findLineMatches(lines, term));
                }
            }

            if (relevanceScore > 0) {
                // Remove duplicates and limit snippets
                List<String> uniqueSnippets = matchedSnippets.stream()
                        .distinct()
                        .limit(3)
                        .toList();
                // Remove duplicate line matches (by line number)
                List<LineMatch> uniqueLineMatches = lineMatches.stream()
                        .collect(Collectors.groupingBy(LineMatch::getLineNumber))
                        .values()
                        .stream()
                        .map(group -> group.get(0)) // Take first match for each line number
                        .sorted((a, b) -> Integer.compare(a.getLineNumber(), b.getLineNumber()))
                        .limit(10) // Limit to first 10 matches
                        .toList();

                return new SearchResult(
                        file.getName(),
                        file.getAbsolutePath(),
                        uniqueSnippets.isEmpty() ? "File contains matching content"
                                : String.join("\n\n", uniqueSnippets),
                        relevanceScore,
                        "file-search",
                        uniqueLineMatches);
            }

        } catch (Exception e) {
            // Skip files that can't be processed
        }

        return null;
    }

    private List<String> extractSearchTerms(String query) {
        // Simple tokenization - split on whitespace and common separators
        return Arrays.stream(query.toLowerCase().split("[\\s,;.!?()\\[\\]{}\"']+"))
                .filter(term -> term.length() > 2) // Skip very short terms
                .filter(term -> !isStopWord(term))
                .distinct()
                .toList();
    }

    private boolean isStopWord(String term) {
        Set<String> stopWords = Set.of(
                "the", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by",
                "from", "is", "are", "was", "were", "be", "been", "have", "has", "had", "do",
                "does", "did", "will", "would", "could", "should", "may", "might", "can",
                "this", "that", "these", "those", "i", "you", "he", "she", "it", "we", "they");
        return stopWords.contains(term);
    }

    private long countOccurrences(String text, String term) {
        if (term.isEmpty())
            return 0;

        long count = 0;
        int index = 0;
        while ((index = text.indexOf(term, index)) != -1) {
            count++;
            index += term.length();
        }
        return count;
    }

    private double getTermWeight(String term, File file) {
        String fileName = file.getName().toLowerCase();

        // Higher weight for matches in important file types
        if (fileName.endsWith("controller.java"))
            return 3.0;
        if (fileName.endsWith("service.java"))
            return 2.5;
        if (fileName.endsWith("repository.java"))
            return 2.0;
        if (fileName.endsWith(".java"))
            return 1.5;
        if (fileName.endsWith(".xml") || fileName.endsWith(".properties"))
            return 1.2;

        return 1.0;
    }

    private List<String> extractSnippets(String content, String term) {
        List<String> snippets = new ArrayList<>();
        String[] lines = content.split("\n");

        for (int i = 0; i < lines.length; i++) {
            if (lines[i].toLowerCase().contains(term.toLowerCase())) {
                // Extract context around the match
                int start = Math.max(0, i - 2);
                int end = Math.min(lines.length, i + 3);

                StringBuilder snippet = new StringBuilder();
                for (int j = start; j < end; j++) {
                    if (j == i) {
                        // Highlight the matching line
                        snippet.append(">>> ").append(lines[j].trim()).append("\n");
                    } else {
                        snippet.append("    ").append(lines[j].trim()).append("\n");
                    }
                }

                snippets.add(snippet.toString().trim());

                // Limit snippets per file
                if (snippets.size() >= 2)
                    break;
            }
        }

        return snippets;
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
                !pathStr.contains("codebase\\spring-ai") && // Exclude only the Spring AI source
                !pathStr.contains("codebase/spring-ai"); // Support both Windows and Unix paths
    }

    /**
     * Search result class for file-based search
     */
    public static class SearchResult {
        private final String fileName;
        private final String filePath;
        private final String content;
        private final double relevanceScore;
        private final String searchType;
        private final List<LineMatch> lineMatches;

        public SearchResult(String fileName, String filePath, String content, double relevanceScore,
                String searchType) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.content = content;
            this.relevanceScore = relevanceScore;
            this.searchType = searchType;
            this.lineMatches = new ArrayList<>();
        }

        public SearchResult(String fileName, String filePath, String content, double relevanceScore, String searchType,
                List<LineMatch> lineMatches) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.content = content;
            this.relevanceScore = relevanceScore;
            this.searchType = searchType;
            this.lineMatches = lineMatches != null ? lineMatches : new ArrayList<>();
        }

        public String getFileName() {
            return fileName;
        }

        public String getFilePath() {
            return filePath;
        }

        public String getContent() {
            return content;
        }

        public double getRelevanceScore() {
            return relevanceScore;
        }

        public String getSearchType() {
            return searchType;
        }

        public List<LineMatch> getLineMatches() {
            return lineMatches;
        }
    }

    /**
     * Represents a line match with line number and content
     */
    public static class LineMatch {
        private final int lineNumber;
        private final String lineContent;
        private final String matchedTerm;

        public LineMatch(int lineNumber, String lineContent, String matchedTerm) {
            this.lineNumber = lineNumber;
            this.lineContent = lineContent;
            this.matchedTerm = matchedTerm;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public String getLineContent() {
            return lineContent;
        }

        public String getMatchedTerm() {
            return matchedTerm;
        }
    }

    private List<LineMatch> findLineMatches(String[] lines, String term) {
        List<LineMatch> matches = new ArrayList<>();
        String termLower = term.toLowerCase();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.toLowerCase().contains(termLower)) {
                matches.add(new LineMatch(i + 1, line.trim(), term));
            }
        }

        return matches;
    }

    private long countOccurrencesInLines(String[] lines, String term) {
        long count = 0;
        for (String line : lines) {
            count += countOccurrences(line.toLowerCase(), term);
        }
        return count;
    }
}
