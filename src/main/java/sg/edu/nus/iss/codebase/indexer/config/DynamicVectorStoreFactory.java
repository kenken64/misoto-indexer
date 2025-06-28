package sg.edu.nus.iss.codebase.indexer.config;

import io.qdrant.client.QdrantClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Factory for creating VectorStore instances with dynamic collection names.
 * This allows us to use different Qdrant collections based on the directory
 * being indexed.
 */
@Component
public class DynamicVectorStoreFactory {

    @Autowired
    private QdrantClient qdrantClient;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Value("${spring.ai.vectorstore.qdrant.host}")
    private String qdrantHost;

    @Value("${spring.ai.vectorstore.qdrant.port}")
    private int qdrantPort;

    @Value("${spring.ai.vectorstore.qdrant.use-tls}")
    private boolean useTls;

    @Value("${spring.ai.vectorstore.qdrant.api-key}")
    private String apiKey;

    /**
     * Create a VectorStore instance for a specific collection name.
     * This allows us to dynamically switch between collections based on the
     * directory being indexed.
     * 
     * @param collectionName The name of the Qdrant collection to use
     * @return A VectorStore instance configured for the specified collection
     */
    public VectorStore createVectorStore(String collectionName) {
        return QdrantVectorStore.builder(qdrantClient, embeddingModel)
                .collectionName(collectionName)
                .build();
    }

    /**
     * Get the default VectorStore instance (uses codebase-index collection).
     * This maintains compatibility with the existing Spring AI configuration.
     */
    public VectorStore getDefaultVectorStore() {
        return createVectorStore("codebase-index");
    }
}
