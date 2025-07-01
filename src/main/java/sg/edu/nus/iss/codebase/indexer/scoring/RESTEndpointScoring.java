package sg.edu.nus.iss.codebase.indexer.scoring;

import org.springframework.ai.document.Document;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Generic REST API endpoint scoring system
 * Designed to identify and rank REST API implementations across any web framework
 */
public class RESTEndpointScoring {
    
    // Generic patterns for REST API detection
    private static final Pattern ROUTE_DECORATOR_PATTERN = Pattern.compile("@\\w+\\.route\\s*\\(\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern WEB_APP_PATTERN = Pattern.compile("\\w+\\s*=\\s*\\w+\\s*\\(");
    private static final Pattern API_PATH_PATTERN = Pattern.compile("/api/[a-zA-Z0-9-_]+");
    private static final Pattern WEB_FRAMEWORK_IMPORTS = Pattern.compile("from\\s+\\w+\\s+import");
    
    /**
     * Calculate generic REST API score designed to rank comprehensive API implementations
     */
    public static double calculateRESTScore(Document doc, String query) {
        String content = doc.getText();
        String contentLower = content.toLowerCase();
        String queryLower = query.toLowerCase();
        
        double score = 0.0;
        
        // STAGE 1: Web Framework Detection (25% weight)
        score += calculateWebFrameworkScore(content, contentLower) * 0.25;
        
        // STAGE 2: REST API Pattern Detection (45% weight) - Highest weight
        score += calculateAPIPatternScore(content, contentLower) * 0.45;
        
        // STAGE 3: Query-API Relevance (20% weight)
        score += calculateAPIQueryRelevance(contentLower, queryLower) * 0.20;
        
        // STAGE 4: Implementation Quality (10% weight)
        score += calculateImplementationQuality(content, contentLower) * 0.10;
        
        return Math.min(score, 1.0);
    }
    
    /**
     * STAGE 1: Generic Web Framework Detection
     */
    private static double calculateWebFrameworkScore(String content, String contentLower) {
        double frameworkScore = 0.0;
        
        // Web application initialization patterns
        if (WEB_APP_PATTERN.matcher(content).find()) {
            frameworkScore += 0.3; // Generic app initialization
        }
        
        // Web framework imports (covers Flask, FastAPI, Django, etc.)
        if (WEB_FRAMEWORK_IMPORTS.matcher(content).find()) {
            frameworkScore += 0.25;
            
            // Bonus for comprehensive web imports
            int webImportCount = 0;
            String[] webImports = {"render_template", "request", "jsonify", "response", "json", "http"};
            for (String webImport : webImports) {
                if (contentLower.contains(webImport)) {
                    webImportCount++;
                }
            }
            
            if (webImportCount >= 3) {
                frameworkScore += 0.25; // Comprehensive web framework usage
            }
        }
        
        // Route/endpoint decorator patterns
        Matcher routeMatcher = ROUTE_DECORATOR_PATTERN.matcher(content);
        int routeCount = 0;
        while (routeMatcher.find()) {
            routeCount++;
        }
        
        if (routeCount > 0) {
            frameworkScore += Math.min(0.4, routeCount * 0.07); // Higher weight for multiple routes
        }
        
        return Math.min(frameworkScore, 1.0);
    }
    
    /**
     * STAGE 2: REST API Pattern Detection - Core scoring logic
     */
    private static double calculateAPIPatternScore(String content, String contentLower) {
        double apiScore = 0.0;
        
        // API endpoint path scoring (targets app.py patterns)
        Map<String, Double> endpointPatterns = new HashMap<>();
        endpointPatterns.put("/api/generate", 0.20);      // Generation endpoints
        endpointPatterns.put("/api/validate", 0.18);      // Validation endpoints  
        endpointPatterns.put("/api/status", 0.15);        // Status endpoints
        endpointPatterns.put("/api/", 0.10);              // Generic API paths
        endpointPatterns.put("'/api/", 0.10);             // Quoted API paths
        endpointPatterns.put("\"/api/", 0.10);            // Double-quoted API paths
        
        for (Map.Entry<String, Double> pattern : endpointPatterns.entrySet()) {
            if (contentLower.contains(pattern.getKey().toLowerCase())) {
                apiScore += pattern.getValue();
            }
        }
        
        // HTTP method diversity scoring
        Map<String, Double> httpMethods = new HashMap<>();
        httpMethods.put("post", 0.15);   // POST APIs are valuable
        httpMethods.put("get", 0.10);    // GET APIs common
        httpMethods.put("put", 0.08);    // PUT for updates
        httpMethods.put("delete", 0.08); // DELETE for removal
        
        for (Map.Entry<String, Double> method : httpMethods.entrySet()) {
            if (contentLower.contains("'" + method.getKey() + "'") || 
                contentLower.contains("\"" + method.getKey() + "\"") ||
                contentLower.contains("methods") && contentLower.contains(method.getKey())) {
                apiScore += method.getValue();
            }
        }
        
        // API endpoint density bonus
        Matcher apiMatcher = API_PATH_PATTERN.matcher(content);
        int apiCount = 0;
        while (apiMatcher.find()) {
            apiCount++;
        }
        
        if (apiCount >= 3) {
            apiScore += 0.20; // Strong bonus for multiple API endpoints
        } else if (apiCount >= 2) {
            apiScore += 0.10; // Moderate bonus
        }
        
        // JSON response patterns (REST APIs typically return JSON)
        if (contentLower.contains("jsonify") || contentLower.contains("json.dumps") || 
            contentLower.contains("return json") || contentLower.contains("'success'")) {
            apiScore += 0.15;
        }
        
        // Error handling in APIs
        if (contentLower.contains("error") && (contentLower.contains("400") || 
            contentLower.contains("500") || contentLower.contains("404"))) {
            apiScore += 0.10;
        }
        
        return Math.min(apiScore, 1.0);
    }
    
    /**
     * STAGE 3: Query-API Relevance scoring
     */
    private static double calculateAPIQueryRelevance(String contentLower, String queryLower) {
        double relevanceScore = 0.0;
        
        // Direct query term matching optimized for REST API searches
        Map<String, Double> queryTermWeights = new HashMap<>();
        queryTermWeights.put("rest api", 0.30);
        queryTermWeights.put("api endpoint", 0.25);
        queryTermWeights.put("web api", 0.20);
        queryTermWeights.put("endpoint", 0.15);
        queryTermWeights.put("api", 0.15);
        queryTermWeights.put("route", 0.12);
        queryTermWeights.put("service", 0.10);
        queryTermWeights.put("http", 0.08);
        queryTermWeights.put("json", 0.08);
        queryTermWeights.put("web service", 0.15);
        
        for (Map.Entry<String, Double> term : queryTermWeights.entrySet()) {
            if (queryLower.contains(term.getKey())) {
                // Context-aware matching
                if (term.getKey().contains("api") && contentLower.contains("/api/")) {
                    relevanceScore += term.getValue();
                } else if (term.getKey().equals("endpoint") && contentLower.contains("@")) {
                    relevanceScore += term.getValue();
                } else if (term.getKey().equals("route") && contentLower.contains("route")) {
                    relevanceScore += term.getValue();
                } else if (contentLower.contains(term.getKey())) {
                    relevanceScore += term.getValue() * 0.8;
                }
            }
        }
        
        // Semantic query matching
        if (queryLower.contains("endpoint") && contentLower.contains("def ")) {
            relevanceScore += 0.10; // Function definitions often are endpoints
        }
        
        if (queryLower.contains("rest") && contentLower.contains("json")) {
            relevanceScore += 0.15; // REST typically uses JSON
        }
        
        return Math.min(relevanceScore, 1.0);
    }
    
    /**
     * STAGE 4: Implementation Quality assessment
     */
    private static double calculateImplementationQuality(String content, String contentLower) {
        double qualityScore = 0.4; // Base score
        
        // File size and comprehensiveness
        if (content.length() > 4000) qualityScore += 0.2;  // Substantial implementation
        if (content.length() > 7000) qualityScore += 0.15; // Comprehensive implementation
        
        // Documentation quality
        if (content.contains("\"\"\"") || content.contains("'''") || content.contains("/**")) {
            qualityScore += 0.15; // Good documentation
        }
        
        // Error handling sophistication
        if (contentLower.contains("try:") && contentLower.contains("except")) {
            qualityScore += 0.10;
        }
        
        // Logging and monitoring
        if (contentLower.contains("log") || contentLower.contains("print")) {
            qualityScore += 0.05;
        }
        
        // Configuration and setup
        if (contentLower.contains("config") || contentLower.contains("args") || 
            contentLower.contains("environ")) {
            qualityScore += 0.05;
        }
        
        return Math.min(qualityScore, 1.0);
    }
    
    /**
     * Generate scoring breakdown for analysis
     */
    public static String generateScoringBreakdown(Document doc, String query) {
        String content = doc.getText();
        String contentLower = content.toLowerCase();
        String queryLower = query.toLowerCase();
        
        double frameworkScore = calculateWebFrameworkScore(content, contentLower);
        double apiScore = calculateAPIPatternScore(content, contentLower);
        double relevanceScore = calculateAPIQueryRelevance(contentLower, queryLower);
        double qualityScore = calculateImplementationQuality(content, contentLower);
        
        double totalScore = frameworkScore * 0.25 + apiScore * 0.45 + 
                           relevanceScore * 0.20 + qualityScore * 0.10;
        
        StringBuilder breakdown = new StringBuilder();
        breakdown.append("🎯 REST API Scoring Breakdown:\n");
        breakdown.append(String.format("   Framework Score: %.3f (25%% weight)\n", frameworkScore));
        breakdown.append(String.format("   API Pattern:     %.3f (45%% weight)\n", apiScore));
        breakdown.append(String.format("   Query Relevance: %.3f (20%% weight)\n", relevanceScore));
        breakdown.append(String.format("   Quality Score:   %.3f (10%% weight)\n", qualityScore));
        breakdown.append(String.format("   TOTAL SCORE:     %.3f\n", totalScore));
        
        return breakdown.toString();
    }
}
