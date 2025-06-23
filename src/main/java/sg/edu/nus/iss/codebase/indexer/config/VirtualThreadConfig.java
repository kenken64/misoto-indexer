package sg.edu.nus.iss.codebase.indexer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class VirtualThreadConfig implements AsyncConfigurer {    /**
     * Configure Virtual Thread Executor for Indexing Operations
     * 
     * Virtual threads are perfect for I/O-intensive operations like:
     * - File reading
     * - Network calls to Ollama (embedding generation)  
     * - Network calls to Qdrant (vector storage)
     * 
     * Benefits:
     * - Lightweight: Millions of virtual threads possible
     * - Non-blocking: Better resource utilization
     * - Scalable: Automatic scaling based on workload
     */
    @Bean("virtualThreadExecutor")
    public Executor virtualThreadExecutor() {
        return Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());
    }

    /**
     * High-performance executor for background indexing
     * Uses virtual threads for maximum concurrency
     */
    @Bean("indexingExecutor")  
    public Executor indexingExecutor() {
        // Create virtual thread executor with custom naming
        return Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual()
                .name("indexing-worker-", 0)
                .factory()
        );
    }

    /**
     * Configure default async executor to use virtual threads
     */
    @Override
    public Executor getAsyncExecutor() {
        return virtualThreadExecutor();
    }
}
