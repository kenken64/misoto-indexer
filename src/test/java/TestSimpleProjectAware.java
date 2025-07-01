import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import sg.edu.nus.iss.codebase.indexer.IndexerApplication;
import sg.edu.nus.iss.codebase.indexer.service.HybridSearchService;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileIndexingService;

/**
 * Simple test to verify project-aware prioritization
 */
public class TestSimpleProjectAware {
    public static void main(String[] args) {
        System.setProperty("app.cli.enabled", "false");
        ApplicationContext context = SpringApplication.run(IndexerApplication.class);
        
        try {
            System.out.println("🔍 SIMPLE PROJECT-AWARE TEST");
            System.out.println("===========================");
            
            HybridSearchService searchService = context.getBean(HybridSearchService.class);
            FileIndexingService indexingService = context.getBean(FileIndexingService.class);
            
            // Index Python project
            indexingService.setIndexingDirectory("./codebase/dssi-day3-ollama");
            
            System.out.println("🐍 Testing search for 'python' in Python project");
            System.out.println("Expected: Python files should rank higher than non-Python files");
            System.out.println();
            
            // Search for something Python-specific
            var results = searchService.search("python", 5);
            
            System.out.printf("📊 Found %d results%n", results.size());
            
            if (results.size() > 0) {
                System.out.println("📋 Results:");
                for (int i = 0; i < results.size(); i++) {
                    var result = results.get(i);
                    String filename = result.getFileName();
                    boolean isPython = filename.endsWith(".py");
                    String indicator = isPython ? "🐍" : "📄";
                    
                    System.out.printf("   %d. %s %s (Score: %.3f)%n", 
                        i + 1, indicator, filename, result.getRelevanceScore());
                }
                
                // Check if first result is Python
                boolean firstIsPython = results.get(0).getFileName().endsWith(".py");
                System.out.println();
                if (firstIsPython) {
                    System.out.println("✅ SUCCESS: Python file is ranked first!");
                } else {
                    System.out.println("❌ ISSUE: Python file is not ranked first");
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}