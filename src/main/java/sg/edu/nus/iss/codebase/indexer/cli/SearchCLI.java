package sg.edu.nus.iss.codebase.indexer.cli;

import sg.edu.nus.iss.codebase.indexer.dto.SearchRequest;
import sg.edu.nus.iss.codebase.indexer.service.HybridSearchService;
import sg.edu.nus.iss.codebase.indexer.service.FileSearchService;
import sg.edu.nus.iss.codebase.indexer.util.ScoreFormatter;
import sg.edu.nus.iss.codebase.indexer.config.DynamicVectorStoreFactory;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileIndexingService;
import sg.edu.nus.iss.codebase.indexer.model.IndexingStatus;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;

@Component
@ConditionalOnProperty(name = "app.cli.enabled", havingValue = "true", matchIfMissing = true)
public class SearchCLI implements CommandLineRunner {
    @Autowired
    private HybridSearchService hybridSearchService;
    
    @Autowired
    private DynamicVectorStoreFactory vectorStoreFactory;
    
    @Autowired
    private FileIndexingService fileIndexingService;

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

            // NOTE: setIndexingDirectory already starts indexing automatically through
            // setIndexingDirectoryWithCollection, so we don't need to start it again

            // Add a small delay to let indexing messages complete, then clear screen
            System.out.println("[WAIT] Waiting 3 seconds for indexing to initialize...");
            try {
                Thread.sleep(3000); // Wait 3 seconds for indexing status messages
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("[CLEAR] Clearing screen and showing interface...");
            clearScreen();
        }

