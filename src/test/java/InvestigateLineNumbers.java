import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import sg.edu.nus.iss.codebase.indexer.IndexerApplication;
import sg.edu.nus.iss.codebase.indexer.service.HybridSearchService;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileIndexingService;

/**
 * Investigate why Python code line numbers are not showing accurately
 */
public class InvestigateLineNumbers {
    public static void main(String[] args) {
        System.setProperty("app.cli.enabled", "false");
        ApplicationContext context = SpringApplication.run(IndexerApplication.class);
        
        try {
            System.out.println("🔍 INVESTIGATING PYTHON LINE NUMBER ACCURACY");
            System.out.println("===========================================");
            
            HybridSearchService searchService = context.getBean(HybridSearchService.class);
            FileIndexingService indexingService = context.getBean(FileIndexingService.class);
            
            // Index Python project
            indexingService.setIndexingDirectory("./codebase/dssi-day3-ollama");
            
            System.out.println("🎯 Testing Python-specific searches to examine line number accuracy");
            System.out.println();
            
            // Test 1: Search for Flask specific terms
            System.out.println("🐍 Test 1: Searching for 'Flask import'");
            var results1 = searchService.search("Flask import", 3);
            examineResults("Flask import", results1);
            
            // Test 2: Search for route decorators  
            System.out.println("🐍 Test 2: Searching for 'app.route'");
            var results2 = searchService.search("app.route", 3);
            examineResults("app.route", results2);
            
            // Test 3: Search for function definitions
            System.out.println("🐍 Test 3: Searching for 'def function'");
            var results3 = searchService.search("def function", 3);
            examineResults("def function", results3);
            
            System.out.println("🔍 INVESTIGATION COMPLETE");
            System.out.println();
            System.out.println("💡 Key observations to look for:");
            System.out.println("   • Are line numbers showing the actual matching lines?");
            System.out.println("   • Are Python files consistently ranked first?");
            System.out.println("   • Are the displayed code snippets relevant to the search?");
            System.out.println("   • Do line numbers match the actual file content?");
            
        } catch (Exception e) {
            System.err.println("❌ Investigation failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
    
    private static void examineResults(String searchTerm, java.util.List<sg.edu.nus.iss.codebase.indexer.service.HybridSearchService.SearchResult> results) {
        System.out.printf("📊 Found %d results for '%s':%n", results.size(), searchTerm);
        
        for (int i = 0; i < Math.min(3, results.size()); i++) {
            var result = results.get(i);
            String filename = result.getFileName();
            boolean isPython = filename.endsWith(".py");
            String indicator = isPython ? "🐍" : "📄";
            
            System.out.printf("   %d. %s %s (Score: %.3f)%n", 
                i + 1, indicator, filename, result.getRelevanceScore());
            
            // Examine line matches if available
            var lineMatches = result.getLineMatches();
            if (lineMatches != null && !lineMatches.isEmpty()) {
                System.out.println("      🎯 Line matches found:");
                for (int j = 0; j < Math.min(3, lineMatches.size()); j++) {
                    var lineMatch = lineMatches.get(j);
                    System.out.printf("         Line %d: %s%n", 
                        lineMatch.getLineNumber(), 
                        lineMatch.getLineContent().substring(0, Math.min(80, lineMatch.getLineContent().length())));
                }
            } else {
                System.out.println("      ⚠️ No line matches found");
                // Show content preview instead
                String content = result.getContent();
                if (content != null && content.length() > 0) {
                    String preview = content.substring(0, Math.min(100, content.length())).replace("\n", " ");
                    System.out.printf("      📝 Content preview: %s...%n", preview);
                }
            }
            
            // Check if this is the highest ranked result and if it's Python
            if (i == 0) {
                if (isPython) {
                    System.out.println("      ✅ Python file correctly ranked first");
                } else {
                    System.out.println("      ❌ Non-Python file ranked first - need stronger prioritization");
                }
            }
        }
        System.out.println();
    }
}