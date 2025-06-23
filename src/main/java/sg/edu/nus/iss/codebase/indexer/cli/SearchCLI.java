package sg.edu.nus.iss.codebase.indexer.cli;

import sg.edu.nus.iss.codebase.indexer.dto.SearchRequest;
import sg.edu.nus.iss.codebase.indexer.service.HybridSearchService;
import sg.edu.nus.iss.codebase.indexer.service.FileSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class SearchCLI implements CommandLineRunner {
    @Autowired
    private HybridSearchService hybridSearchService;

    private final Scanner scanner = new Scanner(System.in);

    /**
     * Cross-platform screen clearing utility
     */
    private void clearScreen() {
        try {
            // Try ANSI escape sequence first (works on most modern terminals)
            System.out.print("\033[2J\033[H");
            System.out.flush();

            // Alternative: Add some blank lines for compatibility
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        } catch (Exception e) {
            // Fallback: Just add blank lines
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }

    @Override
    public void run(String... args) {
        // Check if directory argument is provided
        if (args.length > 0) {
            String directory = args[0];
            System.out.println("[DIR] Setting indexing directory: " + directory);
            hybridSearchService.setIndexingDirectory(directory);
            System.out.println("[START] Indexing started in background...");

            // Add a small delay to let indexing messages complete, then clear screen
            try {
                Thread.sleep(3000); // Wait 3 seconds for indexing status messages
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            clearScreen();
        }

        displayWelcomeMessage();
        mainLoop();
    }

    private void displayWelcomeMessage() {
        System.out.println();
        System.out.println("===============================================================");
        System.out.println("                 MISOTO CODEBASE INDEXER                   ");
        System.out.println("                  Intelligent Code Search                   ");
        System.out.println("                   >> HYBRID INDEXING <<                   ");
        System.out.println("===============================================================");
        System.out.println();

        // Show indexing status
        displayIndexingStatus();
    }

    private void displayIndexingStatus() {
        try {
            HybridSearchService.IndexingStatus status = hybridSearchService.getIndexingStatus();

            if (status == null) {
                System.out.println("[STATUS] Unable to retrieve indexing status");
                return;
            }

            if (status.isComplete()) {
                System.out.println("[DONE] Indexing Complete: " + status.getIndexedFiles() + " files indexed");
            } else if (status.isInProgress()) {
                System.out.printf("[PROGRESS] Indexing in Progress: %d/%d files (%.1f%%) - Search available%n",
                        status.getIndexedFiles(), status.getTotalFiles(), status.getProgress());
            } else {
                System.out.println("[READY] Ready to start indexing");
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Error retrieving indexing status: " + e.getMessage());
        }
        System.out.println();
    }    private void mainLoop() {
        while (true) {
            System.out.println(); // Add spacing before menu
            displayMenu();
            int choice = getChoice();
            switch (choice) {
                case 1 -> performNaturalLanguageSearch();
                case 2 -> displayDetailedIndexingStatus();
                case 3 -> performSemanticCodeSearch();
                case 4 -> performTextSearch();
                case 5 -> performAdvancedSearch();
                case 6 -> startIndexing();
                case 7 -> displayHelp();
                case 0 -> {
                    System.out.println("Thank you for using Misoto Codebase Indexer!");
                    return;
                }
                default -> {
                    System.out.println("[ERROR] Invalid choice. Please try again.");
                    System.out.print("Press Enter to continue...");
                    scanner.nextLine(); // Wait for user to press Enter
                    continue; // Skip the rest of the loop and go back to menu display
                }
            }
        }
    }private void displayMenu() {
        System.out.println("+--------------------- SEARCH MENU ---------------------+");
        System.out.println("| 1. [>] Search with Natural Language Prompt            |");
        System.out.println("| 2. [i] Indexing Status                                |");
        System.out.println("| 3. [S] Semantic Code Search                           |");
        System.out.println("| 4. [T] Text Search                                    |");
        System.out.println("| 5. [A] Advanced Search                                |");
        System.out.println("| 6. [I] Index Codebase                                 |");
        System.out.println("| 7. [?] Help                                           |");
        System.out.println("| 0. [X] Exit                                           |");
        System.out.println("+-------------------------------------------------------+");
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
        performHybridSearch(query);
    }

    private void displayDetailedIndexingStatus() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("[STATUS] DETAILED INDEXING STATUS");
        System.out.println("=".repeat(80));

        try {
            // Get indexing service from hybrid search service
            var indexingService = hybridSearchService.getIndexingService();

            if (indexingService == null) {
                System.out.println("ERROR: Indexing service not available");
                return;
            }

            // Basic status
            HybridSearchService.IndexingStatus status = hybridSearchService.getIndexingStatus();

            if (status == null) {
                System.out.println("ERROR: Unable to retrieve indexing status");
                return;
            }

            System.out.println("[PROGRESS] INDEXING PROGRESS:");
            System.out.println("-".repeat(40));
            System.out.printf("[DONE] Files Indexed: %d%n", status.getIndexedFiles());
            System.out.printf("[PENDING] Pending Files: %d%n",
                    Math.max(0, status.getTotalFiles() - status.getIndexedFiles()));
            System.out.printf("[TOTAL] Total Files: %d%n", status.getTotalFiles());            System.out.printf("[PROGRESS] Progress: %.1f%% - %s%n", status.getProgress(),
                status.isInProgress() ? "In Progress" : (status.isComplete() ? "Complete" : "Ready"));
            System.out.println("\n[TIME] TIMING INFORMATION:");
            System.out.println("-".repeat(40));

            if (status.isInProgress()) {
                try {
                    long currentDuration = indexingService.getCurrentIndexingDuration();
                    long estimatedTotal = indexingService.getEstimatedTotalDuration();
                    long remaining = Math.max(0, estimatedTotal - currentDuration);
                    System.out.printf("[CURRENT] Current Duration: %s%n", formatDuration(currentDuration));
                    System.out.printf("[ESTIMATE] Estimated Total: %s%n", formatDuration(estimatedTotal));
                    System.out.printf("[REMAINING] Estimated Remaining: %s%n", formatDuration(remaining));
                    System.out.printf("[SPEED] Average Speed: %.1f files/sec%n", indexingService.getIndexingSpeed());
                } catch (Exception e) {
                    System.out.println("[ERROR] Could not retrieve timing information: " + e.getMessage());
                }

            } else if (status.isComplete()) {
                try {
                    long totalDuration = indexingService.getTotalIndexingDuration();
                    System.out.printf("[DONE] Total Duration: %s%n", formatDuration(totalDuration));
                    System.out.printf("[SPEED] Average Speed: %.1f files/sec%n", indexingService.getIndexingSpeed());
                } catch (Exception e) {
                    System.out.println("[ERROR] Could not retrieve completion timing: " + e.getMessage());
                }
            } else {
                System.out.println("[READY] Indexing not started yet");
            }            System.out.println("\n[THREADS] VIRTUAL THREAD METRICS:");
            System.out.println("-".repeat(40));
            try {
                System.out.printf("[ACTIVE] Active Virtual Threads: %d%n", indexingService.getActiveVirtualThreads());
            } catch (Exception e) {
                System.out.println("[ERROR] Could not retrieve active threads: " + e.getMessage());
            }
            try {
                System.out.printf("[PEAK] Peak Virtual Threads: %d%n", indexingService.getPeakVirtualThreads());
            } catch (Exception e) {
                System.out.println("[ERROR] Could not retrieve peak threads: " + e.getMessage());
            }
            try {
                System.out.printf("[TASKS] Total Tasks Executed: %d%n", indexingService.getTotalTasksExecuted());
            } catch (Exception e) {
                System.out.println("[ERROR] Could not retrieve total tasks: " + e.getMessage());
            }            System.out.println("\n[FILES] FILE TYPE BREAKDOWN:");
            System.out.println("-".repeat(40));
            try {
                var fileTypeStats = indexingService.getFileTypeStatistics();
                if (fileTypeStats == null || fileTypeStats.isEmpty()) {
                    System.out.println("[INFO] No files indexed yet");
                } else {                    fileTypeStats.forEach(
                            (type, count) -> System.out.printf("[FILES] %s: %d files%n", type, count));
                }            } catch (Exception e) {
                System.out.println("[ERROR] Could not retrieve file type statistics: " + e.getMessage());
            }System.out.println("\n[SKIP] SKIPPED FILE EXTENSIONS:");
            System.out.println("-".repeat(40));
            try {
                var skippedExtensions = indexingService.getSkippedFileExtensions();
                if (skippedExtensions == null || skippedExtensions.isEmpty()) {
                    System.out.println("[OK] No file types were skipped");
                } else {
                    skippedExtensions.entrySet().stream()
                            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                            .forEach(entry -> System.out.printf("[SKIP] %s: %d files (not supported)%n",
                                    entry.getKey().isEmpty() ? "[no extension]" : entry.getKey(),
                                    entry.getValue()));
                }            } catch (Exception e) {
                System.out.println("[ERROR] Could not retrieve skipped extensions: " + e.getMessage());
            }System.out.println("\n[ERRORS] ERROR SUMMARY:");
            System.out.println("-".repeat(40));
            try {
                System.out.printf("[FAIL] Failed Files: %d%n", indexingService.getFailedFileCount());            } catch (Exception e) {
                System.out.println("[ERROR] Could not retrieve failed file count: " + e.getMessage());
            }
            try {
                System.out.printf("[SKIP] Skipped Files: %d%n", indexingService.getSkippedFileCount());
            } catch (Exception e) {
                System.out.println("[ERROR] Could not retrieve skipped file count: " + e.getMessage());
            }

            if (status.isInProgress()) {
                System.out.println("\n[NOTE] Indexing is still in progress. Statistics will continue updating.");
            }        } catch (Exception e) {
            System.err.println("[ERROR] Error retrieving indexing status: " + e.getMessage());
        }

        System.out.println("=".repeat(80));
        System.out.println();
    }

    private String formatDuration(long milliseconds) {
        if (milliseconds < 0)
            return "Unknown";

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
        System.out.println();

        System.out.println("🎯 SEARCH TYPES:");
        System.out.println("  1. Natural Language: Ask questions in plain English");
        System.out.println("  2. Indexing Status: View detailed indexing progress and metrics");
        System.out.println("  3. Semantic Search: Find conceptually similar code using AI");
        System.out.println("  4. Text Search: Fast keyword-based search");
        System.out.println("  5. Advanced Search: Combine multiple criteria for precise results");
        System.out.println("  6. Index Codebase: Manage indexing process and directory");
        System.out.println();

        System.out.println("🧠 SEMANTIC SEARCH TIPS:");
        System.out.println("  • Use conceptual terms: 'authentication', 'database repository'");
        System.out.println("  • Similarity threshold: 0.8-1.0 (exact), 0.6-0.8 (related), 0.4-0.6 (loose)");
        System.out.println("  • Best for finding similar patterns and architectures");
        System.out.println();

        System.out.println("📝 TEXT SEARCH TIPS:");
        System.out.println("  • Use exact terms: '@RestController', 'findByUsername'");
        System.out.println("  • Case-sensitive option for precise matching");
        System.out.println("  • Best for finding specific annotations, method names, classes");
        System.out.println();

        System.out.println("⚙️ ADVANCED SEARCH FEATURES:");
        System.out.println("  • Combine text, semantic, or hybrid search");
        System.out.println("  • Filter by file extensions (.java, .kt, .py)");
        System.out.println("  • Adjust similarity thresholds");
        System.out.println("  • Control result limits");
        System.out.println();

        System.out.println("🚀 HYBRID INDEXING:");
        System.out.println("  • Priority files (controllers, services) indexed first");
        System.out.println("  • Search works immediately with progressive results");
        System.out.println("  • Background indexing continues for complete coverage");
        System.out.println("  • Automatic fallback if vector search is unavailable");
        System.out.println();

        System.out.println("💡 PERFORMANCE TIPS:");
        System.out.println("  • Search is available during indexing");
        System.out.println("  • Check status (option 2) to monitor progress");
        System.out.println("  • Use clean run script for optimal startup");
        System.out.println("  • Virtual threads optimize resource usage");
        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println();
    }

    private void performSemanticCodeSearch() {
        System.out.println("\n╔═══════════════════════════════════════════╗");
        System.out.println("║           🧠 SEMANTIC CODE SEARCH          ║");
        System.out.println("╚═══════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Find conceptually similar code using AI embeddings.");
        System.out.println("Examples:");
        System.out.println("  • 'database repository pattern'");
        System.out.println("  • 'authentication logic'");
        System.out.println("  • 'REST API endpoints'");
        System.out.println();

        System.out.print("🧠 Enter search query: ");
        String query = scanner.nextLine().trim();
        if (query.isEmpty()) {
            System.out.println("❌ Query cannot be empty.");
            return;
        }

        System.out.print("🎯 Similarity threshold (0.0-1.0) [0.7]: ");
        String thresholdInput = scanner.nextLine().trim();
        double threshold = 0.7;
        if (!thresholdInput.isEmpty()) {
            try {
                threshold = Double.parseDouble(thresholdInput);
                if (threshold < 0.0 || threshold > 1.0) {
                    System.out.println("⚠️  Threshold must be between 0.0 and 1.0. Using default 0.7");
                    threshold = 0.7;
                }
            } catch (NumberFormatException e) {
                System.out.println("⚠️  Invalid threshold format. Using default 0.7");
            }
        }

        System.out.print("🔍 Max results [10]: ");
        String maxResultsInput = scanner.nextLine().trim();
        int maxResults = 10;
        if (!maxResultsInput.isEmpty()) {
            try {
                maxResults = Integer.parseInt(maxResultsInput);
                if (maxResults <= 0) {
                    System.out.println("⚠️  Max results must be positive. Using default 10");
                    maxResults = 10;
                }
            } catch (NumberFormatException e) {
                System.out.println("⚠️  Invalid number format. Using default 10");
            }
        }

        System.out.println("\n🔍 Performing semantic search...");
        performSemanticSearch(query, threshold, maxResults);
    }

    private void performTextSearch() {
        System.out.println("\n╔═══════════════════════════════════════════╗");
        System.out.println("║             📝 TEXT SEARCH                 ║");
        System.out.println("╚═══════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Fast keyword-based search across all indexed files.");
        System.out.println("Examples:");
        System.out.println("  • '@RestController'");
        System.out.println("  • 'findByUsername'");
        System.out.println("  • 'implements Serializable'");
        System.out.println();

        System.out.print("📝 Enter search term: ");
        String query = scanner.nextLine().trim();
        if (query.isEmpty()) {
            System.out.println("❌ Search term cannot be empty.");
            return;
        }

        System.out.print("🔍 Case sensitive? [y/N]: ");
        String caseSensitiveInput = scanner.nextLine().trim();
        boolean caseSensitive = caseSensitiveInput.toLowerCase().startsWith("y");

        System.out.print("📊 Max results [20]: ");
        String maxResultsInput = scanner.nextLine().trim();
        int maxResults = 20;
        if (!maxResultsInput.isEmpty()) {
            try {
                maxResults = Integer.parseInt(maxResultsInput);
                if (maxResults <= 0) {
                    System.out.println("⚠️  Max results must be positive. Using default 20");
                    maxResults = 20;
                }
            } catch (NumberFormatException e) {
                System.out.println("⚠️  Invalid number format. Using default 20");
            }
        }

        System.out.println("\n🔍 Performing text search...");
        performTextSearchQuery(query, caseSensitive, maxResults);
    }

    private void performAdvancedSearch() {
        System.out.println("\n╔═══════════════════════════════════════════╗");
        System.out.println("║           ⚙️  ADVANCED SEARCH              ║");
        System.out.println("╚═══════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Combine multiple search criteria for precise results.");
        System.out.println();

        // Build advanced search request
        SearchRequest.SearchRequestBuilder requestBuilder = SearchRequest.builder();

        // Main query
        System.out.print("🔍 Search query: ");
        String query = scanner.nextLine().trim();
        if (query.isEmpty()) {
            System.out.println("❌ Query cannot be empty.");
            return;
        }
        requestBuilder.query(query);

        // Search type
        System.out.println("\n📊 Search Type:");
        System.out.println("  1. Text search");
        System.out.println("  2. Semantic search");
        System.out.println("  3. Hybrid search (recommended)");
        System.out.print("Select type [3]: ");
        String typeInput = scanner.nextLine().trim();
        SearchRequest.SearchType searchType = SearchRequest.SearchType.HYBRID;
        switch (typeInput) {
            case "1" -> searchType = SearchRequest.SearchType.TEXT;
            case "2" -> searchType = SearchRequest.SearchType.SEMANTIC;
            default -> searchType = SearchRequest.SearchType.HYBRID;
        }
        requestBuilder.searchType(searchType);

        // File extensions filter
        System.out.print("\n📁 File extensions (comma-separated, e.g., .java,.kt,.py) [all]: ");
        String extensionsInput = scanner.nextLine().trim();
        if (!extensionsInput.isEmpty()) {
            List<String> extensions = Arrays.asList(extensionsInput.split(","));
            extensions = extensions.stream().map(String::trim).collect(Collectors.toList());
            requestBuilder.fileTypes(extensions);
        }

        // Similarity threshold for semantic search
        if (searchType == SearchRequest.SearchType.SEMANTIC || searchType == SearchRequest.SearchType.HYBRID) {
            System.out.print("🎯 Similarity threshold (0.0-1.0) [0.7]: ");
            String thresholdInput = scanner.nextLine().trim();
            if (!thresholdInput.isEmpty()) {
                try {
                    double threshold = Double.parseDouble(thresholdInput);
                    if (threshold >= 0.0 && threshold <= 1.0) {
                        requestBuilder.threshold(threshold);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("⚠️  Invalid threshold format. Using default 0.7");
                }
            }
        }

        // Max results
        System.out.print("📊 Max results [15]: ");
        String maxResultsInput = scanner.nextLine().trim();
        int maxResults = 15;
        if (!maxResultsInput.isEmpty()) {
            try {
                maxResults = Integer.parseInt(maxResultsInput);
                if (maxResults <= 0) {
                    maxResults = 15;
                }
            } catch (NumberFormatException e) {
                System.out.println("⚠️  Invalid number format. Using default 15");
            }
        }
        requestBuilder.limit(maxResults);

        System.out.println("\n🔍 Performing advanced search...");
        performAdvancedSearchQuery(requestBuilder.build());
    }

    private void startIndexing() {
        System.out.println("\n╔═══════════════════════════════════════════╗");
        System.out.println("║            📚 INDEX CODEBASE               ║");
        System.out.println("╚═══════════════════════════════════════════╝");
        System.out.println();

        System.out.println("📚 Codebase Indexing Options:");
        System.out.println();
        System.out.println("1. 🔄 Restart indexing (current directory)");
        System.out.println("2. 📁 Change indexing directory");
        System.out.println("3. 🗑️  Clear cache and reindex all files");
        System.out.println("4. 📊 View indexing statistics");
        System.out.println("0. 🔙 Back to main menu");
        System.out.println();

        // Show current status
        HybridSearchService.IndexingStatus status = hybridSearchService.getIndexingStatus();
        System.out.printf("Current directory: %s%n", getCurrentIndexingDirectory());
        System.out.printf("Indexed files: %d | Total files: %d%n",
                status.getIndexedFiles(), status.getTotalFiles());
        System.out.println();

        System.out.print("Enter choice [1-4, 0]: ");
        String choiceInput = scanner.nextLine().trim();

        switch (choiceInput) {
            case "1" -> restartIndexing();
            case "2" -> changeIndexingDirectory();
            case "3" -> clearCacheAndReindex();
            case "4" -> displayDetailedIndexingStatus();
            case "0" -> System.out.println("🔙 Returning to main menu...");
            default -> System.out.println("❌ Invalid choice.");
        }
    }

    private void performSemanticSearch(String query, double threshold, int maxResults) {
        long startTime = System.currentTimeMillis();

        try {
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .searchType(SearchRequest.SearchType.SEMANTIC)
                    .threshold(threshold)
                    .limit(maxResults)
                    .build();

            HybridSearchService.HybridSearchResult result = hybridSearchService.performAdvancedSearch(request);
            long searchTime = System.currentTimeMillis() - startTime;

            displaySemanticSearchResults(result, threshold, searchTime);

        } catch (Exception e) {
            System.err.println("❌ Semantic search failed: " + e.getMessage());
        }
    }

    private void performTextSearchQuery(String query, boolean caseSensitive, int maxResults) {
        long startTime = System.currentTimeMillis();

        try {
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .searchType(SearchRequest.SearchType.TEXT)
                    .limit(maxResults)
                    .build();

            // Add case sensitivity to filters if needed
            if (caseSensitive) {
                Map<String, Object> filters = new HashMap<>();
                filters.put("caseSensitive", true);
                request.setFilters(filters);
            }

            HybridSearchService.HybridSearchResult result = hybridSearchService.performAdvancedSearch(request);
            long searchTime = System.currentTimeMillis() - startTime;

            displayTextSearchResults(result, caseSensitive, searchTime);

        } catch (Exception e) {
            System.err.println("❌ Text search failed: " + e.getMessage());
        }
    }

    private void performAdvancedSearchQuery(SearchRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            HybridSearchService.HybridSearchResult result = hybridSearchService.performAdvancedSearch(request);
            long searchTime = System.currentTimeMillis() - startTime;

            displayAdvancedSearchResults(result, request, searchTime);

        } catch (Exception e) {
            System.err.println("❌ Advanced search failed: " + e.getMessage());
        }
    }

    private void displaySemanticSearchResults(HybridSearchService.HybridSearchResult result, double threshold,
            long searchTime) {
        System.out.println("\n" + "=".repeat(80));
        System.out.printf("🧠 SEMANTIC SEARCH RESULTS (completed in %dms)%n", searchTime);
        System.out.println("=".repeat(80));

        System.out.printf("🎯 Similarity threshold: %.2f%n", threshold);
        System.out.printf("📊 Found %d results%n%n", result.getTotalResults());

        if (!result.getVectorResults().isEmpty()) {
            for (int i = 0; i < result.getVectorResults().size(); i++) {
                HybridSearchService.SearchResult vResult = result.getVectorResults().get(i);
                System.out.printf("%d. 📄 %s (similarity: %.2f)%n",
                        i + 1, vResult.getFileName(), vResult.getScore());
                System.out.printf("   📁 %s%n", vResult.getFilePath());
                System.out.printf("   📝 %s%n%n", truncateContent(vResult.getContent(), 200));
            }
        } else {
            System.out.println("❌ No semantic matches found above the threshold.");
            System.out.println("💡 Try lowering the similarity threshold or using different terms.");
        }

        System.out.println("=".repeat(80));
        System.out.println();
    }

    private void displayTextSearchResults(HybridSearchService.HybridSearchResult result, boolean caseSensitive,
            long searchTime) {
        System.out.println("\n" + "=".repeat(80));
        System.out.printf("📝 TEXT SEARCH RESULTS (completed in %dms)%n", searchTime);
        System.out.println("=".repeat(80));

        System.out.printf("🔍 Case sensitive: %s%n", caseSensitive ? "Yes" : "No");
        System.out.printf("📊 Found %d matches in %d files%n%n",
                result.getTotalResults(), result.getVectorResults().size() + result.getFileResults().size());

        // Combine and display results
        List<Object> allResults = new ArrayList<>();
        allResults.addAll(result.getVectorResults());
        allResults.addAll(result.getFileResults());

        for (int i = 0; i < Math.min(allResults.size(), 20); i++) {
            if (allResults.get(i) instanceof HybridSearchService.SearchResult) {
                HybridSearchService.SearchResult searchResult = (HybridSearchService.SearchResult) allResults.get(i);
                System.out.printf("%d. 📄 %s%n", i + 1, searchResult.getFileName());
                System.out.printf("   📁 %s%n", searchResult.getFilePath());
                System.out.printf("   📝 %s%n%n", truncateContent(searchResult.getContent(), 150));
            } else if (allResults.get(i) instanceof FileSearchService.SearchResult) {
                FileSearchService.SearchResult fileResult = (FileSearchService.SearchResult) allResults.get(i);
                System.out.printf("%d. 📄 %s%n", i + 1, fileResult.getFileName());
                System.out.printf("   📁 %s%n", fileResult.getFilePath());
                System.out.printf("   📝 %s%n%n", truncateContent(fileResult.getContent(), 150));
            }
        }

        if (result.getTotalResults() == 0) {
            System.out.println("❌ No matches found.");
            System.out.println("💡 Try different search terms or check if files are indexed.");
        }

        System.out.println("=".repeat(80));
        System.out.println();
    }

    private void displayAdvancedSearchResults(HybridSearchService.HybridSearchResult result, SearchRequest request,
            long searchTime) {
        System.out.println("\n" + "=".repeat(80));
        System.out.printf("⚙️  ADVANCED SEARCH RESULTS (completed in %dms)%n", searchTime);
        System.out.println("=".repeat(80));

        // Display search criteria
        System.out.println("🔍 Search Criteria Applied:");
        System.out.printf("   • Query: \"%s\"%n", request.getQuery());
        System.out.printf("   • Type: %s%n", request.getSearchType());
        if (request.getFileTypes() != null && !request.getFileTypes().isEmpty()) {
            System.out.printf("   • File types: %s%n", String.join(", ", request.getFileTypes()));
        }
        if (request.getThreshold() != null) {
            System.out.printf("   • Similarity threshold: %.2f%n", request.getThreshold());
        }
        System.out.printf("   • Max results: %d%n", request.getLimit());
        System.out.println();

        System.out.printf("📊 Found %d results%n%n", result.getTotalResults());

        // Display results based on search type
        if (request.getSearchType() == SearchRequest.SearchType.SEMANTIC ||
                request.getSearchType() == SearchRequest.SearchType.HYBRID) {

            if (!result.getVectorResults().isEmpty()) {
                System.out.println("🎯 SEMANTIC MATCHES:");
                System.out.println("-".repeat(40));
                for (int i = 0; i < result.getVectorResults().size(); i++) {
                    HybridSearchService.SearchResult vResult = result.getVectorResults().get(i);
                    System.out.printf("%d. 📄 %s", i + 1, vResult.getFileName());
                    if (vResult.getScore() > 0) {
                        System.out.printf(" (%.2f)", vResult.getScore());
                    }
                    System.out.println();
                    System.out.printf("   📁 %s%n", vResult.getFilePath());
                    System.out.printf("   📝 %s%n%n", truncateContent(vResult.getContent(), 180));
                }
            }
        }

        if (request.getSearchType() == SearchRequest.SearchType.TEXT ||
                request.getSearchType() == SearchRequest.SearchType.HYBRID) {

            if (!result.getFileResults().isEmpty()) {
                System.out.println("📝 TEXT MATCHES:");
                System.out.println("-".repeat(40));
                for (int i = 0; i < Math.min(result.getFileResults().size(), 10); i++) {
                    FileSearchService.SearchResult fResult = result.getFileResults().get(i);
                    System.out.printf("%d. 📄 %s (Score: %.1f)%n",
                            i + 1, fResult.getFileName(), fResult.getRelevanceScore());
                    System.out.printf("   📁 %s%n", fResult.getFilePath());
                    System.out.printf("   📝 %s%n%n", truncateContent(fResult.getContent(), 180));
                }
            }
        }

        if (result.getTotalResults() == 0) {
            System.out.println("❌ No results found matching your criteria.");
            System.out.println("💡 Try:");
            System.out.println("   • Broadening your search terms");
            System.out.println("   • Lowering the similarity threshold");
            System.out.println("   • Removing file type filters");
            System.out.println("   • Using a different search type");
        }

        System.out.println("=".repeat(80));
        System.out.println();
    }

    private void restartIndexing() {
        System.out.println("🔄 Restarting indexing...");
        try {
            hybridSearchService.restartIndexing();
            System.out.println("✅ Indexing restarted successfully.");
        } catch (Exception e) {
            System.err.println("❌ Failed to restart indexing: " + e.getMessage());
        }
    }

    private void changeIndexingDirectory() {
        System.out.print("📁 Enter new indexing directory path: ");
        String newDirectory = scanner.nextLine().trim();
        if (!newDirectory.isEmpty()) {
            try {
                hybridSearchService.setIndexingDirectory(newDirectory);
                System.out.println("✅ Indexing directory changed to: " + newDirectory);
                System.out.println("🚀 Starting indexing...");
            } catch (Exception e) {
                System.err.println("❌ Failed to change directory: " + e.getMessage());
            }
        } else {
            System.out.println("❌ Directory path cannot be empty.");
        }
    }

    private void clearCacheAndReindex() {
        System.out.print("⚠️  This will clear all cached data and reindex everything. Continue? [y/N]: ");
        String confirm = scanner.nextLine().trim();
        if (confirm.toLowerCase().startsWith("y")) {
            System.out.println("🗑️  Clearing cache and reindexing...");
            try {
                hybridSearchService.clearCacheAndReindex();
                System.out.println("✅ Cache cleared and reindexing started.");
            } catch (Exception e) {
                System.err.println("❌ Failed to clear cache: " + e.getMessage());
            }
        } else {
            System.out.println("❌ Operation cancelled.");
        }
    }

    private String getCurrentIndexingDirectory() {
        try {
            return hybridSearchService.getCurrentIndexingDirectory();
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
