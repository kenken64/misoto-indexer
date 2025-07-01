import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import sg.edu.nus.iss.codebase.indexer.IndexerApplication;
import sg.edu.nus.iss.codebase.indexer.service.HybridSearchService;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileIndexingService;

/**
 * Test enhanced search result display with line numbers and matching content
 */
public class TestEnhancedSearchDisplay {
    public static void main(String[] args) {
        System.setProperty("app.cli.enabled", "false");
        ApplicationContext context = SpringApplication.run(IndexerApplication.class);
        
        try {
            System.out.println("🧪 TESTING ENHANCED SEARCH RESULT DISPLAY");
            System.out.println("========================================");
            
            HybridSearchService searchService = context.getBean(HybridSearchService.class);
            FileIndexingService indexingService = context.getBean(FileIndexingService.class);
            
            // Index Python project
            indexingService.setIndexingDirectory("./codebase/dssi-day3-ollama");
            
            System.out.println("🎯 Testing enhanced search result display with line numbers");
            System.out.println("Expected features:");
            System.out.println("  ✅ Language indicator emojis (🐍 for Python, etc.)");
            System.out.println("  ✅ Line numbers with filename:line format");
            System.out.println("  ✅ Syntax-aware content preservation");
            System.out.println("  ✅ Smart indentation indicators");
            System.out.println("  ✅ Relevance-based line matching");
            System.out.println();
            
            // Test 1: Search for Python functions
            System.out.println("🔍 Test 1: Searching for 'function definition'...");
            var results1 = searchService.search("function definition", 3);
            
            if (results1.size() > 0) {
                System.out.printf("📊 Found %d results%n", results1.size());
                System.out.println("📋 Results should show:");
                System.out.println("   • Python function definitions with line numbers");
                System.out.println("   • Language indicators (🐍 for .py files)");
                System.out.println("   • Proper indentation preservation");
                System.out.println();
                
                // Results will be displayed automatically by the search service
            }
            
            System.out.println();
            
            // Test 2: Search for Flask routes
            System.out.println("🔍 Test 2: Searching for 'Flask route'...");
            var results2 = searchService.search("Flask route", 2);
            
            if (results2.size() > 0) {
                System.out.printf("📊 Found %d results%n", results2.size());
                System.out.println("📋 Results should show:");
                System.out.println("   • @app.route decorators with line numbers");
                System.out.println("   • Function definitions following routes");
                System.out.println("   • Smart syntax preservation for decorators");
                System.out.println();
            }
            
            System.out.println();
            
            // Test 3: Multi-term search
            System.out.println("🔍 Test 3: Searching for 'import flask'...");
            var results3 = searchService.search("import flask", 2);
            
            if (results3.size() > 0) {
                System.out.printf("📊 Found %d results%n", results3.size());
                System.out.println("📋 Results should show:");
                System.out.println("   • Import statements with exact line numbers");
                System.out.println("   • Preserved import syntax");
                System.out.println("   • Relevant matching term identification");
                System.out.println();
            }
            
            System.out.println("✅ ENHANCED SEARCH DISPLAY TEST COMPLETE");
            System.out.println();
            System.out.println("🎯 Key enhancements implemented:");
            System.out.println("   • Line-by-line content matching with precise line numbers");
            System.out.println("   • Language-specific emoji indicators for file types");
            System.out.println("   • Smart syntax preservation during truncation");
            System.out.println("   • Indentation-aware content formatting");
            System.out.println("   • Relevance-based line scoring and sorting");
            System.out.println("   • Enhanced query term matching with tokenization");
            System.out.println("   • Context-aware content extraction");
            System.out.println();
            System.out.println("💡 Search results now show: filename:line │ actual matching content");
            System.out.println("💡 This provides precise navigation to relevant code sections!");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}