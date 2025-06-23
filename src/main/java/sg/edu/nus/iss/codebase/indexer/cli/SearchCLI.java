package sg.edu.nus.iss.codebase.indexer.cli;

import sg.edu.nus.iss.codebase.indexer.service.HybridSearchService;
import sg.edu.nus.iss.codebase.indexer.service.FileSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Scanner;

@Component
public class SearchCLI implements CommandLineRunner {

    @Autowired
    private HybridSearchService hybridSearchService;

    private final Scanner scanner = new Scanner(System.in);    @Override
    public void run(String... args) {
        // Check if directory argument is provided
        if (args.length > 0) {
            String directory = args[0];
            System.out.println("📁 Setting indexing directory: " + directory);
            hybridSearchService.setIndexingDirectory(directory);
            System.out.println("🚀 Indexing started in background...");
        }
        
        displayWelcomeMessage();
        mainLoop();
    }

    private void displayWelcomeMessage() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                    MISOTO CODEBASE INDEXER                  ║");
        System.out.println("║                   Intelligent Code Search                   ║");
        System.out.println("║                     🚀 HYBRID INDEXING                       ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        // Show indexing status
        displayIndexingStatus();
    }    private void displayIndexingStatus() {
        HybridSearchService.IndexingStatus status = hybridSearchService.getIndexingStatus();
        
        if (status.isComplete()) {
            System.out.println("✅ Indexing Complete: " + status.getIndexedFiles() + " files indexed");
        } else if (status.isInProgress()) {
            System.out.printf("⏳ Indexing in Progress: %d/%d files (%.1f%%) - Search available%n", 
                status.getIndexedFiles(), status.getTotalFiles(), status.getProgress());
        } else {
            System.out.println("⚡ Ready to start indexing");
        }
        System.out.println();
    }

    private void mainLoop() {
        while (true) {
            displayMenu();            int choice = getChoice();
            switch (choice) {
                case 1 -> performNaturalLanguageSearch();
                case 2 -> displayDetailedIndexingStatus();
                case 6 -> displayHelp();
                case 0 -> {
                    System.out.println("👋 Thank you for using Misoto Codebase Indexer!");
                    return;
                }
                default -> System.out.println("❌ Invalid choice. Please try again.");
            }
        }
    }    private void displayMenu() {
        System.out.println("┌─────────────────── SEARCH MENU ───────────────────┐");
        System.out.println("│ 1. 🤖 Natural Language Search                    │");
        System.out.println("│ 2. 📊 Indexing Status                            │");
        System.out.println("│ 6. ❓ Help                                        │");
        System.out.println("│ 0. 🚪 Exit                                        │");
        System.out.println("└───────────────────────────────────────────────────┘");
        System.out.print("Enter your choice: ");
    }

    private int getChoice() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void performNaturalLanguageSearch() {
        System.out.println("\n=== Natural Language Search ===");
        System.out.println("Enter your search query in natural language:");
        System.out.println("Examples:");
        System.out.println("  - 'Find functions that handle user authentication'");
        System.out.println("  - 'Show me REST API endpoints for user management'");
        System.out.println("  - 'Find classes that implement caching logic'");
        System.out.print("Query: ");
        
        String query = scanner.nextLine().trim();
        if (query.isEmpty()) {
            System.out.println("❌ Query cannot be empty.");
            return;
        }

        System.out.println("🤖 Searching with AI-powered natural language processing...");
        performHybridSearch(query);    }

    private void displayDetailedIndexingStatus() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 DETAILED INDEXING STATUS");
        System.out.println("=".repeat(80));
        
        try {
            // Get indexing service from hybrid search service
            var indexingService = hybridSearchService.getIndexingService();
            
            // Basic status
            HybridSearchService.IndexingStatus status = hybridSearchService.getIndexingStatus();
            
            System.out.println("🔄 INDEXING PROGRESS:");
            System.out.println("-".repeat(40));
            System.out.printf("✅ Files Indexed: %d%n", status.getIndexedFiles());
            System.out.printf("⏳ Pending Files: %d%n", 
                Math.max(0, status.getTotalFiles() - status.getIndexedFiles()));
            System.out.printf("📁 Total Files: %d%n", status.getTotalFiles());
            System.out.printf("📈 Progress: %.1f%%%n", status.getProgress());
            
            System.out.println("\n⏱️ TIMING INFORMATION:");
            System.out.println("-".repeat(40));
            
            if (status.isInProgress()) {
                long currentDuration = indexingService.getCurrentIndexingDuration();
                long estimatedTotal = indexingService.getEstimatedTotalDuration();
                long remaining = Math.max(0, estimatedTotal - currentDuration);
                
                System.out.printf("⏰ Current Duration: %s%n", formatDuration(currentDuration));
                System.out.printf("🎯 Estimated Total: %s%n", formatDuration(estimatedTotal));
                System.out.printf("⏳ Estimated Remaining: %s%n", formatDuration(remaining));
                System.out.printf("🚀 Average Speed: %.1f files/sec%n", indexingService.getIndexingSpeed());
                
            } else if (status.isComplete()) {
                long totalDuration = indexingService.getTotalIndexingDuration();
                System.out.printf("✅ Total Duration: %s%n", formatDuration(totalDuration));
                System.out.printf("⚡ Average Speed: %.1f files/sec%n", indexingService.getIndexingSpeed());
            } else {
                System.out.println("🚀 Indexing not started yet");
            }
            
            System.out.println("\n🧵 VIRTUAL THREAD METRICS:");
            System.out.println("-".repeat(40));
            System.out.printf("🔧 Active Virtual Threads: %d%n", indexingService.getActiveVirtualThreads());
            System.out.printf("📊 Peak Virtual Threads: %d%n", indexingService.getPeakVirtualThreads());
            System.out.printf("⚙️ Total Tasks Executed: %d%n", indexingService.getTotalTasksExecuted());
              System.out.println("\n📁 FILE TYPE BREAKDOWN:");
            System.out.println("-".repeat(40));
            var fileTypeStats = indexingService.getFileTypeStatistics();
            if (fileTypeStats.isEmpty()) {
                System.out.println("📄 No files indexed yet");
            } else {
                fileTypeStats.forEach((type, count) -> 
                    System.out.printf("📄 %s: %d files%n", type, count));
            }
            
            System.out.println("\n⏭️ SKIPPED FILE EXTENSIONS:");
            System.out.println("-".repeat(40));
            var skippedExtensions = indexingService.getSkippedFileExtensions();
            if (skippedExtensions.isEmpty()) {
                System.out.println("✅ No file types were skipped");
            } else {
                skippedExtensions.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(entry -> 
                        System.out.printf("🚫 %s: %d files (not supported)%n", 
                            entry.getKey().isEmpty() ? "[no extension]" : entry.getKey(), 
                            entry.getValue()));
            }
                
            System.out.println("\n⚠️ ERROR SUMMARY:");
            System.out.println("-".repeat(40));
            System.out.printf("❌ Failed Files: %d%n", indexingService.getFailedFileCount());
            System.out.printf("⏭️ Skipped Files: %d%n", indexingService.getSkippedFileCount());
            
            if (status.isInProgress()) {
                System.out.println("\n💡 NOTE: Indexing is still in progress. Statistics will continue updating.");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error retrieving indexing status: " + e.getMessage());
        }
        
        System.out.println("=".repeat(80));
        System.out.println();
    }
    
