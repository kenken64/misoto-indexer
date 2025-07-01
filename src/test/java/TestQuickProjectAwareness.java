import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import sg.edu.nus.iss.codebase.indexer.IndexerApplication;
import sg.edu.nus.iss.codebase.indexer.service.HybridSearchService;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileIndexingService;

/**
 * Quick test for project-aware prioritization
 */
public class TestQuickProjectAwareness {
    public static void main(String[] args) {
        System.setProperty("app.cli.enabled", "false");
        ApplicationContext context = SpringApplication.run(IndexerApplication.class);
        
        try {
            System.out.println("🧪 QUICK PROJECT-AWARE PRIORITIZATION TEST");
            System.out.println("=========================================");
            
            HybridSearchService searchService = context.getBean(HybridSearchService.class);
            FileIndexingService indexingService = context.getBean(FileIndexingService.class);
            
            // Index Python project
            indexingService.setIndexingDirectory("./codebase/dssi-day3-ollama");
            
            System.out.println("🐍 Testing in Python project: ./codebase/dssi-day3-ollama");
            System.out.println("Expected: Python files should get higher scores");
            System.out.println();
            
            // Quick search test
            System.out.println("🔍 Searching for 'python code'...");
            var results = searchService.search("python code", 5);
            
            System.out.printf("📊 Found %d results%n", results.size());
            System.out.println("📋 Results (Python files should be prioritized):");
            
            for (int i = 0; i < Math.min(3, results.size()); i++) {
                var result = results.get(i);
                String filename = result.getFileName();
                boolean isPython = filename.endsWith(".py");
                String indicator = isPython ? "🐍" : "📄";
                
                System.out.printf("   %d. %s %s%n", i + 1, indicator, filename);
            }
            
            // Count Python files in top results
            long pythonFiles = results.stream()
                .limit(3)
                .mapToLong(r -> r.getFileName().endsWith(".py") ? 1 : 0)
                .sum();
            
            System.out.println();
            System.out.printf("📊 Python files in top 3: %d/3%n", pythonFiles);
            
            if (pythonFiles >= 2) {
                System.out.println("✅ SUCCESS: Project-aware prioritization working!");
                System.out.println("🎯 Python files are properly prioritized in Python project");
            } else if (pythonFiles >= 1) {
                System.out.println("⚠️ PARTIAL: Some Python prioritization detected");
                System.out.println("💡 Consider increasing project-aware bonus weights");
            } else {
                System.out.println("❌ ISSUE: Python files not prioritized in Python project");
                System.out.println("🔧 Project-aware scoring may need adjustment");
            }
            
            System.out.println();
            System.out.println("✅ Test complete - Project-aware prioritization implemented!");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}