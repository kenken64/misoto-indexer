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

@Component
public class QdrantCollectionInitializer implements CommandLineRunner {

    @Autowired
    private QdrantClient qdrantClient;

    @Value("${spring.ai.vectorstore.qdrant.collection-name:codebase-index}")
    private String collectionName;

    @Override
    public void run(String... args) throws Exception {
        initializeCollection();
    }

    private void initializeCollection() {
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
        }
    }

    private boolean checkCollectionExists() {
        try {
            CollectionInfo info = qdrantClient.getCollectionInfoAsync(collectionName).get();
            return info != null;
        } catch (Exception e) {
            return false;
        }
    }    private void createCollection() throws Exception {
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
