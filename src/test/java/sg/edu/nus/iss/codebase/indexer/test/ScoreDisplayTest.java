package sg.edu.nus.iss.codebase.indexer.test;

import sg.edu.nus.iss.codebase.indexer.util.ScoreFormatter;

/**
 * Test the ScoreFormatter utility to show how scores are displayed
 */
public class ScoreDisplayTest {
    
    public static void main(String[] args) {
        System.out.println("🎯 SCORE DISPLAY DEMONSTRATION");
        System.out.println("=".repeat(50));
        
        // Test different score ranges
        double[] testScores = {0.95, 0.85, 0.75, 0.65, 0.45, 0.25, 0.15, 0.05};
        String[] searchTypes = {"Vector", "Text", "Hybrid", "Enhanced"};
        
        System.out.println("📊 Score Formatting Examples:");
        System.out.println("-".repeat(30));
        
        for (int i = 0; i < testScores.length; i++) {
            double score = testScores[i];
            String searchType = searchTypes[i % searchTypes.length];
            
            System.out.printf("Result %d: Example File %d.java%n", i + 1, i + 1);
            System.out.printf("  Basic:    Score: %s%n", ScoreFormatter.formatScore(score));
            System.out.printf("  Compact:  Score: %s%n", ScoreFormatter.formatScoreCompact(score));
            System.out.printf("  Detailed: %s%n", ScoreFormatter.formatScoreDetailed(score, searchType));
            System.out.printf("  Bar:      %s %s%n", 
                    ScoreFormatter.createScoreBar(score, 15),
                    ScoreFormatter.formatScoreAsPercentage(score));
            System.out.println();
        }
        
        System.out.println("🎨 Quality Indicators:");
        System.out.println("-".repeat(30));
        System.out.println("🟢 Excellent (0.9+)");
        System.out.println("🟡 Good (0.7-0.89)");
        System.out.println("🟠 Fair (0.5-0.69)");
        System.out.println("🔴 Poor (0.3-0.49)");
        System.out.println("⚫ Very Poor (0.0-0.29)");
        
        System.out.println("\n📈 Score Bar Legend:");
        System.out.println("-".repeat(30));
        System.out.println("█ = Filled portion (relevance)");
        System.out.println("░ = Empty portion");
        
        System.out.println("\n✅ Score display is now enabled in search results!");
        System.out.println("   • Vector search results show relevance scores");
        System.out.println("   • File search results show TF-IDF scores");
        System.out.println("   • Advanced search provides detailed score analysis");
        System.out.println("   • Optional detailed score breakdown available");
    }
}
