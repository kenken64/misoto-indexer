package sg.edu.nus.iss.codebase.indexer.service.impl.search;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Query analyzer that understands user intent and optimizes search strategies accordingly
 */
@Component
public class QueryIntelligenceService {
    
    private static final Pattern METHOD_PATTERN = Pattern.compile("\\w+\\s*\\([^)]*\\)");
    private static final Pattern CLASS_PATTERN = Pattern.compile("\\b[A-Z][a-zA-Z0-9]*\\b");
    private static final Pattern ANNOTATION_PATTERN = Pattern.compile("@\\w+");
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("\\b\\w+(\\.\\w+)+\\b");
    
    private static final Set<String> CODE_KEYWORDS = Set.of(
        "class", "interface", "method", "function", "variable", "field", "constructor",
        "public", "private", "protected", "static", "final", "abstract", "extends", "implements",
        "return", "throw", "catch", "try", "if", "else", "for", "while", "switch", "case"
    );
    
    private static final Set<String> FRAMEWORK_KEYWORDS = Set.of(
        "spring", "hibernate", "junit", "mockito", "lombok", "jackson", "slf4j",
        "controller", "service", "repository", "component", "autowired", "bean",
        "entity", "table", "column", "test", "mock", "before", "after"
    );

    /**
     * Analyze query and provide search optimization recommendations
     */
    public QueryAnalysis analyzeQuery(String query) {
        QueryAnalysis analysis = new QueryAnalysis(query);
        
        String queryLower = query.toLowerCase();
        
        // Detect query type
        analysis.queryType = detectQueryType(query);
        
        // Extract and classify terms
        analysis.terms = extractTerms(query);
        analysis.codeTerms = extractCodeTerms(queryLower);
        analysis.frameworkTerms = extractFrameworkTerms(queryLower);
        
        // Detect patterns
        analysis.hasMethodSignature = METHOD_PATTERN.matcher(query).find();
        analysis.hasClassName = CLASS_PATTERN.matcher(query).find();
        analysis.hasAnnotation = ANNOTATION_PATTERN.matcher(query).find();
        analysis.hasPackageName = PACKAGE_PATTERN.matcher(query).find();
        
        // Determine search strategy preference
        analysis.searchStrategy = determineOptimalStrategy(analysis);
        
        // Calculate complexity score
        analysis.complexityScore = calculateComplexityScore(analysis);
        
        // Generate search suggestions
        analysis.suggestions = generateSearchSuggestions(analysis);
        
        return analysis;
    }

    private QueryType detectQueryType(String query) {
        String queryLower = query.toLowerCase();
        
        // Natural language patterns
        if (queryLower.startsWith("find") || queryLower.startsWith("show") || 
            queryLower.startsWith("get") || queryLower.startsWith("search")) {
            return QueryType.NATURAL_LANGUAGE;
        }
        
        // Code search patterns
        if (METHOD_PATTERN.matcher(query).find()) {
            return QueryType.METHOD_SEARCH;
        }
        
        if (CLASS_PATTERN.matcher(query).find() && 
            (queryLower.contains("class") || queryLower.contains("interface"))) {
            return QueryType.CLASS_SEARCH;
        }
        
        if (ANNOTATION_PATTERN.matcher(query).find()) {
            return QueryType.ANNOTATION_SEARCH;
        }
        
        if (PACKAGE_PATTERN.matcher(query).find()) {
            return QueryType.PACKAGE_SEARCH;
        }
        
        // Error/exception search
        if (queryLower.contains("error") || queryLower.contains("exception") || 
            queryLower.contains("throw") || queryLower.contains("catch")) {
            return QueryType.ERROR_SEARCH;
        }
        
        // Configuration search
        if (queryLower.contains("config") || queryLower.contains("properties") || 
            queryLower.contains("setting") || queryLower.contains("environment")) {
            return QueryType.CONFIG_SEARCH;
        }
        
        // Test search
        if (queryLower.contains("test") || queryLower.contains("junit") || 
            queryLower.contains("mock") || queryLower.contains("assert")) {
            return QueryType.TEST_SEARCH;
        }
        
        return QueryType.GENERIC;
    }

    private List<String> extractTerms(String query) {
        return Arrays.stream(query.split("[\\s,;.!?()\\[\\]{}\"']+"))
                    .filter(term -> term.length() > 2)
                    .filter(term -> !isStopWord(term))
                    .distinct()
                    .collect(Collectors.toList());
    }

    private List<String> extractCodeTerms(String queryLower) {
        return Arrays.stream(queryLower.split("\\s+"))
                    .filter(CODE_KEYWORDS::contains)
                    .distinct()
                    .collect(Collectors.toList());
    }

    private List<String> extractFrameworkTerms(String queryLower) {
        return Arrays.stream(queryLower.split("\\s+"))
                    .filter(FRAMEWORK_KEYWORDS::contains)
                    .distinct()
                    .collect(Collectors.toList());
    }

