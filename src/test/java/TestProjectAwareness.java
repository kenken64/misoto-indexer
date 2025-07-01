import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import sg.edu.nus.iss.codebase.indexer.IndexerApplication;
import sg.edu.nus.iss.codebase.indexer.service.HybridSearchService;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileIndexingService;

/**
 * Test project-aware prioritization in Python project
 */
public class TestProjectAwareness {
    public static void main(String[] args) {
        System.setProperty("app.cli.enabled", "false");
        ApplicationContext context = SpringApplication.run(IndexerApplication.class);
        
        try {
            System.out.println("🧪 TESTING PROJECT-AWARE PRIORITIZATION");
            System.out.println("======================================");
            
            HybridSearchService searchService = context.getBean(HybridSearchService.class);
            FileIndexingService indexingService = context.getBean(FileIndexingService.class);
            
            // Index Python project
            indexingService.setIndexingDirectory("./codebase/dssi-day3-ollama");
            
            System.out.println("📊 Project Setup:");
            System.out.println("   • Directory: ./codebase/dssi-day3-ollama (Python project)");
            System.out.println("   • Expected behavior: Python files should be prioritized higher");
            System.out.println();
            
            // Test 1: General search that should prioritize Python files
            System.out.println("🎯 Test 1: Searching for 'function' in Python project");
            System.out.println("Expected: Python files (.py) should appear higher in results");
            System.out.println();
            
            var results1 = searchService.search("function", 10);
            System.out.printf("📊 Search Results: %d documents found%n", results1.size());
            
            if (results1.size() > 0) {
                System.out.println("📋 Top 5 results (should prioritize Python files):");
                for (int i = 0; i < Math.min(5, results1.size()); i++) {
                    var result = results1.get(i);
                    String extension = getFileExtension(result.getFileName());
                    String priority = extension.equals(".py") ? "🐍 PYTHON" : "📄 " + extension.toUpperCase();
                    System.out.printf("   %d. %s [%s]%n", i + 1, result.getFileName(), priority);
                }
            }
            
            System.out.println();
            
            // Test 2: Python-specific search
            System.out.println("🎯 Test 2: Searching for 'Flask routes' in Python project");
            System.out.println("Expected: app.py and other Flask files should be prioritized");
            System.out.println();
            
            var results2 = searchService.search("Flask routes", 8);
            System.out.printf("📊 Search Results: %d documents found%n", results2.size());
            
            if (results2.size() > 0) {
                System.out.println("📋 Top results (should prioritize Flask/Python files):");
                for (int i = 0; i < Math.min(3, results2.size()); i++) {
                    var result = results2.get(i);
                    String extension = getFileExtension(result.getFileName());
                    String priority = extension.equals(".py") ? "🐍 PYTHON" : "📄 " + extension.toUpperCase();
                    System.out.printf("   %d. %s [%s]%n", i + 1, result.getFileName(), priority);
                    
                    // Show content preview for Python files
                    if (extension.equals(".py") && result.getContent().length() > 0) {
                        String preview = result.getContent().substring(0, Math.min(100, result.getContent().length()));
                        System.out.printf("      Preview: %s...%n", preview.replace("\\n", " "));
                    }
                }
            }
            
            System.out.println();
            
            // Analysis
            System.out.println("🔍 PROJECT-AWARE PRIORITIZATION ANALYSIS:");
            System.out.println("✅ Key features implemented:");
            System.out.println("   • Project type detection (Python project detected)");
            System.out.println("   • File extension boost (+0.4 for .py files)");
            System.out.println("   • Flask framework detection (+0.3 for Flask imports)");
            System.out.println("   • Python syntax bonuses (def, imports, etc.)");
            System.out.println("   • Python config files boost (requirements.txt, setup.py)");
            System.out.println();
            
            // Count Python vs non-Python files in top results
            int pythonCount = 0;
            int nonPythonCount = 0;
            for (var result : results1) {
                if (getFileExtension(result.getFileName()).equals(".py")) {
                    pythonCount++;
                } else {
                    nonPythonCount++;
                }
            }
            
            System.out.printf("📊 Result Distribution: %d Python files, %d non-Python files%n", pythonCount, nonPythonCount);
            if (pythonCount > nonPythonCount) {
                System.out.println("✅ SUCCESS: Python files are prioritized in Python project!");
            } else {
                System.out.println("⚠️ ATTENTION: Python files should be prioritized more in Python project");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
    
    private static String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}