import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import sg.edu.nus.iss.codebase.indexer.IndexerApplication;
import sg.edu.nus.iss.codebase.indexer.service.HybridSearchService;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileIndexingService;

/**
 * Check what files are actually indexed to understand why app.py is missing
 */
public class CheckIndexedFiles {
    public static void main(String[] args) {
        System.setProperty("app.cli.enabled", "false");
        ApplicationContext context = SpringApplication.run(IndexerApplication.class);
        
        try {
            System.out.println("🔍 CHECKING INDEXED FILES");
            System.out.println("========================");
            
            HybridSearchService searchService = context.getBean(HybridSearchService.class);
            FileIndexingService indexingService = context.getBean(FileIndexingService.class);
            
            // Index Python project
            indexingService.setIndexingDirectory("./codebase/dssi-day3-ollama");
            
            System.out.println("📊 Indexing Status:");
            System.out.printf("   • Directory: %s%n", indexingService.getCurrentIndexingDirectory());
            System.out.printf("   • Collection: %s%n", indexingService.getCurrentCollectionName());
            
            var status = indexingService.getIndexingStatus();
            System.out.printf("   • Indexed files: %d%n", status.getIndexedFiles());
            System.out.printf("   • Total files: %d%n", status.getTotalFiles());
            System.out.printf("   • Progress: %.1f%%%n", status.getProgress());
            System.out.printf("   • Complete: %s%n", status.isIndexingComplete());
            System.out.println();
            
            // Try different searches to see what files are found
            String[] testSearches = {
                "app.py",
                "start_webapp.py", 
                "python",
                "flask",
                "def",
                "@app.route"
            };
            
            for (String searchTerm : testSearches) {
                System.out.printf("🔍 Searching for '%s':%n", searchTerm);
                var results = searchService.search(searchTerm, 5);
                
                if (results.size() > 0) {
                    System.out.printf("   Found %d results:%n", results.size());
                    var uniqueFiles = results.stream()
                        .map(r -> r.getFileName())
                        .distinct()
                        .toList();
                    
                    for (String filename : uniqueFiles) {
                        System.out.printf("     • %s%n", filename);
                    }
                } else {
                    System.out.println("   No results found");
                }
                System.out.println();
            }
            
            System.out.println("💡 ANALYSIS:");
            System.out.println("   • Which Python files are being found?");
            System.out.println("   • Is app.py missing from indexing?");
            System.out.println("   • Are only certain files being indexed?");
            
        } catch (Exception e) {
            System.err.println("❌ Check failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}