    private SearchStrategy determineOptimalStrategy(QueryAnalysis analysis) {
        // High-level semantic queries benefit from vector search
        if (analysis.queryType == QueryType.NATURAL_LANGUAGE || 
            analysis.complexityScore > 0.7) {
            return SearchStrategy.VECTOR_PREFERRED;
        }
        
        // Specific code searches work better with text search
        if (analysis.queryType == QueryType.METHOD_SEARCH || 
            analysis.queryType == QueryType.ANNOTATION_SEARCH ||
            analysis.hasMethodSignature) {
            return SearchStrategy.TEXT_PREFERRED;
        }
        
        // Balanced approach for other cases
        return SearchStrategy.HYBRID_BALANCED;
    }

    private double calculateComplexityScore(QueryAnalysis analysis) {
        double score = 0.0;
        
        // Base complexity from query length
        score += Math.min(0.3, analysis.terms.size() * 0.05);
        
        // Natural language complexity
        if (analysis.queryType == QueryType.NATURAL_LANGUAGE) {
            score += 0.4;
        }
        
        // Code-specific complexity
        score += analysis.codeTerms.size() * 0.1;
        score += analysis.frameworkTerms.size() * 0.15;
        
        // Pattern complexity
        if (analysis.hasMethodSignature) score += 0.2;
        if (analysis.hasClassName) score += 0.15;
        if (analysis.hasAnnotation) score += 0.1;
        if (analysis.hasPackageName) score += 0.1;
        
        return Math.min(1.0, score);
    }

    private List<String> generateSearchSuggestions(QueryAnalysis analysis) {
        List<String> suggestions = new ArrayList<>();
        
        switch (analysis.queryType) {
            case METHOD_SEARCH -> {
                suggestions.add("Try searching for just the method name without parentheses");
                suggestions.add("Include parameter types for more specific results");
                suggestions.add("Search for the class name that contains this method");
            }
            case CLASS_SEARCH -> {
                suggestions.add("Try searching for methods within this class");
                suggestions.add("Look for classes that extend or implement this");
                suggestions.add("Search for imports of this class");
            }
            case NATURAL_LANGUAGE -> {
                suggestions.add("Try using specific technical terms");
                suggestions.add("Include relevant framework or library names");
                suggestions.add("Be more specific about the functionality you're looking for");
            }
            case ERROR_SEARCH -> {
                suggestions.add("Include the full exception class name");
                suggestions.add("Search for the error message text");
                suggestions.add("Look for try-catch blocks related to this error");
            }
            case CONFIG_SEARCH -> {
                suggestions.add("Search for property file names");
                suggestions.add("Look for @ConfigurationProperties annotations");
                suggestions.add("Try searching for environment-specific settings");
            }
            default -> {
                suggestions.add("Try being more specific with technical terms");
                suggestions.add("Include relevant class or method names");
                suggestions.add("Consider searching for related concepts");
            }
        }
        
        return suggestions;
    }

    private boolean isStopWord(String term) {
        Set<String> stopWords = Set.of(
            "the", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by",
            "from", "is", "are", "was", "were", "be", "been", "have", "has", "had", "do",
            "does", "did", "will", "would", "could", "should", "may", "might", "can",
            "this", "that", "these", "those", "i", "you", "he", "she", "it", "we", "they"
        );
        return stopWords.contains(term.toLowerCase());
    }

    /**
     * Query analysis result with search optimization recommendations
     */
    public static class QueryAnalysis {
        private final String originalQuery;
        private QueryType queryType;
        private List<String> terms;
        private List<String> codeTerms;
        private List<String> frameworkTerms;
        private boolean hasMethodSignature;
        private boolean hasClassName;
        private boolean hasAnnotation;
        private boolean hasPackageName;
        private SearchStrategy searchStrategy;
        private double complexityScore;
        private List<String> suggestions;

        public QueryAnalysis(String originalQuery) {
            this.originalQuery = originalQuery;
        }

        // Getters
        public String getOriginalQuery() { return originalQuery; }
        public QueryType getQueryType() { return queryType; }
        public List<String> getTerms() { return terms; }
        public List<String> getCodeTerms() { return codeTerms; }
        public List<String> getFrameworkTerms() { return frameworkTerms; }
        public boolean hasMethodSignature() { return hasMethodSignature; }
        public boolean hasClassName() { return hasClassName; }
        public boolean hasAnnotation() { return hasAnnotation; }
        public boolean hasPackageName() { return hasPackageName; }
        public SearchStrategy getSearchStrategy() { return searchStrategy; }
        public double getComplexityScore() { return complexityScore; }
        public List<String> getSuggestions() { return suggestions; }
    }

    public enum QueryType {
        NATURAL_LANGUAGE,
        METHOD_SEARCH,
        CLASS_SEARCH,
        ANNOTATION_SEARCH,
        PACKAGE_SEARCH,
        ERROR_SEARCH,
        CONFIG_SEARCH,
        TEST_SEARCH,
        GENERIC
    }

    public enum SearchStrategy {
        VECTOR_PREFERRED,    // Use vector search primarily
        TEXT_PREFERRED,      // Use text search primarily  
        HYBRID_BALANCED      // Use both equally
    }
}
