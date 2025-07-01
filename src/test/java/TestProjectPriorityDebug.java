import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import sg.edu.nus.iss.codebase.indexer.IndexerApplication;
import sg.edu.nus.iss.codebase.indexer.service.HybridSearchService;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileIndexingService;

/**
 * Debug test to see why HTML is ranking higher than Python
 */
public class TestProjectPriorityDebug {
    public static void main(String[] args) {
        System.setProperty("app.cli.enabled", "false");
        ApplicationContext context = SpringApplication.run(IndexerApplication.class);
        
        try {
            System.out.println("🔍 DEBUGGING HTML vs PYTHON PRIORITY");
            System.out.println("===================================");
            
            HybridSearchService searchService = context.getBean(HybridSearchService.class);
            FileIndexingService indexingService = context.getBean(FileIndexingService.class);
            
            // Index Python project
            indexingService.setIndexingDirectory("./codebase/dssi-day3-ollama");
            
            System.out.println("🎯 Searching for query that returned HTML higher than Python");
            System.out.println("Expected: Python files should rank higher than HTML files");
            System.out.println();
            
            // Test the problematic search
            System.out.println("🔍 Searching for terms that might favor both HTML and Python...");
            var results = searchService.search("app", 5);
            
            System.out.printf("📊 Found %d results%n", results.size());
            System.out.println("📋 Detailed Results:");
            
            for (int i = 0; i < results.size(); i++) {
                var result = results.get(i);
                String filename = result.getFileName();
                String extension = getFileExtension(filename);
                boolean isPython = extension.equals(".py");
                boolean isHtml = extension.equals(".html");
                double score = result.getRelevanceScore();
                
                String typeIndicator;
                if (isPython) {
                    typeIndicator = "🐍 PYTHON";
                } else if (isHtml) {
                    typeIndicator = "🌐 HTML";
                } else {
                    typeIndicator = "📄 " + extension.toUpperCase();
                }
                
                System.out.printf("   %d. %-25s [%s] Score: %.3f%n", 
                    i + 1, filename, typeIndicator, score);
            }
            
            // Analysis
            System.out.println();
            System.out.println("🔍 PRIORITY ANALYSIS:");
            
            boolean pythonFirst = false;
            boolean htmlBeforePython = false;
            int firstPythonIndex = -1;
            int firstHtmlIndex = -1;
            
            for (int i = 0; i < results.size(); i++) {
                var result = results.get(i);
                String extension = getFileExtension(result.getFileName());
                
                if (extension.equals(".py") && firstPythonIndex == -1) {
                    firstPythonIndex = i;
                    if (i == 0) pythonFirst = true;
                }
                if (extension.equals(".html") && firstHtmlIndex == -1) {
                    firstHtmlIndex = i;
                }
            }
            
            if (firstHtmlIndex != -1 && firstPythonIndex != -1 && firstHtmlIndex < firstPythonIndex) {
                htmlBeforePython = true;
            }
            
            System.out.printf("   • First Python file at position: %d%n", firstPythonIndex + 1);
            System.out.printf("   • First HTML file at position: %d%n", firstHtmlIndex + 1);
            
            if (pythonFirst) {
                System.out.println("   ✅ SUCCESS: Python file is ranked first!");
            } else if (htmlBeforePython) {
                System.out.println("   ❌ ISSUE: HTML file ranks higher than Python file");
                System.out.println("   🔧 Need to strengthen project-aware scoring");
            } else {
                System.out.println("   ⚠️ MIXED: No clear HTML vs Python conflict detected");
            }
            
            System.out.println();
            System.out.println("💡 DEBUGGING TIPS:");
            System.out.println("   • Check if project-aware bonus is strong enough");
            System.out.println("   • Verify penalty for HTML files in Python projects");
            System.out.println("   • Ensure vector vs file search scoring consistency");
            
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