        System.out.println("[DISPLAY] Showing welcome message...");
        displayWelcomeMessage();
        System.out.println("[LOOP] Starting main loop...");
        mainLoop();
    }

    private void displayWelcomeMessage() {
        printAsciiArt();
        System.out.println("===============================================================");
        System.out.println("                   >> HYBRID INDEXING <<                   ");
        System.out.println("===============================================================");
        System.out.println();

        // Show indexing status
        displayIndexingStatus();
    }

    private void displayIndexingStatus() {
        try {
            IndexingStatus status = hybridSearchService.getIndexingStatus();

            if (status == null) {
                System.out.println("[STATUS] Unable to retrieve indexing status");
                return;
            }

            if (status.isIndexingComplete()) {
                System.out.println("[DONE] Indexing Complete: " + status.getIndexedFiles() + " files indexed");
            } else if (status.isIndexingInProgress()) {
                System.out.printf("[PROGRESS] Indexing in Progress: %d/%d files (%.1f%%) - Search available%n",
                        status.getIndexedFiles(), status.getTotalFiles(), status.getProgress());
            } else {
                System.out.println("[READY] Ready to start indexing");
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Error retrieving indexing status: " + e.getMessage());
        }
        System.out.println();
    }

    private void mainLoop() {
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
                case 7 -> performDirectQdrantQuery();
                case 8 -> displayHelp();
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
    }

    private void displayMenu() {
        System.out.println("+--------------------- SEARCH MENU ---------------------+");
        System.out.println("| 1. [>] Search with Natural Language Prompt            |");
        System.out.println("| 2. [i] Indexing Status                                |");
        System.out.println("| 3. [S] Semantic Code Search                           |");
        System.out.println("| 4. [T] Text Search                                    |");
        System.out.println("| 5. [A] Advanced Search                                |");
        System.out.println("| 6. [I] Index Codebase                                 |");
        System.out.println("| 7. [Q] Direct Qdrant Vector Query                     |");
        System.out.println("| 8. [?] Help                                           |");
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
            IndexingStatus status = hybridSearchService.getIndexingStatus();

            if (status == null) {
                System.out.println("ERROR: Unable to retrieve indexing status");
                return;
            }

            System.out.println("[PROGRESS] INDEXING PROGRESS:");
            System.out.println("-".repeat(40));
            System.out.printf("[DONE] Files Indexed: %d%n", status.getIndexedFiles());
            System.out.printf("[PENDING] Pending Files: %d%n",
                    Math.max(0, status.getTotalFiles() - status.getIndexedFiles()));
            System.out.printf("[TOTAL] Total Files: %d%n", status.getTotalFiles());
            System.out.printf("[PROGRESS] Progress: %.1f%% - %s%n", status.getProgress(),
                    status.isIndexingInProgress() ? "In Progress" : (status.isIndexingComplete() ? "Complete" : "Ready"));
            System.out.println("\n[TIME] TIMING INFORMATION:");
            System.out.println("-".repeat(40));

            if (status.isIndexingInProgress()) {
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

            } else if (status.isIndexingComplete()) {
                try {
                    long totalDuration = indexingService.getTotalIndexingDuration();
                    System.out.printf("[DONE] Total Duration: %s%n", formatDuration(totalDuration));
                    System.out.printf("[SPEED] Average Speed: %.1f files/sec%n", indexingService.getIndexingSpeed());
                } catch (Exception e) {
                    System.out.println("[ERROR] Could not retrieve completion timing: " + e.getMessage());
                }
            } else {
                System.out.println("[READY] Indexing not started yet");
            }
            System.out.println("\n[THREADS] VIRTUAL THREAD METRICS:");
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
            }
            System.out.println("\n[FILES] FILE TYPE BREAKDOWN:");
            System.out.println("-".repeat(40));
            try {
                var fileTypeStats = indexingService.getFileTypeStatistics();
                if (fileTypeStats == null || fileTypeStats.isEmpty()) {
                    System.out.println("[INFO] No files indexed yet");
                } else {
                    fileTypeStats.forEach(
                            (type, count) -> System.out.printf("[FILES] %s: %d files%n", type, count));
                }
            } catch (Exception e) {
                System.out.println("[ERROR] Could not retrieve file type statistics: " + e.getMessage());
            }
            System.out.println("\n[SKIP] SKIPPED FILE EXTENSIONS:");
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
                }
            } catch (Exception e) {
                System.out.println("[ERROR] Could not retrieve skipped extensions: " + e.getMessage());
            }
            System.out.println("\n[ERRORS] ERROR SUMMARY:");
            System.out.println("-".repeat(40));
            try {
                System.out.printf("[FAIL] Failed Files: %d%n", indexingService.getFailedFileCount());
            } catch (Exception e) {
                System.out.println("[ERROR] Could not retrieve failed file count: " + e.getMessage());
            }
            try {
                System.out.printf("[SKIP] Skipped Files: %d%n", indexingService.getSkippedFileCount());
            } catch (Exception e) {
                System.out.println("[ERROR] Could not retrieve skipped file count: " + e.getMessage());
            }

            if (status.isIndexingInProgress()) {
                System.out.println("\n[NOTE] Indexing is still in progress. Statistics will continue updating.");
            }
        } catch (Exception e) {
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
        System.out.printf("📊 Total results: %d%n%n", result.getTotalResults()); // Display vector search results
        if (!result.getVectorResults().isEmpty()) {
            System.out.println("🎯 VECTOR SEARCH RESULTS:");
            System.out.println("-".repeat(40));
            for (int i = 0; i < result.getVectorResults().size(); i++) {
                HybridSearchService.SearchResult vResult = result.getVectorResults().get(i);
                System.out.printf("%d. 📄 %s (Score: %s)%n", 
                        i + 1, vResult.getFileName(), ScoreFormatter.formatScoreCompact(vResult.getRelevanceScore()));
                System.out.printf("   📁 %s%n", vResult.getFilePath());
                System.out.printf("   📦 Collection: %s%n", vResult.getCollectionName());
                System.out.printf("   📅 Modified: %s%n", vResult.getLastModifiedDate());
                System.out.printf("   🔍 Indexed: %s%n", vResult.getIndexedAt());
                System.out.printf("   📏 Size: %s bytes%n", vResult.getFileSize());

                // Display enhanced line matches if available
                if (!vResult.getLineMatches().isEmpty()) {
                    System.out.println("   🎯 Matching Content:");
                    // Convert HybridSearchService.LineMatch to FileSearchService.LineMatch
                    List<FileSearchService.LineMatch> convertedMatches = vResult.getLineMatches().stream()
                        .map(match -> new FileSearchService.LineMatch(
                            match.getLineNumber(),
                            match.getLineContent(),
                            match.getMatchedTerm()
                        ))
                        .collect(Collectors.toList());
                    displayEnhancedLineMatches(convertedMatches, vResult.getFileName());
                } else {
                    System.out.printf("   📝 Content Preview: %s%n", truncateContent(vResult.getContent(), 200));
                }
                System.out.println();
            }
        } // Display file search results
        if (!result.getFileResults().isEmpty()) {
            System.out.println("📂 FILE SEARCH RESULTS:");
            System.out.println("-".repeat(40));
            for (int i = 0; i < Math.min(5, result.getFileResults().size()); i++) {
                FileSearchService.SearchResult fResult = result.getFileResults().get(i);
                System.out.printf("%d. 📄 %s (Score: %s)%n",
                        i + 1, fResult.getFileName(), ScoreFormatter.formatScoreCompact(fResult.getRelevanceScore()));
                System.out.printf("   📁 %s%n", fResult.getFilePath());

                // Display enhanced line matches if available
                if (!fResult.getLineMatches().isEmpty()) {
                    System.out.println("   🎯 Matching Content:");
                    displayEnhancedLineMatches(fResult.getLineMatches(), fResult.getFileName());
                } else {
                    System.out.printf("   📝 Content Preview: %s%n", truncateContent(fResult.getContent(), 150));
                }
                System.out.println();
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

        // Offer detailed score analysis
        offerDetailedScoreAnalysis(result);

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
        System.out.println("  7. Direct Qdrant Query: Send raw vector queries to database");
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

        System.out.println("🔧 DIRECT QDRANT QUERY:");
        System.out.println("  • Send raw vector queries directly to Qdrant database");
        System.out.println("  • Bypass all processing layers for debugging");
        System.out.println("  • View raw document metadata and content");
        System.out.println("  • Useful for troubleshooting vector storage issues");
        System.out.println("  • Shows current collection and indexing status");
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
        IndexingStatus status = hybridSearchService.getIndexingStatus();
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

        System.out.printf("🎯 Using alternative ranking system (threshold: %.2f bypassed)%n", threshold);
        System.out.printf("📊 Found %d results%n%n", result.getTotalResults());

        if (!result.getVectorResults().isEmpty()) {
            // Check if this is an endpoint discovery query
            if (isEndpointDiscoveryQuery(result)) {
                displayEndpointAnalysis(result.getVectorResults());
            } else {
                // Standard semantic search results
                for (int i = 0; i < result.getVectorResults().size(); i++) {
                    HybridSearchService.SearchResult vResult = result.getVectorResults().get(i);
                    System.out.printf("%d. 📄 %s (similarity: %.2f)%n",
                            i + 1, vResult.getFileName(), vResult.getScore());
                    System.out.printf("   📁 %s%n", vResult.getFilePath());
                    System.out.printf("   📝 %s%n%n", truncateContent(vResult.getContent(), 200));
                }
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

                // Display line matches if available
                if (!fileResult.getLineMatches().isEmpty()) {
                    System.out.println("   🎯 Line Matches:");
                    for (FileSearchService.LineMatch lineMatch : fileResult.getLineMatches()) {
                        System.out.printf("      Line %d: %s%n", lineMatch.getLineNumber(), lineMatch.getLineContent());
                    }
                } else {
                    System.out.printf("   📝 %s%n", truncateContent(fileResult.getContent(), 150));
                }
                System.out.println();
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
                    System.out.printf("%d. 📄 %s (Score: %s)%n", 
                            i + 1, vResult.getFileName(), ScoreFormatter.formatScoreCompact(vResult.getRelevanceScore()));
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
                    System.out.printf("%d. 📄 %s (Score: %s)%n",
                            i + 1, fResult.getFileName(), ScoreFormatter.formatScoreCompact(fResult.getRelevanceScore()));
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

        // Offer detailed score analysis
        offerDetailedScoreAnalysis(result);

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

    /**
     * Display detailed score analysis for search results
     */
    private void displayDetailedScores(HybridSearchService.HybridSearchResult result) {
        System.out.println("\n📊 DETAILED SCORE ANALYSIS:");
        System.out.println("=".repeat(50));
        
        if (!result.getVectorResults().isEmpty()) {
            System.out.println("🎯 Vector Search Scores:");
            for (int i = 0; i < Math.min(5, result.getVectorResults().size()); i++) {
                HybridSearchService.SearchResult vResult = result.getVectorResults().get(i);
                System.out.printf("  %d. %s%n", i + 1, vResult.getFileName());
                System.out.printf("     %s%n", ScoreFormatter.formatScoreDetailed(vResult.getRelevanceScore(), "Vector"));
                System.out.printf("     %s %s%n", 
                        ScoreFormatter.createScoreBar(vResult.getRelevanceScore(), 20),
                        ScoreFormatter.formatScoreAsPercentage(vResult.getRelevanceScore()));
                System.out.println();
            }
        }
        
        if (!result.getFileResults().isEmpty()) {
            System.out.println("📝 File Search Scores:");
            for (int i = 0; i < Math.min(5, result.getFileResults().size()); i++) {
                FileSearchService.SearchResult fResult = result.getFileResults().get(i);
                System.out.printf("  %d. %s%n", i + 1, fResult.getFileName());
                System.out.printf("     %s%n", ScoreFormatter.formatScoreDetailed(fResult.getRelevanceScore(), "Text"));
                System.out.printf("     %s %s%n", 
                        ScoreFormatter.createScoreBar(fResult.getRelevanceScore(), 20),
                        ScoreFormatter.formatScoreAsPercentage(fResult.getRelevanceScore()));
                System.out.println();
            }
        }
        
        System.out.println("=".repeat(50));
    }

    /**
     * Ask user if they want to see detailed score analysis
     */
    private void offerDetailedScoreAnalysis(HybridSearchService.HybridSearchResult result) {
        if (result.getTotalResults() > 0) {
            System.out.print("\n🔍 Would you like to see detailed score analysis? (y/n): ");
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.equals("y") || input.equals("yes")) {
                displayDetailedScores(result);
            }
        }
    }

    private void printAsciiArt() {
        // ANSI color codes for beautiful gradient effect
        String RESET = "\033[0m";
        String BOLD = "\033[1m";
        
        // Gradient colors: Purple -> Blue -> Cyan -> Green -> Yellow -> Orange -> Red
        String PURPLE = "\033[35m";
        String BRIGHT_PURPLE = "\033[95m";
        String BLUE = "\033[34m";
        String BRIGHT_BLUE = "\033[94m";
        String CYAN = "\033[36m";
        String BRIGHT_CYAN = "\033[96m";
        String GREEN = "\033[32m";
        String BRIGHT_GREEN = "\033[92m";
        String YELLOW = "\033[33m";
        String BRIGHT_YELLOW = "\033[93m";
        String RED = "\033[31m";
        String BRIGHT_RED = "\033[91m";
        String MAGENTA = "\033[35m";
        String BRIGHT_MAGENTA = "\033[95m";
        
        System.out.println();
        // Beautiful gradient ASCII art with smooth color transitions
        System.out.println(BOLD + BRIGHT_PURPLE + "███╗   ███╗██╗███████╗ ██████╗ ████████╗ ██████╗  " + RESET + "   " + BOLD + BRIGHT_RED + "██╗███╗   ██╗██████╗ ███████╗██╗  ██╗███████╗██████╗ " + RESET);
        System.out.println(BOLD + PURPLE + "████╗ ████║██║██╔════╝██╔═══██╗╚══██╔══╝██╔═══██╗ " + RESET + "   " + BOLD + RED + "██║████╗  ██║██╔══██╗██╔════╝╚██╗██╔╝██╔════╝██╔══██╗" + RESET);
        System.out.println(BOLD + BRIGHT_BLUE + "██╔████╔██║██║███████╗██║   ██║   ██║   ██║   ██║ " + RESET + "   " + BOLD + BRIGHT_YELLOW + "██║██╔██╗ ██║██║  ██║█████╗   ╚███╔╝ █████╗  ██████╔╝" + RESET);
        System.out.println(BOLD + BLUE + "██║╚██╔╝██║██║╚════██║██║   ██║   ██║   ██║   ██║ " + RESET + "   " + BOLD + YELLOW + "██║██║╚██╗██║██║  ██║██╔══╝   ██╔██╗ ██╔══╝  ██╔══██╗" + RESET);
        System.out.println(BOLD + BRIGHT_CYAN + "██║ ╚═╝ ██║██║███████║╚██████╔╝   ██║   ╚██████╔╝ " + RESET + "   " + BOLD + BRIGHT_GREEN + "██║██║ ╚████║██████╔╝███████╗██╔╝ ██╗███████╗██║  ██║" + RESET);
        System.out.println(BOLD + CYAN + "╚═╝     ╚═╝╚═╝╚══════╝ ╚═════╝    ╚═╝    ╚═════╝  " + RESET + "   " + BOLD + GREEN + "╚═╝╚═╝  ╚═══╝╚═════╝ ╚══════╝╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝" + RESET);
        System.out.println();
        System.out.println(BOLD + "                          " + BRIGHT_MAGENTA + "🌈 " + BRIGHT_GREEN + ">.< " + BRIGHT_CYAN + "Intelligent Code Search System " + BRIGHT_MAGENTA + "🌈" + RESET);
        System.out.println();
        
    }

    /**
     * Check if the search results indicate an endpoint discovery query
     */
    private boolean isEndpointDiscoveryQuery(HybridSearchService.HybridSearchResult result) {
        // Count how many results contain endpoint-related content
        int endpointCount = 0;
        int totalResults = result.getVectorResults().size();
        
        for (HybridSearchService.SearchResult searchResult : result.getVectorResults()) {
            String content = searchResult.getContent().toLowerCase();
            if (content.contains("@app.route") || 
                content.contains("@requestmapping") ||
                content.contains("@getmapping") ||
                content.contains("@postmapping") ||
                content.contains("rest api endpoint") ||
                content.contains("route:") ||
                content.contains("endpoint:") ||
                content.contains("api endpoint") ||
                content.contains("app.get") ||
                content.contains("app.post") ||
                content.contains("methods=[") ||
                content.contains("def ") && (content.contains("route") || content.contains("api"))) {
                endpointCount++;
            }
        }
        
        // If more than 60% of results are endpoint-related, treat as endpoint discovery
        return totalResults > 0 && (double) endpointCount / totalResults > 0.6;
    }

    /**
     * Display results in endpoint analysis format
     */
    private void displayEndpointAnalysis(List<HybridSearchService.SearchResult> results) {
        System.out.println("🎯 API ENDPOINTS:");
        System.out.println();
        
        List<EndpointInfo> endpoints = new ArrayList<>();
        List<EndpointInfo> errorHandlers = new ArrayList<>();
        
        // Parse and categorize endpoints
        for (HybridSearchService.SearchResult result : results) {
            List<EndpointInfo> parsed = parseEndpoints(result);
            for (EndpointInfo endpoint : parsed) {
                if (endpoint.isErrorHandler) {
                    errorHandlers.add(endpoint);
                } else {
                    endpoints.add(endpoint);
                }
            }
        }
        
        // Display regular endpoints
        for (int i = 0; i < endpoints.size(); i++) {
            EndpointInfo endpoint = endpoints.get(i);
            System.out.printf("  %d. %s%n", i + 1, endpoint.route);
            System.out.printf("  - Function: %s%n", endpoint.functionName);
            System.out.printf("  - Purpose: %s%n", endpoint.purpose);
            if (endpoint.input != null && !endpoint.input.isEmpty()) {
                System.out.printf("  - Input: %s%n", endpoint.input);
            }
            System.out.printf("  - Returns: %s%n", endpoint.returns);
            System.out.println();
        }
        
        // Display error handlers
        if (!errorHandlers.isEmpty()) {
            System.out.println("🚨 Error Handlers:");
            System.out.println();
            
            for (int i = 0; i < errorHandlers.size(); i++) {
                EndpointInfo handler = errorHandlers.get(i);
                System.out.printf("  %d. %s%n", endpoints.size() + i + 1, handler.route);
                System.out.printf("  - Function: %s%n", handler.functionName);
                System.out.printf("  - Purpose: %s%n", handler.purpose);
                System.out.printf("  - Returns: %s%n", handler.returns);
                System.out.println();
            }
        }
        
        // Display summary
        System.out.println("📊 Summary:");
        System.out.println();
        int totalRoutes = endpoints.size() + errorHandlers.size();
        System.out.printf("  - Total Routes: %d (%d regular endpoints + %d error handlers)%n", 
            totalRoutes, endpoints.size(), errorHandlers.size());
        
        // Count API endpoints (those with /api/ in path)
        long apiEndpoints = endpoints.stream().filter(e -> e.route.contains("/api/")).count();
        System.out.printf("  - API Endpoints: %d%n", apiEndpoints);
        
        // Count by HTTP method
        long getEndpoints = endpoints.stream().filter(e -> e.route.contains("[GET]") || !e.route.contains("methods=")).count();
        long postEndpoints = endpoints.stream().filter(e -> e.route.contains("[POST]") || e.route.contains("methods=['POST']")).count();
        System.out.printf("  - HTTP Methods: GET (%d), POST (%d)%n", getEndpoints, postEndpoints);
        
        // Determine framework
        String framework = "Unknown";
        if (results.stream().anyMatch(r -> r.getContent().contains("@app.route"))) {
            framework = "Flask";
        } else if (results.stream().anyMatch(r -> r.getContent().contains("@RequestMapping"))) {
            framework = "Spring Boot";
        } else if (results.stream().anyMatch(r -> r.getContent().contains("app.get") || r.getContent().contains("app.post"))) {
            framework = "Express.js";
        }
        System.out.printf("  - Framework: %s%n", framework);
    }

    /**
     * Parse endpoint information from search result
     */
    private List<EndpointInfo> parseEndpoints(HybridSearchService.SearchResult result) {
        List<EndpointInfo> endpoints = new ArrayList<>();
        String content = result.getContent();
        String[] lines = content.split("\n");
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            // Flask route detection
            if (line.contains("@app.route")) {
                EndpointInfo endpoint = new EndpointInfo();
                endpoint.route = extractFlaskRoute(line);
                
                // Find function name and purpose
                for (int j = i + 1; j < Math.min(i + 10, lines.length); j++) {
                    String nextLine = lines[j].trim();
                    if (nextLine.startsWith("def ")) {
                        endpoint.functionName = extractFunctionName(nextLine);
                        break;
                    }
                }
                
                // Determine purpose and other details
                endpoint.purpose = inferPurpose(endpoint.route, content);
                endpoint.input = inferInput(content, endpoint.route);
                endpoint.returns = inferReturns(content, endpoint.route);
                endpoint.isErrorHandler = false;
                
                endpoints.add(endpoint);
            }
            
            // Error handler detection
            else if (line.contains("@app.errorhandler")) {
                EndpointInfo handler = new EndpointInfo();
                handler.route = line;
                
                // Find function name
                for (int j = i + 1; j < Math.min(i + 5, lines.length); j++) {
                    String nextLine = lines[j].trim();
                    if (nextLine.startsWith("def ")) {
                        handler.functionName = extractFunctionName(nextLine);
                        break;
                    }
                }
                
                handler.purpose = inferErrorHandlerPurpose(line);
                handler.returns = inferErrorHandlerReturns(line);
                handler.isErrorHandler = true;
                
                endpoints.add(handler);
            }
        }
        
        return endpoints;
    }

    /**
     * Extract Flask route information
     */
    private String extractFlaskRoute(String line) {
        // Extract route path and methods
        String route = line;
        if (line.contains("('") && line.contains("')")) {
            int start = line.indexOf("('") + 2;
            int end = line.indexOf("')", start);
            if (end > start) {
                String path = line.substring(start, end);
                
                // Extract HTTP methods
                if (line.contains("methods=")) {
                    int methodStart = line.indexOf("methods=");
                    String methodPart = line.substring(methodStart);
                    if (methodPart.contains("['POST']")) {
                        route = "@app.route('" + path + "', methods=['POST'])";
                    } else if (methodPart.contains("['GET']")) {
                        route = "@app.route('" + path + "') [GET]";
                    } else {
                        route = "@app.route('" + path + "', " + methodPart.split("\\)")[0] + ")";
                    }
                } else {
                    route = "@app.route('" + path + "') [GET]";
                }
            }
        }
        return route;
    }

    /**
     * Extract function name from def line
     */
    private String extractFunctionName(String line) {
        if (line.startsWith("def ")) {
            int start = 4;
            int end = line.indexOf("(");
            if (end > start) {
                return line.substring(start, end) + "()";
            }
        }
        return "unknown()";
    }

    /**
     * Infer endpoint purpose
     */
    private String inferPurpose(String route, String content) {
        String lowerContent = content.toLowerCase();
        
        if (route.contains("/api/generate-sql")) {
            return "API endpoint to generate SQL from natural language";
        } else if (route.contains("/api/validate-sql")) {
            return "API endpoint to validate SQL syntax";
        } else if (route.contains("/api/status")) {
            return "Check the status of the text-to-SQL service";
        } else if (route.contains("'/'") && !route.contains("/api/")) {
            return "Main page with the text-to-SQL interface";
        } else if (route.contains("/examples")) {
            return "Page with example schemas and queries";
        } else if (lowerContent.contains("render_template")) {
            return "Web page endpoint";
        } else if (lowerContent.contains("jsonify")) {
            return "API endpoint returning JSON response";
        } else {
            return "Application endpoint";
        }
    }

    /**
     * Infer input requirements
     */
    private String inferInput(String content, String route) {
        String lowerContent = content.toLowerCase();
        
        if (route.contains("methods=['POST']")) {
            if (route.contains("/api/generate-sql")) {
                return "JSON with schema and query fields";
            } else if (route.contains("/api/validate-sql")) {
                return "JSON with sql field";
            } else if (lowerContent.contains("request.get_json")) {
                return "JSON data";
            } else {
                return "POST data";
            }
        }
        return null; // GET requests typically don't have input requirements
    }

    /**
     * Infer return type
     */
    private String inferReturns(String content, String route) {
        String lowerContent = content.toLowerCase();
        
        if (lowerContent.contains("render_template")) {
            if (route.contains("'/'")) {
                return "Renders index.html template";
            } else if (route.contains("/examples")) {
                return "Renders examples.html template";
            } else {
                return "Renders HTML template";
            }
        } else if (lowerContent.contains("jsonify")) {
            if (route.contains("/api/generate-sql")) {
                return "JSON with generated SQL, validation status, and response";
            } else if (route.contains("/api/validate-sql")) {
                return "JSON with validation status and message";
            } else if (route.contains("/api/status")) {
                return "JSON with Ollama status, model existence, and timestamp";
            } else {
                return "JSON response";
            }
        } else {
            return "Response data";
        }
    }

    /**
     * Infer error handler purpose
     */
    private String inferErrorHandlerPurpose(String line) {
        if (line.contains("404")) {
            return "Handle 404 Not Found errors";
        } else if (line.contains("500")) {
            return "Handle 500 Internal Server errors";
        } else {
            return "Handle application errors";
        }
    }

    /**
     * Infer error handler returns
     */
    private String inferErrorHandlerReturns(String line) {
        if (line.contains("404")) {
            return "Renders 404.html template with 404 status";
        } else if (line.contains("500")) {
            return "Renders 500.html template with 500 status";
        } else {
            return "Error response";
        }
    }

    /**
     * Helper class to store endpoint information
     */
    private static class EndpointInfo {
        String route;
        String functionName;
        String purpose;
        String input;
        String returns;
        boolean isErrorHandler;
    }

    /**
     * Display enhanced line matches with better formatting and context
     */
    private void displayEnhancedLineMatches(List<FileSearchService.LineMatch> lineMatches, String fileName) {
        if (lineMatches == null || lineMatches.isEmpty()) {
            return;
        }
        
        // Determine file type for syntax highlighting context
        String fileExtension = getFileExtension(fileName);
        String languageIndicator = getLanguageIndicator(fileExtension);
        
        // Group consecutive line numbers for better display
        Map<Integer, String> sortedMatches = new HashMap<>();
        for (FileSearchService.LineMatch match : lineMatches) {
            sortedMatches.put(match.getLineNumber(), match.getLineContent());
        }
        
        // Sort by line number and display
        sortedMatches.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .limit(5) // Show max 5 matches to avoid overwhelming output
            .forEach(entry -> {
                int lineNumber = entry.getKey();
                String content = entry.getValue();
                
                // Format line number with consistent width
                String formattedLineNumber = String.format("%4d", lineNumber);
                
                // Truncate long lines but preserve important parts
                String displayContent = formatLineContent(content, fileExtension);
                
                // Display with language indicator and line reference
                System.out.printf("      %s %s:%s │ %s%n", 
                    languageIndicator, 
                    fileName, 
                    formattedLineNumber, 
                    displayContent);
            });
        
        // Show additional context if there are more matches
        if (lineMatches.size() > 5) {
            System.out.printf("      💡 + %d more matches in this file%n", lineMatches.size() - 5);
        }
    }
    
    /**
     * Get file extension from filename
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
    }
    
    /**
     * Get language indicator emoji based on file extension
     */
    private String getLanguageIndicator(String extension) {
        switch (extension) {
            case ".py": return "🐍";
            case ".java": return "☕";
            case ".js":
            case ".ts": return "🟨";
            case ".jsx":
            case ".tsx": return "⚛️";
            case ".rs": return "🦀";
            case ".go": return "🐹";
            case ".cs": return "🔷";
            case ".cpp":
            case ".cc":
            case ".c": return "⚡";
            case ".php": return "🐘";
            case ".rb": return "💎";
            case ".sh":
            case ".bash": return "🐚";
            case ".sql": return "🗃️";
            case ".yml":
            case ".yaml": return "📄";
            case ".json": return "📋";
            case ".xml": return "🏷️";
            case ".html": return "🌐";
            case ".css": return "🎨";
            case ".md": return "📝";
            default: return "📄";
        }
    }
    
    /**
     * Format line content for better display with context preservation
     */
    private String formatLineContent(String content, String fileExtension) {
        if (content == null) {
            return "";
        }
        
        // Trim whitespace but preserve indentation structure
        String trimmed = content.trim();
        
        // Detect indentation level
        int originalLength = content.length();
        int trimmedLength = trimmed.length();
        int leadingSpaces = originalLength - trimmedLength - (content.length() - content.replaceAll("\\s+$", "").length());
        String indentIndicator = leadingSpaces > 0 ? "→".repeat(Math.min(leadingSpaces / 2, 4)) : "";
        
        // Truncate long lines while preserving key syntax
        if (trimmed.length() > 100) {
            // Try to preserve important parts based on file type
            String truncated = preserveImportantSyntax(trimmed, fileExtension);
            return indentIndicator + truncated + "...";
        }
        
        return indentIndicator + trimmed;
    }
    
    /**
     * Preserve important syntax when truncating based on file type
     */
    private String preserveImportantSyntax(String content, String fileExtension) {
        // For Python files, preserve function definitions, decorators, imports
        if (".py".equals(fileExtension)) {
            if (content.startsWith("def ") || content.startsWith("class ") || 
                content.startsWith("@") || content.startsWith("import ") || 
                content.startsWith("from ")) {
                return content.substring(0, Math.min(80, content.length()));
            }
        }
        
        // For Java files, preserve method signatures, annotations
        if (".java".equals(fileExtension)) {
            if (content.contains("public ") || content.contains("private ") || 
                content.contains("@") || content.contains("class ") || 
                content.contains("interface ")) {
                return content.substring(0, Math.min(80, content.length()));
            }
        }
        
        // For JavaScript/TypeScript, preserve function definitions, exports
        if (".js".equals(fileExtension) || ".ts".equals(fileExtension) || 
            ".jsx".equals(fileExtension) || ".tsx".equals(fileExtension)) {
            if (content.contains("function ") || content.contains("export ") || 
                content.contains("import ") || content.contains("const ") || 
                content.contains("let ")) {
                return content.substring(0, Math.min(80, content.length()));
            }
        }
        
        // Default truncation - preserve start and try to end at word boundary
        String truncated = content.substring(0, Math.min(80, content.length()));
        int lastSpace = truncated.lastIndexOf(' ');
        if (lastSpace > 60) { // Only use word boundary if it's not too early
            truncated = truncated.substring(0, lastSpace);
        }
        return truncated;
    }

    /**
     * Perform direct query to Qdrant vector database
     */
    private void performDirectQdrantQuery() {
        try {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("🔍 DIRECT QDRANT VECTOR DATABASE QUERY");
            System.out.println("=".repeat(80));
            
            // Get current collection information
            String currentCollection = fileIndexingService.getCurrentCollectionName();
            String currentDirectory = fileIndexingService.getCurrentIndexingDirectory();
            
            System.out.println("📊 Current Collection Info:");
            System.out.printf("   • Collection: %s%n", currentCollection);
            System.out.printf("   • Directory: %s%n", currentDirectory);
            System.out.printf("   • Indexed Files: %d%n", fileIndexingService.getIndexedFileCount());
            System.out.println();
            
            // Get query from user
            System.out.print("🔎 Enter your vector search query: ");
            String query = scanner.nextLine().trim();
            
            if (query.isEmpty()) {
                System.out.println("❌ Query cannot be empty.");
                return;
            }
            
            // Get search parameters
            System.out.print("📊 Max results (default 10): ");
            String maxResultsInput = scanner.nextLine().trim();
            int maxResults = maxResultsInput.isEmpty() ? 10 : Integer.parseInt(maxResultsInput);
            
            System.out.print("🎯 Similarity threshold (0.0-1.0, default 0.0): ");
            String thresholdInput = scanner.nextLine().trim();
            double threshold = thresholdInput.isEmpty() ? 0.0 : Double.parseDouble(thresholdInput);
            
            System.out.println("\n⚡ Executing direct vector search...");
            long startTime = System.currentTimeMillis();
            
            // Create vector store for current collection
            VectorStore vectorStore = vectorStoreFactory.createVectorStore(currentCollection);
            
            // Build search request
            org.springframework.ai.vectorstore.SearchRequest searchRequest = 
                org.springframework.ai.vectorstore.SearchRequest.builder()
                    .query(query)
                    .topK(maxResults)
                    .similarityThreshold(threshold)
                    .build();
            
            // Execute direct vector search
            List<Document> results = vectorStore.similaritySearch(searchRequest);
            
            long searchTime = System.currentTimeMillis() - startTime;
            
            // Display results
            displayDirectQdrantResults(results, query, searchTime, threshold);
            
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid number format. Please enter valid numeric values.");
        } catch (Exception e) {
            System.err.println("❌ Error performing direct Qdrant query: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }
    
    /**
     * Display results from direct Qdrant vector search
     */
    private void displayDirectQdrantResults(List<Document> results, String query, long searchTime, double threshold) {
        System.out.println("\n" + "=".repeat(80));
        System.out.printf("🎯 DIRECT VECTOR SEARCH RESULTS (completed in %dms)%n", searchTime);
        System.out.println("=".repeat(80));
        
        System.out.printf("🔍 Query: \"%s\"%n", query);
        System.out.printf("📊 Threshold: %.3f | Found: %d documents%n", threshold, results.size());
        System.out.println();
        
        if (results.isEmpty()) {
            System.out.println("❌ No documents found matching your query.");
            System.out.println("💡 Try:");
            System.out.println("   • Lowering the similarity threshold");
            System.out.println("   • Using different keywords");
            System.out.println("   • Checking if files are properly indexed");
        } else {
            System.out.println("📋 Raw Vector Search Results:");
            System.out.println("-".repeat(40));
            
            for (int i = 0; i < results.size(); i++) {
                Document doc = results.get(i);
                Map<String, Object> metadata = doc.getMetadata();
                
                System.out.printf("%d. 📄 %s%n", i + 1, 
                    metadata.getOrDefault("filename", "Unknown"));
                
                // Display metadata info
                System.out.printf("   📁 Path: %s%n", 
                    metadata.getOrDefault("filepath", "Unknown"));
                System.out.printf("   🏷️  Type: %s%n", 
                    metadata.getOrDefault("documentType", "Unknown"));
                System.out.printf("   📏 Size: %s bytes%n", 
                    metadata.getOrDefault("size", "Unknown"));
                
                // Display chunk info if available
                if (metadata.containsKey("chunk")) {
                    System.out.printf("   🧩 Chunk: %s", metadata.get("chunk"));
                    if (metadata.containsKey("total_chunks")) {
                        System.out.printf(" of %s", metadata.get("total_chunks"));
                    }
                    System.out.println();
                }
                
                // Display content preview
                String content = doc.getText();
                System.out.printf("   📝 Content: %s%n", truncateContent(content, 200));
                
                // Check for Flask/API content
                if (content.contains("@app.route") || content.contains("Flask") || content.contains("/api/")) {
                    System.out.println("   🎯 Contains Flask/API content!");
                }
                
                // Display additional metadata fields
                System.out.println("   🏷️  Metadata fields: " + metadata.keySet());
                
                System.out.println();
            }
        }
        
        System.out.println("=".repeat(80));
        System.out.println("💡 This is a direct vector database query bypassing all processing layers.");
        System.out.println("🔧 Use this to debug vector storage and retrieval issues.");
    }
}
