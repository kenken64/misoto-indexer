package sg.edu.nus.iss.codebase.indexer.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.CreateCollection;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Collections.CollectionInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;
import org.slf4j.LoggerFactory;

@Component
public class QdrantCollectionInitializer implements CommandLineRunner {

    @Autowired
    private QdrantClient qdrantClient;    @Value("${spring.ai.vectorstore.qdrant.collection-name:codebase-index}")
    private String collectionName;    @Override
    public void run(String... args) throws Exception {
        // Collection initialization is now handled dynamically by IndexingService
        // based on the directory being indexed
        System.out.println("📋 Dynamic collection initialization enabled - collections will be created as needed");
    }private void initializeCollection() {
        // Temporarily suppress all gRPC and Qdrant logging during initialization
        suppressLogging();
        
        try {
            System.out.println("🔧 Checking Qdrant collection: " + collectionName);
            
            // Check if collection exists
            boolean collectionExists = checkCollectionExists();
            
            if (!collectionExists) {
                System.out.println("🆕 Creating Qdrant collection: " + collectionName);
                createCollection();
                System.out.println("✅ Qdrant collection created successfully: " + collectionName);
            } else {
                System.out.println("✅ Qdrant collection already exists: " + collectionName);
            }
            
        } catch (Exception e) {
            System.err.println("⚠️ Warning: Could not initialize Qdrant collection: " + e.getMessage());
            System.err.println("💡 Vector indexing will be skipped, but file-based search will still work");
        } finally {
            // Restore normal logging
            restoreLogging();
        }
    }
    
    private void suppressLogging() {
        // Suppress gRPC and Qdrant loggers programmatically
        setLoggerLevel("io.grpc", Level.OFF);
        setLoggerLevel("io.qdrant", Level.OFF);
        setLoggerLevel("io.netty", Level.OFF);
        setLoggerLevel("grpc", Level.OFF);
    }
    
    private void restoreLogging() {
        // Restore to WARN level (as per application.properties)
        setLoggerLevel("io.grpc", Level.WARN);
        setLoggerLevel("io.qdrant", Level.WARN);
        setLoggerLevel("io.netty", Level.WARN);
        setLoggerLevel("grpc", Level.WARN);
    }
    
    private void setLoggerLevel(String loggerName, Level level) {
        try {
            Logger logger = (Logger) LoggerFactory.getLogger(loggerName);
            logger.setLevel(level);
        } catch (Exception e) {
            // Ignore any errors setting log levels
        }
    }    private boolean checkCollectionExists() {
        try {
            // Suppress the gRPC error logging for this specific call
            CollectionInfo info = qdrantClient.getCollectionInfoAsync(collectionName).get();
            return info != null;
        } catch (java.util.concurrent.ExecutionException e) {
            // Check if the error message indicates collection doesn't exist
            String errorMessage = e.getMessage();
            if (errorMessage != null && 
                (errorMessage.contains("NOT_FOUND") || errorMessage.contains("doesn't exist"))) {
                // Collection doesn't exist, this is expected
                return false;
            }
            // For other types of errors, still return false but don't log
            return false;
        } catch (Exception e) {
            // Check for collection not found in any exception
            String errorMessage = e.getMessage();
            if (errorMessage != null && 
                (errorMessage.contains("NOT_FOUND") || errorMessage.contains("doesn't exist"))) {
                return false;
            }
            // For any other exception, return false
            return false;
        }
    }private void createCollection() throws Exception {
        // Create vector parameters for embeddings
        // Default dimension for CodeLlama embeddings is 4096
        VectorParams vectorParams = VectorParams.newBuilder()
            .setSize(4096)  // CodeLlama embedding dimension
            .setDistance(Distance.Cosine)
            .build();

        // Create collection request
        CreateCollection createCollection = CreateCollection.newBuilder()
            .setCollectionName(collectionName)
            .setVectorsConfig(
                io.qdrant.client.grpc.Collections.VectorsConfig.newBuilder()
                    .setParams(vectorParams)
                    .build()
            )
            .build();

        // Execute collection creation
        qdrantClient.createCollectionAsync(createCollection).get();
    }
}
