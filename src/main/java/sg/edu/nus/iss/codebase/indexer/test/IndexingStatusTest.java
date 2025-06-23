package sg.edu.nus.iss.codebase.indexer.test;

import sg.edu.nus.iss.codebase.indexer.service.HybridSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Simple test to verify indexing service metrics are working
 */
@Component
public class IndexingStatusTest implements CommandLineRunner {

    @Autowired
    private HybridSearchService hybridSearchService;

    @Override
    public void run(String... args) {
        // Check if we should run the test
        if (args.length > 0 && "test-status".equals(args[0])) {
            testIndexingStatus();
            System.exit(0);
        }
    }

    private void testIndexingStatus() {
        System.out.println("=== TESTING INDEXING STATUS METRICS ===");
        
        try {
            var indexingService = hybridSearchService.getIndexingService();
            
            if (indexingService == null) {
                System.out.println("❌ FAIL: Indexing service is null");
                return;
            }
            
            System.out.println("✅ SUCCESS: Indexing service is available");
            
            // Test basic status
            try {
                var status = hybridSearchService.getIndexingStatus();
                System.out.println("✅ SUCCESS: Basic status retrieved - " + 
                    status.getIndexedFiles() + "/" + status.getTotalFiles() + " files");
            } catch (Exception e) {
                System.out.println("❌ FAIL: Error getting basic status: " + e.getMessage());
            }
            
            // Test individual metrics methods
            try {
                long duration = indexingService.getCurrentIndexingDuration();
                System.out.println("✅ SUCCESS: getCurrentIndexingDuration() = " + duration + "ms");
            } catch (Exception e) {
                System.out.println("❌ FAIL: getCurrentIndexingDuration() error: " + e.getMessage());
            }
            
            try {
                double speed = indexingService.getIndexingSpeed();
                System.out.println("✅ SUCCESS: getIndexingSpeed() = " + speed + " files/sec");
            } catch (Exception e) {
                System.out.println("❌ FAIL: getIndexingSpeed() error: " + e.getMessage());
            }
            
            try {
                int activeThreads = indexingService.getActiveVirtualThreads();
                System.out.println("✅ SUCCESS: getActiveVirtualThreads() = " + activeThreads);
            } catch (Exception e) {
                System.out.println("❌ FAIL: getActiveVirtualThreads() error: " + e.getMessage());
            }
            
            try {
                var fileStats = indexingService.getFileTypeStatistics();
                System.out.println("✅ SUCCESS: getFileTypeStatistics() = " + fileStats.size() + " types");
            } catch (Exception e) {
                System.out.println("❌ FAIL: getFileTypeStatistics() error: " + e.getMessage());
            }
            
            try {
                int failedFiles = indexingService.getFailedFileCount();
                System.out.println("✅ SUCCESS: getFailedFileCount() = " + failedFiles);
            } catch (Exception e) {
                System.out.println("❌ FAIL: getFailedFileCount() error: " + e.getMessage());
            }
            
            System.out.println("=== TEST COMPLETED ===");
            
        } catch (Exception e) {
            System.out.println("❌ MAJOR FAIL: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
