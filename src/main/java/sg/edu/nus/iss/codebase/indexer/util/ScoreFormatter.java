package sg.edu.nus.iss.codebase.indexer.util;

/**
 * Utility class for formatting search result scores and providing score analysis
 */
public class ScoreFormatter {
    
    /**
     * Format a relevance score with appropriate precision and visual indicators
     */
    public static String formatScore(double score) {
        return String.format("%.3f", score);
    }
    
    /**
     * Format a score with visual quality indicator
     */
    public static String formatScoreWithQuality(double score) {
        String formattedScore = formatScore(score);
        String qualityIndicator = getQualityIndicator(score);
        return String.format("%s %s", formattedScore, qualityIndicator);
    }
    
    /**
     * Get a visual quality indicator based on score
     */
    public static String getQualityIndicator(double score) {
        if (score >= 0.9) {
            return "🟢"; // Excellent match
        } else if (score >= 0.7) {
            return "🟡"; // Good match
        } else if (score >= 0.5) {
            return "🟠"; // Fair match
        } else if (score >= 0.3) {
            return "🔴"; // Poor match
        } else {
            return "⚫"; // Very poor match
        }
    }
    
    /**
     * Get score quality description
     */
    public static String getScoreQuality(double score) {
        if (score >= 0.9) {
            return "Excellent";
        } else if (score >= 0.7) {
            return "Good";
        } else if (score >= 0.5) {
            return "Fair";
        } else if (score >= 0.3) {
            return "Poor";
        } else {
            return "Very Poor";
        }
    }
    
    /**
     * Format score with percentage representation
     */
    public static String formatScoreAsPercentage(double score) {
        return String.format("%.1f%%", score * 100);
    }
    
    /**
     * Format score for detailed analysis display
     */
    public static String formatScoreDetailed(double score, String searchType) {
        return String.format("Score: %s (%s) [%s - %s match]", 
                formatScore(score),
                formatScoreAsPercentage(score),
                searchType,
                getScoreQuality(score));
    }
    
    /**
     * Create a visual score bar (for terminal display)
     */
    public static String createScoreBar(double score, int length) {
        int filledLength = (int) (score * length);
        StringBuilder bar = new StringBuilder();
        
        bar.append("[");
        for (int i = 0; i < length; i++) {
            if (i < filledLength) {
                bar.append("█");
            } else {
                bar.append("░");
            }
        }
        bar.append("]");
        
        return bar.toString();
    }
    
    /**
     * Format score for compact display (used in lists)
     */
    public static String formatScoreCompact(double score) {
        return String.format("%.3f %s", score, getQualityIndicator(score));
    }
}
