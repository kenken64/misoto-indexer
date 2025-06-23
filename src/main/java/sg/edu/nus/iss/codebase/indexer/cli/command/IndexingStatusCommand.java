package sg.edu.nus.iss.codebase.indexer.cli.command;

import sg.edu.nus.iss.codebase.indexer.model.IndexingStatus;
import sg.edu.nus.iss.codebase.indexer.service.interfaces.FileIndexingService;

import java.util.Map;

/**
 * Command to display indexing status
 * Implements Command Pattern
 */
public class IndexingStatusCommand implements Command {
    
    private final FileIndexingService indexingService;

    public IndexingStatusCommand(FileIndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @Override
    public void execute() {
        IndexingStatus status = indexingService.getIndexingStatus();
        displayDetailedIndexingStatus(status);
    }

    @Override
    public String getDescription() {
        return "📊 Display detailed indexing status and metrics";
    }

    private void displayDetailedIndexingStatus(IndexingStatus status) {
        System.out.println("\n╔═════════════════ INDEXING STATUS ═════════════════╗");
        
        // Progress information
        System.out.printf("║ 📊 Progress: %,d / %,d files (%.1f%%)%s║%n", 
            status.getIndexedFiles(), status.getTotalFiles(), status.getProgress(),
            " ".repeat(Math.max(0, 15 - String.valueOf(status.getTotalFiles()).length())));
        
        // Timing information
        long durationSeconds = status.getCurrentDuration() / 1000;
        long remainingSeconds = status.getEstimatedRemainingTime() / 1000;
        System.out.printf("║ ⏱️  Duration: %ds | Estimated: %ds remaining%s║%n",
            durationSeconds, remainingSeconds,
            " ".repeat(Math.max(0, 20 - String.valueOf(durationSeconds + remainingSeconds).length())));
        
        // Performance metrics
        System.out.printf("║ 🚀 Speed: %.1f files/second%s║%n", 
            status.getIndexingSpeed(),
            " ".repeat(Math.max(0, 35 - String.format("%.1f", status.getIndexingSpeed()).length())));
        
        // Thread information
        System.out.printf("║ 🧵 Threads: %d active, %d peak%s║%n",
            status.getActiveThreads(), status.getPeakThreads(),
            " ".repeat(Math.max(0, 30 - String.valueOf(status.getActiveThreads() + status.getPeakThreads()).length())));
        
        System.out.println("║                                                   ║");
        
        // File type breakdown
        if (!status.getFileTypeStatistics().isEmpty()) {
            System.out.println("║ 📄 File Types Indexed:                           ║");
            status.getFileTypeStatistics().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> {
                    String line = String.format("║   • %s: %,d files", entry.getKey(), entry.getValue());
                    System.out.printf("%s%s║%n", line, 
                        " ".repeat(Math.max(0, 53 - line.length())));
                });
        }
        
        System.out.println("║                                                   ║");
        
        // Skipped extensions
        if (!status.getSkippedFileExtensions().isEmpty()) {
            StringBuilder skippedLine = new StringBuilder("║ 🚫 Skipped Extensions: ");
            status.getSkippedFileExtensions().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .forEach(entry -> skippedLine.append(entry.getKey())
                    .append(" (").append(entry.getValue()).append("), "));
            
            String line = skippedLine.toString();
            if (line.endsWith(", ")) {
                line = line.substring(0, line.length() - 2);
            }
            System.out.printf("%s%s║%n", line, 
                " ".repeat(Math.max(0, 53 - line.length())));
        }
        
        // Error information
        if (status.getFailedFiles() > 0 || status.getSkippedFiles() > 0) {
            System.out.printf("║ ⚠️  Failed: %d files | Skipped: %d files%s║%n",
                status.getFailedFiles(), status.getSkippedFiles(),
                " ".repeat(Math.max(0, 20 - String.valueOf(status.getFailedFiles() + status.getSkippedFiles()).length())));
        }
        
        System.out.println("╚═══════════════════════════════════════════════════╝");
        
        // Status message
        if (status.isIndexingInProgress()) {
            System.out.println("🔄 Indexing in progress... Press any key to return to menu.");
        } else if (status.isIndexingComplete()) {
            System.out.println("✅ Indexing complete! All files have been processed.");
        } else {
            System.out.println("⏸️ Indexing not started. Use option 6 to begin indexing.");
        }
    }
}
