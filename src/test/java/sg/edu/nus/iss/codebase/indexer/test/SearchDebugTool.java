package sg.edu.nus.iss.codebase.indexer.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import sg.edu.nus.iss.codebase.indexer.service.HybridSearchService;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileIndexingService;
import sg.edu.nus.iss.codebase.indexer.service.HybridSearchService.SearchResult;

import java.util.List;

@SpringBootApplication
@ComponentScan(basePackages = "sg.edu.nus.iss.codebase.indexer")
public class SearchDebugTool {

    public static void main(String[] args) {
        System.setProperty("logging.level.io.qdrant", "ERROR");
        System.setProperty("logging.level.io.grpc", "ERROR");
        
        ConfigurableApplicationContext context = SpringApplication.run(SearchDebugTool.class, args);
        
        try {
            HybridSearchService searchService = context.getBean(HybridSearchService.class);
            FileIndexingService fileIndexingService = context.getBean(FileIndexingService.class);
            
            System.out.println("=== SEARCH DEBUG INFORMATION ===");
            System.out.println("Current indexing directory: " + fileIndexingService.getCurrentIndexingDirectory());
            System.out.println("Current collection name: " + fileIndexingService.getCurrentCollectionName());
            
            System.out.println("\n=== SEARCHING FOR 'gradient_accumulation_steps' ===");
            List<SearchResult> results = searchService.search("gradient_accumulation_steps", 5);
            
            System.out.println("Found " + results.size() + " results:");
            for (int i = 0; i < results.size(); i++) {
                SearchResult result = results.get(i);
                System.out.println("\nResult " + (i + 1) + ":");
                System.out.println("  File: " + result.getFilePath());
                System.out.println("  Collection: " + result.getMetadata().get("collection"));
                System.out.println("  Score: " + result.getScore());
                System.out.println("  Content Preview: " + result.getContent().substring(0, Math.min(200, result.getContent().length())));
                
                // Check if this content contains the specific value we're looking for
                if (result.getContent().contains("gradient_accumulation_steps = 4")) {
                    System.out.println("  *** CONTAINS CORRECT VALUE (4) ***");
                } else if (result.getContent().contains("gradient_accumulation_steps = 1")) {
                    System.out.println("  *** CONTAINS INCORRECT VALUE (1) ***");
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            context.close();
        }
    }
}
