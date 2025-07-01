import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import sg.edu.nus.iss.codebase.indexer.IndexerApplication;
import sg.edu.nus.iss.codebase.indexer.service.HybridSearchService;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileIndexingService;

/**
 * Debug why Python files aren't being prioritized in search results
 */
public class DebugProjectPrioritization {
    public static void main(String[] args) {
        System.setProperty("app.cli.enabled", "false");
        ApplicationContext context = SpringApplication.run(IndexerApplication.class);
        
        try {
            System.out.println("🔍 DEBUGGING PROJECT-AWARE PRIORITIZATION");
            System.out.println("=========================================");
            
            HybridSearchService searchService = context.getBean(HybridSearchService.class);
            FileIndexingService indexingService = context.getBean(FileIndexingService.class);
            
            // Index Python project
            indexingService.setIndexingDirectory("./codebase/dssi-day3-ollama");
            
            System.out.println("📊 Project Context:");
            System.out.printf("   • Directory: %s%n", indexingService.getCurrentIndexingDirectory());
            System.out.printf("   • Collection: %s%n", indexingService.getCurrentCollectionName());
            System.out.println("   • Expected: Python files should be ranked higher");
            System.out.println();
            
            // Test search that should prioritize Python files
            System.out.println("🔍 Searching for 'function' (should prioritize Python .py files)...");
            var results = searchService.search("function", 10);
            
            System.out.printf("📊 Found %d results%n", results.size());
            System.out.println();
            System.out.println("📋 Search Results (ordered by current ranking):");
            
            for (int i = 0; i < results.size(); i++) {
                var result = results.get(i);
                String filename = result.getFileName();
                String extension = getFileExtension(filename);
                boolean isPython = extension.equals(".py");
                String indicator = isPython ? "🐍 PYTHON" : "📄 " + extension.toUpperCase();
                double score = result.getRelevanceScore();
                
                System.out.printf("   %2d. %-20s [%s] Score: %.3f%n", 
                    i + 1, filename, indicator, score);
            }
            
            // Analysis
            System.out.println();
            System.out.println("🔍 ANALYSIS:");
            
            int pythonCount = 0;
            int pythonInTop3 = 0;
            double bestPythonScore = 0.0;
            double bestNonPythonScore = 0.0;
            
            for (int i = 0; i < results.size(); i++) {
                var result = results.get(i);
                boolean isPython = getFileExtension(result.getFileName()).equals(".py");
                
                if (isPython) {
                    pythonCount++;
                    if (i < 3) pythonInTop3++;
                    bestPythonScore = Math.max(bestPythonScore, result.getRelevanceScore());
                } else {
                    bestNonPythonScore = Math.max(bestNonPythonScore, result.getRelevanceScore());
                }
            }
            
            System.out.printf("   • Total Python files: %d/%d%n", pythonCount, results.size());
            System.out.printf("   • Python files in top 3: %d/3%n", pythonInTop3);
            System.out.printf("   • Best Python score: %.3f%n", bestPythonScore);
            System.out.printf("   • Best non-Python score: %.3f%n", bestNonPythonScore);
            
            if (pythonInTop3 >= 2) {
                System.out.println("   ✅ SUCCESS: Python files are properly prioritized!");
            } else if (bestPythonScore < bestNonPythonScore) {
                System.out.println("   ❌ ISSUE: Python files have lower scores than non-Python files");
                System.out.println("   🔧 Project-aware bonus may not be applying correctly");
            } else {
                System.out.println("   ⚠️ PARTIAL: Some Python prioritization, but could be stronger");
            }
            
            System.out.println();
            System.out.println("💡 DEBUG TIPS:");
            System.out.println("   • Check if project type detection is working");
            System.out.println("   • Verify project-aware bonus is being added to composite scores");
            System.out.println("   • Ensure Python file detection logic is correct");
            
        } catch (Exception e) {
            System.err.println("❌ Debug failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
    
    private static String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }
}