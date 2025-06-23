package sg.edu.nus.iss.codebase.indexer.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Environment post processor to load .env file before Spring Boot application properties
 */
@Component
public class EnvironmentConfig implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        try {
            // Load .env file from project root
            Dotenv dotenv = Dotenv.configure()
                    .directory(".")  // Look in current directory
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();

            // Create a map of properties from the .env file
            Map<String, Object> envProperties = new HashMap<>();
            dotenv.entries().forEach(entry -> {
                envProperties.put(entry.getKey(), entry.getValue());
            });

            // Add the properties to Spring's environment with high precedence
            if (!envProperties.isEmpty()) {
                environment.getPropertySources().addFirst(
                    new MapPropertySource("dotenv", envProperties)
                );
                System.out.println("✅ Environment variables loaded from .env file");
            }
            
        } catch (Exception e) {
            System.err.println("⚠️  Could not load .env file: " + e.getMessage());
            System.err.println("   Make sure .env file exists in project root with QDRANT_HOST, QDRANT_PORT, and QDRANT_API_KEY");
        }
    }
}
