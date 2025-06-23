# Misoto Codebase Indexer

An AI-powered terminal application for intelligent code search and indexing using Spring AI and vector databases.

#4. **Configure the Application:**
   ```bash
   # Copy environment template
   cp .env.example .env
   
   # Edit .env file with your cluster details
   QDRANT_HOST=your-cluster-id.qdrant.tech
   QDRANT_API_KEY=your-api-key-here
   ```s

- 🔍 **Natural Language Search**: Search code using plain English queries
- 🧠 **Semantic Search**: Find conceptually similar code using AI embeddings
- 📝 **Text Search**: Traditional keyword-based search
- ⚙️ **Advanced Search**: Filter by file type, language, repository
- 📚 **Intelligent Indexing**: AI-powered code analysis and indexing

## Prerequisites

- Java 17+
- Maven 3.8+
- Ollama (for local AI models)
- Qdrant Cloud cluster (for vector search)

## 🤖 Ollama Model Setup

This application uses specialized AI models for embeddings and chat:

### Required Models:
- **nomic-embed-text**: High-quality embedding model (768 dimensions)
- **codellama:7b**: Code-aware chat model for intelligent analysis

### Quick Setup:
```bash
# Run the setup script (Windows)
setup-models.bat

# Or run manually:
ollama pull nomic-embed-text
ollama pull codellama:7b
```

### Linux/Mac Setup:
```bash
# Make script executable and run
chmod +x setup-models.sh
./setup-models.sh
```

### Why nomic-embed-text?
- **Optimized for text**: Better semantic understanding than code-specific models for embeddings
- **Efficient**: 768-dimensional vectors (vs 4096 for CodeLlama)
- **Fast**: Quicker indexing and search operations
- **Quality**: High-quality embeddings for code and documentation

## ☁️ Qdrant Cloud Setup

1. **Create Qdrant Cloud Account:**
   - Go to [https://cloud.qdrant.io/](https://cloud.qdrant.io/)
   - Sign up for a free account (includes 1GB storage)

2. **Create a Cluster:**
   - Click "Create Cluster"
   - Choose your preferred region
   - Select the free tier
   - Wait for cluster deployment

3. **Get Connection Details:**
   - Copy your cluster URL (e.g., `https://xyz-123.qdrant.tech`)
   - Generate an API key from the dashboard

4. **Update Configuration:**
   ```properties
   # In src/main/resources/application.properties
   spring.ai.vectorstore.qdrant.host=xyz-123.qdrant.tech
   spring.ai.vectorstore.qdrant.api-key=your-generated-api-key
   ```

## 🚀 Quick Start Summary

1. **Install Ollama**
   ```bash
   # Download and install Ollama from https://ollama.ai
   # Or use curl (Linux/macOS):
   curl -fsSL https://ollama.ai/install.sh | sh
   ```

2. **Pull CodeLlama Model**
   ```bash
   ollama pull codellama:7b
   ```

3. **Clone and Build**
   ```bash
   git clone <repository-url>
   cd misoto-indexer
   mvn clean compile
   ```

4. **Configure Environment Variables**
   ```bash
   # Copy the environment template
   cp .env.example .env
   
   # Edit .env with your Qdrant Cloud details
   # Update QDRANT_HOST and QDRANT_API_KEY
   ```

5. **Run the Application**
   ```bash
   mvn spring-boot:run
   ```

## Usage

When you run the application, you'll see an interactive menu:

```
╔══════════════════════════════════════════════════════════════╗
║                    MISOTO CODEBASE INDEXER                  ║
║                   Intelligent Code Search                   ║
╚══════════════════════════════════════════════════════════════╝

┌─────────────────── SEARCH MENU ───────────────────┐
│ 1. 🔍 Search with Natural Language Prompt         │
│ 2. 🧠 Semantic Code Search                        │
│ 3. 📝 Text Search                                 │
│ 4. ⚙️  Advanced Search                            │
│ 5. 📚 Index Codebase                             │
│ 6. ❓ Help                                        │
│ 0. 🚪 Exit                                        │
└───────────────────────────────────────────────────┘
```

### Search Examples

**Natural Language Search:**
- "Find functions that handle user authentication"
- "Show me REST API endpoints for user management"
- "Find classes that implement caching logic"

**Semantic Search:**
- Search for code patterns similar to your query
- Adjustable similarity threshold (0.0 to 1.0)

**Text Search:**
- Traditional keyword search in code files
- Fast and precise for known terms

**Advanced Search:**
- Filter by file types (java, js, py, etc.)
- Filter by programming languages
- Repository-specific search

## Development

The project structure:
```
src/main/java/sg/edu/nus/iss/codebase/indexer/
├── IndexerApplication.java     # Main Spring Boot application
├── cli/
│   └── SearchCLI.java         # Command-line interface
├── service/
│   └── SearchService.java     # Search logic and AI integration
└── dto/
    └── SearchRequest.java      # Data transfer objects
```

## Configuration

### Vector Database Setup (Qdrant Cloud)

1. **Create a Qdrant Cloud Account:**
   - Visit [Qdrant Cloud](https://cloud.qdrant.io/)
   - Sign up for a free account
   - Create a new cluster

2. **Get Your Cluster Details:**
   ```bash
   # Your cluster URL will look like:
   # https://your-cluster-id.qdrant.tech:6333
   ```

3. **Configure the Application:**
   ```properties
   # In application.properties
   spring.ai.vectorstore.qdrant.host=your-cluster-id.qdrant.tech
   spring.ai.vectorstore.qdrant.port=6333
   spring.ai.vectorstore.qdrant.use-tls=true
   spring.ai.vectorstore.qdrant.api-key=your-api-key-here
   spring.ai.vectorstore.qdrant.collection-name=codebase-index
   ```

4. **Create Collection (Optional):**
   The application will automatically create the collection if it doesn't exist.

### AI Model Configuration

The application uses Ollama for local AI processing with CodeLlama:

```properties
# Ollama Configuration
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=codellama:7b
spring.ai.ollama.embedding.options.model=codellama:7b
```

**Available Models:**
- `codellama:7b` - 7B parameter CodeLlama model (recommended)
- `codellama:13b` - 13B parameter model (better quality, slower)
- `codellama:34b` - 34B parameter model (best quality, slowest)

**Install different models:**
```bash
ollama pull codellama:13b
ollama pull codellama:34b
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For issues and questions:
- Create an issue on GitHub
- Check the documentation
- Review the help menu in the application (option 6)