    private String formatDuration(long milliseconds) {
        if (milliseconds < 0) return "Unknown";
        
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        seconds %= 60;
        minutes %= 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    private void performHybridSearch(String query) {
        long startTime = System.currentTimeMillis();
        
        try {
            HybridSearchService.HybridSearchResult result = hybridSearchService.performHybridSearch(query, 10);
            
            long searchTime = System.currentTimeMillis() - startTime;
            displaySearchResults(result, searchTime);
            
        } catch (Exception e) {
            System.err.println("❌ Search failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void displaySearchResults(HybridSearchService.HybridSearchResult result, long searchTime) {
        System.out.println("\n" + "=".repeat(80));
        System.out.printf("🔍 SEARCH RESULTS (completed in %dms)%n", searchTime);
        System.out.println("=".repeat(80));

        // Show search method used
        if (result.isUsedFallback()) {
            System.out.println("🔄 Used hybrid search (vector + file-based fallback)");
        } else {
            System.out.println("🎯 Used vector-based semantic search");
        }
        System.out.printf("📊 Total results: %d%n%n", result.getTotalResults());

        // Display vector search results
        if (!result.getVectorResults().isEmpty()) {
            System.out.println("🎯 VECTOR SEARCH RESULTS:");
            System.out.println("-".repeat(40));
            for (int i = 0; i < result.getVectorResults().size(); i++) {
                HybridSearchService.SearchResult vResult = result.getVectorResults().get(i);
                System.out.printf("%d. 📄 %s%n", i + 1, vResult.getFileName());
                System.out.printf("   📁 %s%n", vResult.getFilePath());
                System.out.printf("   📝 %s%n%n", truncateContent(vResult.getContent(), 200));
            }
        }

        // Display file search results
        if (!result.getFileResults().isEmpty()) {
            System.out.println("📂 FILE SEARCH RESULTS:");
            System.out.println("-".repeat(40));
            for (int i = 0; i < Math.min(5, result.getFileResults().size()); i++) {
                FileSearchService.SearchResult fResult = result.getFileResults().get(i);
                System.out.printf("%d. 📄 %s (Score: %.1f)%n", 
                    i + 1, fResult.getFileName(), fResult.getRelevanceScore());
                System.out.printf("   📁 %s%n", fResult.getFilePath());
                System.out.printf("   📝 %s%n%n", truncateContent(fResult.getContent(), 150));
            }
        }

        // Display AI analysis
        if (!result.getAiAnalysis().isEmpty()) {
            System.out.println("🤖 AI ANALYSIS:");
            System.out.println("-".repeat(40));
            System.out.println(result.getAiAnalysis());
            System.out.println();
        }

        if (result.getTotalResults() == 0) {
            System.out.println("❌ No results found. Try different search terms or check if indexing is complete.");
        }

        System.out.println("=".repeat(80));
        System.out.println();
    }

    private String truncateContent(String content, int maxLength) {
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }

    private void displayHelp() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("❓ HELP - How to Use Misoto Codebase Indexer");
        System.out.println("=".repeat(60));
        System.out.println();        System.out.println("🎯 SEARCH TYPES:");
        System.out.println("  1. Natural Language: Ask questions in plain English");
        System.out.println("  2. Indexing Status: View detailed indexing progress and metrics");
        System.out.println();
        System.out.println("🚀 HYBRID INDEXING:");
        System.out.println("  • Priority files (controllers, services) indexed first");
        System.out.println("  • Search works immediately with progressive results");
        System.out.println("  • Background indexing continues for complete coverage");
        System.out.println("  • Automatic fallback if vector search is unavailable");
        System.out.println();        System.out.println("💡 TIPS:");
        System.out.println("  • Ask specific questions for better results");
        System.out.println("  • Use natural language to describe what you're looking for");
        System.out.println("  • Check indexing status to monitor virtual thread performance");
        System.out.println("  • The system handles both technical and natural queries");
        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println();
    }
}
