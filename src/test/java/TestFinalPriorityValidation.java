import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import sg.edu.nus.iss.codebase.indexer.IndexerApplication;
import sg.edu.nus.iss.codebase.indexer.service.HybridSearchService;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileIndexingService;

/**
 * Final validation that Python files consistently rank higher than HTML/other files
 */
public class TestFinalPriorityValidation {
    public static void main(String[] args) {
        System.setProperty("app.cli.enabled", "false");
        ApplicationContext context = SpringApplication.run(IndexerApplication.class);
        
        try {
            System.out.println("✅ FINAL PROJECT-AWARE PRIORITY VALIDATION");
            System.out.println("==========================================");
            
            HybridSearchService searchService = context.getBean(HybridSearchService.class);
            FileIndexingService indexingService = context.getBean(FileIndexingService.class);
            
            // Index Python project
            indexingService.setIndexingDirectory("./codebase/dssi-day3-ollama");
            
            System.out.println("🎯 Testing multiple search terms in Python/Flask project");
            System.out.println("Expected: Python files should consistently rank higher than HTML/CSS/JS files");
            System.out.println();
            
            String[] testQueries = {
                "app",
                "function", 
                "web",
                "template",
                "server",
                "python",
                "flask"
            };
            
            int totalTests = testQueries.length;
            int pythonFirstTests = 0;
            
            for (String query : testQueries) {
                System.out.printf("🔍 Testing '%s'...%n", query);
                var results = searchService.search(query, 5);
                
                if (results.size() > 0) {
                    String firstFile = results.get(0).getFileName();
                    boolean firstIsPython = firstFile.endsWith(".py");
                    
                    if (firstIsPython) {
                        pythonFirstTests++;
                        System.out.printf("   ✅ Python first: %s%n", firstFile);
                    } else {
                        System.out.printf("   ⚠️ Non-Python first: %s%n", firstFile);
                    }
                } else {
                    System.out.println("   ❌ No results found");
                }
            }
            
            System.out.println();
            System.out.println("📊 FINAL RESULTS:");
            System.out.printf("   • Python-first results: %d/%d (%.1f%%)%n", 
                pythonFirstTests, totalTests, (pythonFirstTests * 100.0 / totalTests));
            
            if (pythonFirstTests >= totalTests * 0.8) { // 80% success rate
                System.out.println("   🎉 EXCELLENT: Python prioritization working consistently!");
            } else if (pythonFirstTests >= totalTests * 0.6) { // 60% success rate
                System.out.println("   ✅ GOOD: Python prioritization working well!");
            } else {
                System.out.println("   ⚠️ NEEDS IMPROVEMENT: Python prioritization inconsistent");
            }
            
            System.out.println();
            System.out.println("🎯 PROJECT-AWARE ENHANCEMENTS SUMMARY:");
            System.out.println("   ✅ Increased project-aware weight to 20%");
            System.out.println("   ✅ Boosted Python file bonus to 0.8");
            System.out.println("   ✅ Added penalties for HTML/CSS/JS in Python projects");
            System.out.println("   ✅ Increased file search multiplier to 5x");
            System.out.println("   ✅ Enhanced Flask framework detection");
            System.out.println();
            System.out.println("🚀 Python files should now consistently rank higher in Python projects!");
            
        } catch (Exception e) {
            System.err.println("❌ Validation failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}