# Misoto Codebase Indexer

An AI-powered terminal application for intelligent code search and indexing using Spring AI and vector databases.

## Features

- 🔍 **Natural Language Search**: Search code using plain English queries
- 🧠 **Semantic Search**: Find conceptually similar code using AI embeddings
- 📝 **Text Search**: Traditional keyword-based search
- ⚙️ **Advanced Search**: Filter by file type, language, repository
- 📚 **Intelligent Indexing**: AI-powered code analysis and indexing
- 📊 **Detailed Status Tracking**: Real-time indexing progress and file type statistics
- 💾 **Persistent Caching**: Avoids re-indexing unchanged files
- 🔄 **Background Processing**: Non-blocking indexing with immediate search availability

## 🔄 Application Logic Flow

### **Hybrid Indexing Pipeline**

```mermaid
graph TD
    A[🚀 Application Start] --> B[📋 Initialize Qdrant Collection]
    B --> C[🔍 Set Indexing Directory]
    C --> D[📂 Load File Cache]
    D --> E[🔍 Scan Directory Structure]
    E --> F{📄 File Validation}
    
    F -->|Supported Extension| G[✅ Check Cache Status]
    F -->|Unsupported Extension| H[📊 Track Skipped Extensions]
    
    G -->|New/Modified| I[🚀 Queue for Indexing]
    G -->|Unchanged| J[⏭️ Skip Processing]
    
    I --> K[📋 Phase 1: Priority Files]
    K --> L[⚡ Virtual Thread Processing]
    L --> M[📄 Raw Text Extraction]
    M --> N[🤖 nomic-embed-text Embedding]
    N --> O[📊 768D Vector Generation]
    O --> P[☁️ Qdrant Vector Storage]
    P --> Q[💾 Update Cache]
    
    Q --> R[📋 Phase 2: Remaining Files]
    R --> S[🔄 Background Batch Processing]
    S --> T[✅ Indexing Complete]
    
    H --> U[📊 Status Reporting]
    J --> U
    T --> U
```

### **Embedding Flow Architecture**

```
📄 Raw Text (from source files)
    ↓
🤖 nomic-embed-text (Ollama embedding model - 768 dimensions)  
    ↓
📊 Vector Representation (768-dimensional float array)
    ↓
☁️ Qdrant Cloud (vector database storage with metadata)
```

### **File Processing Strategy**

#### **Priority-Based Indexing**
1. **Phase 1 - Critical Files (Priority 1-5):**
   - Controllers (`*Controller.java`) - Priority 1
   - Services (`*Service.java`) - Priority 2  
   - Repositories (`*Repository.java`) - Priority 3
   - Configuration (`*Config.java`) - Priority 4
   - Applications (`*Application.java`) - Priority 5

2. **Phase 2 - Background Processing:**
   - All remaining supported files
   - Processed in batches using virtual threads
   - Non-blocking execution

#### **Supported File Extensions**

| Category | Extensions | Purpose |
|----------|------------|---------|
| **Java Ecosystem** | `.java`, `.xml`, `.properties`, `.yml`, `.yaml`, `.json` | Core application files |
| **Documentation** | `.md`, `.txt`, `.st`, `.adoc` | Project documentation |
| **JVM Languages** | `.kt`, `.scala` | Kotlin and Scala source |
| **Database** | `.sql`, `.cql` | Database schemas and queries |
| **Web Technologies** | `.html`, `.css`, `.js`, `.jsp`, `.asp`, `.aspx`, `.php` | Frontend and web components |
| **System Scripts** | `.conf`, `.cmd`, `.sh` | Configuration and automation |
| **Programming Languages** | `.py`, `.c`, `.cpp`, `.cs`, `.rb`, `.vb`, `.go`, `.swift`, `.lua`, `.pl`, `.r` | Multi-language support |
| **Documents** | `.pdf` | Documentation and specs |

### **Search Execution Flow**

```mermaid
graph LR
    A[🔍 Search Query] --> B{Search Type}
    
    B -->|Natural Language| C[🤖 Process with LLM]
    B -->|Semantic| D[🧠 Direct Vector Search]
    B -->|Text| E[📝 Keyword Search]
    
    C --> F[🔍 Generate Search Context]
    F --> G[📊 Vector Similarity Search]
    
    D --> G
    E --> H[📂 File Content Search]
    
    G --> I[📋 Rank Results by Relevance]
    H --> I
    
    I --> J[📊 Format and Display Results]
```

### **Performance Optimizations**

- **Virtual Threads**: Concurrent processing for I/O-intensive operations
- **Persistent Cache**: Tracks file modification times to avoid re-indexing
- **Batch Processing**: Groups files for efficient processing
- **Priority Queuing**: Critical files indexed first for immediate search availability
- **Smart Chunking**: Large files split into manageable 3KB chunks with 500-character overlap
- **Background Execution**: Indexing runs asynchronously without blocking the CLI

### **Status Tracking & Metrics**

The application provides comprehensive real-time metrics:

- **📊 Progress**: Indexed vs. total files percentage
- **⏱️ Timing**: Current duration, estimated completion time
- **🚀 Performance**: Files per second processing speed
- **🧵 Threading**: Active and peak virtual thread usage
- **📄 File Types**: Breakdown by extension and count
- **⚠️ Issues**: Failed and skipped file counts
- **🚫 Skipped Extensions**: Non-supported file types encountered

## Prerequisites

- Java 21+
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
   
   # OR use the clean run script (recommended)
   run-clean.bat
   ```

6. **Access Interactive CLI Menu**
   The application will start with a clean interface directly to the menu:   
   
   ```
   ╔══════════════════════════════════════════════════════════════╗
   ║                    MISOTO CODEBASE INDEXER                   ║
   ║                   Intelligent Code Search                    ║
   ╚══════════════════════════════════════════════════════════════╝
   
   ┌─────────────────── SEARCH MENU ───────────────────┐
   │ 1. [>] Search with Natural Language Prompt        │
   │ 2. [i] Indexing Status                            │
   │ 3. [S] Semantic Code Search                       │
   │ 4. [T] Text Search                                │
   │ 5. [A] Advanced Search                            │
   │ 6. [I] Index Codebase                             │
   │ 7. [?] Help                                       │
   │ 0. [X] Exit                                       │
   └───────────────────────────────────────────────────┘
   ```

### Detailed Menu Options

#### **1. 🔍 Natural Language Search**
Use conversational queries to find code with AI assistance:

**Example Queries:**
```
🔍 Search Query: Find authentication logic
🔍 Search Query: Show me REST API endpoints for user management  
🔍 Search Query: Classes that implement caching
🔍 Search Query: Database connection configuration
🔍 Search Query: Error handling middleware
🔍 Search Query: JWT token validation
```

**How it works:**
- AI processes your natural language intent
- Converts to optimized search terms
- Returns ranked results with relevance scores
- Shows code snippets with context

#### **2. 📊 Indexing Status**
Monitor real-time indexing progress and system performance:

```
╔═════════════════ INDEXING STATUS ═════════════════╗
║ 📊 Progress: 1,247 / 2,150 files (58.0%)         ║
║ ⏱️  Duration: 45s | Estimated: 78s remaining      ║
║ 🚀 Speed: 27.7 files/second                      ║
║ 🧵 Threads: 8 active, 12 peak                    ║
║                                                   ║
║ 📄 File Types Indexed:                           ║
║   • .java: 423 files                             ║
║   • .xml: 156 files                              ║
║   • .properties: 89 files                        ║
║   • .md: 67 files                                ║
║   • .kt: 45 files                                ║
║                                                   ║
║ 🚫 Skipped Extensions: .class (234), .jar (12)   ║
║ ⚠️  Failed: 3 files | Skipped: 456 files         ║
╚═══════════════════════════════════════════════════╝
```

**Status Information:**
- **Progress**: Percentage of files processed
- **Performance**: Files per second processing speed
- **Threading**: Virtual thread usage for optimal performance
- **File Breakdown**: Count by file type/extension
- **Issues**: Failed and skipped file tracking

#### **3. 🧠 Semantic Code Search**
Find conceptually similar code using vector embeddings:

**Example Usage:**
```
🧠 Enter search query: database repository pattern
🎯 Similarity threshold (0.0-1.0) [0.7]: 0.8
🔍 Max results [10]: 5

📊 Found 5 results (similarity > 0.8):

1. UserRepository.java (0.92) - Line 23
   @Repository
   public class UserRepository extends JpaRepository<User, Long> {
       Optional<User> findByUsername(String username);
   }

2. ProductService.java (0.89) - Line 45
   private final ProductRepository productRepository;
   
3. OrderRepository.java (0.85) - Line 12
   public interface OrderRepository extends CrudRepository<Order, UUID> {
```

**Features:**
- Adjustable similarity threshold (0.0 to 1.0)
- Vector-based semantic matching
- Ranked results by relevance score
- Context-aware code snippets

#### **4. 📝 Text Search**
Fast keyword-based search across all indexed files:

**Example Usage:**
```
📝 Enter search term: @RestController
🔍 Case sensitive? [y/N]: n
📊 Max results [20]: 10

📊 Found 8 matches in 6 files:

1. UserController.java - Line 15
   @RestController
   @RequestMapping("/api/users")
   public class UserController {

2. AuthController.java - Line 12
   @RestController
   @RequestMapping("/api/auth") 
   public class AuthController {
```

**Search Options:**
- Case-sensitive or insensitive matching
- Regular expression support
- File path filtering
- Configurable result limits

#### **5. ⚙️ Advanced Search**
Combine multiple search criteria for precise results:

**Filter Options:**
```
⚙️ Advanced Search Configuration:
📁 File extensions: .java,.kt,.scala
🏷️  File name pattern: *Service*
📂 Directory filter: src/main/java
🔍 Content contains: @Transactional
📏 File size: 1KB - 100KB
📅 Modified after: 2024-01-01
```

**Example Results:**
```
📊 Advanced Search Results (12 matches):

Filters Applied:
✅ Extensions: .java, .kt
✅ Pattern: *Service*  
✅ Content: @Transactional
✅ Directory: src/main/java

1. UserService.java (src/main/java/service/)
   @Transactional
   public void updateUser(User user) { ... }

2. OrderService.kt (src/main/java/service/)
   @Transactional
   fun processOrder(order: Order) { ... }
```

#### **6. 📚 Index Codebase**
Start or restart the indexing process:

**Options:**
```
📚 Codebase Indexing Options:

1. 🔄 Restart indexing (current directory)
2. 📁 Change indexing directory
3. 🗑️  Clear cache and reindex all files
4. ⏸️  Pause/Resume indexing
5. 📊 View indexing statistics

Current directory: /path/to/project/src
Indexed files: 1,247 | Cache entries: 1,189
```

**Directory Selection:**
```
📁 Select indexing directory:
   Current: /project/src
   
1. 📂 /project/src (current)
2. 📂 /project/src/main/java
3. 📂 /project/codebase
4. 📝 Enter custom path
5. 🔙 Back to main menu

Enter choice [1-5]:
```

#### **7. ❓ Help**
Comprehensive help and documentation:

```
╔═══════════════════ HELP & TIPS ═══════════════════╗
║                                                   ║
║ 🔍 SEARCH TIPS:                                   ║
║   • Use specific terms: "JWT authentication"     ║
║   • Try different phrasings if no results        ║
║   • Combine keywords: "user repository database" ║
║                                                   ║
║ 🎯 SIMILARITY THRESHOLDS:                         ║
║   • 0.9-1.0: Very similar (exact matches)        ║
║   • 0.7-0.9: Similar (related concepts)          ║
║   • 0.5-0.7: Somewhat related                    ║
║   • 0.0-0.5: Loose associations                  ║
║                                                   ║
║ 📁 SUPPORTED FILE TYPES:                          ║
║   • Code: .java, .kt, .scala, .py, .js, .ts     ║
║   • Config: .xml, .yml, .properties, .json      ║
║   • Web: .html, .css, .jsp, .php                ║
║   • Docs: .md, .txt, .adoc                      ║
║   • Scripts: .sh, .cmd, .sql                    ║
║                                                   ║
║ ⚡ PERFORMANCE:                                    ║
║   • Search available during indexing             ║
║   • Priority files indexed first                 ║
║   • Background processing uses virtual threads   ║
║   • Cache prevents re-indexing unchanged files   ║
║                                                   ║
╚═══════════════════════════════════════════════════╝
```

### Search Examples & Best Practices

#### **Natural Language Search Examples**

| Query Type | Example | What it finds |
|------------|---------|---------------|
| **Functionality** | "user authentication" | Login methods, auth filters, JWT handling |
| **Architecture** | "repository pattern" | Data access objects, JPA repositories |
| **Error Handling** | "exception handling" | Try-catch blocks, error controllers |
| **Configuration** | "database configuration" | DataSource beans, connection properties |
| **API Endpoints** | "REST endpoints for users" | UserController methods, API routes |
| **Security** | "authorization logic" | Security configs, role-based access |

#### **Semantic Search Best Practices**

- **High Similarity (0.8-1.0)**: Find exact patterns and implementations
- **Medium Similarity (0.6-0.8)**: Find related concepts and similar logic
- **Low Similarity (0.4-0.6)**: Explore loosely related code
- **Use specific technical terms**: "repository", "controller", "service"
- **Combine concepts**: "user authentication JWT token"

#### **Text Search Tips**

- **Class names**: `UserService`, `@RestController`
- **Method names**: `findByUsername`, `authenticate`
- **Annotations**: `@Transactional`, `@Autowired`
- **Patterns**: Use wildcards like `find*` or `*Controller`
- **Regular expressions**: Enable regex for complex patterns

### Workflow Examples

#### **Example 1: Finding Authentication Code**
```
1. Start with Natural Language: "user authentication"
2. Review results, note relevant classes
3. Use Semantic Search: "JWT token validation" (similarity: 0.7)
4. Drill down with Text Search: "@PreAuthorize"
5. Use Advanced Search: Files containing "auth" in src/main/java
```

#### **Example 2: Understanding Data Access Layer**
```
1. Natural Language: "database repository pattern"
2. Semantic Search: "JPA repository" (similarity: 0.8)
3. Text Search: "extends JpaRepository"
4. Advanced Search: Filter by *.java files containing "@Repository"
```

#### **Example 3: API Endpoint Discovery**
```
1. Natural Language: "REST API endpoints"
2. Text Search: "@RestController"
3. Semantic Search: "HTTP GET POST endpoints" (similarity: 0.7)
4. Advanced Search: Files matching "*Controller.java"
```

### Performance & Monitoring

- **Real-time Status**: Check option 2 for live indexing progress
- **Search During Indexing**: Search works immediately, even while indexing
- **Cache Management**: System automatically manages file change detection
- **Background Processing**: Indexing doesn't block the interactive menu
- **Memory Efficient**: Virtual threads optimize resource usage

## Development

### Project Structure
```
src/main/java/sg/edu/nus/iss/codebase/indexer/
├── IndexerApplication.java          # Main Spring Boot application
├── cli/
│   ├── SearchCLI.java              # Interactive command-line interface
│   └── command/                    # Command Pattern implementation
│       ├── Command.java            # Command interface
│       └── IndexingStatusCommand.java # Status display command
├── config/
│   ├── EnvironmentConfig.java      # Environment variable configuration
│   ├── IndexingConfiguration.java  # Centralized indexing configuration
│   ├── QdrantCollectionInitializer.java # Vector database setup
│   └── VirtualThreadConfig.java    # Async processing configuration
├── controller/
│   └── SearchController.java       # REST API endpoints (optional)
├── dto/
│   └── SearchRequest.java          # Data transfer objects
├── model/
│   └── IndexingStatus.java         # Status and metrics model
├── service/
│   ├── FileSearchService.java      # File-based search implementation
│   ├── HybridSearchService.java    # Main search orchestration
│   ├── impl/                       # Service implementations
│   │   ├── DocumentFactoryManager.java # Factory manager
│   │   ├── FileCacheRepositoryImpl.java # Cache repository implementation
│   │   ├── FileIndexingServiceImpl.java # Core indexing service
│   │   ├── TextDocumentFactory.java # Text document factory
│   │   └── search/                 # Search strategy implementations
│   │       └── SemanticSearchStrategy.java # Semantic search strategy
│   └── interfaces/                 # Service interfaces
│       ├── DocumentFactory.java    # Factory pattern interface
│       ├── FileCacheRepository.java # Repository pattern interface
│       ├── FileIndexingService.java # Service interface
│       ├── IndexingStatusObserver.java # Observer pattern interface
│       └── SearchStrategy.java     # Strategy pattern interface
```

### Architecture & Design Patterns

The codebase has been refactored to implement several design patterns for better maintainability and extensibility:

#### **1. Configuration Pattern**
- **IndexingConfiguration.java**: Centralizes all configuration settings
- Externalized file extensions, priorities, and processing parameters
- Environment-specific configuration through Spring Boot properties

#### **2. Factory Pattern**
- **DocumentFactory**: Interface for creating documents from different file types
- **TextDocumentFactory**: Concrete factory for text-based files
- **DocumentFactoryManager**: Manages multiple factories and selects appropriate one

#### **3. Repository Pattern**
- **FileCacheRepository**: Interface for file cache operations
- **FileCacheRepositoryImpl**: Persistent cache implementation with modification tracking
- Abstracts cache storage details from business logic

#### **4. Strategy Pattern**
- **SearchStrategy**: Interface for different search implementations
- **SemanticSearchStrategy**: Vector-based semantic search
- **TextSearchStrategy**: Keyword-based text search (extensible)
- **NaturalLanguageSearchStrategy**: AI-powered search (extensible)

#### **5. Observer Pattern**
- **IndexingStatusObserver**: Interface for status update notifications
- Real-time status updates to CLI and other components
- Decoupled status reporting from indexing logic

#### **6. Command Pattern**
- **Command**: Interface for CLI actions
- **IndexingStatusCommand**: Encapsulates status display logic
- Extensible menu system with pluggable commands

#### **7. Builder Pattern**
- **IndexingStatus.Builder**: Fluent API for creating status objects
- **SearchRequest**: DTO with builder support for complex queries

#### **8. Service Layer Pattern**
- **FileIndexingService**: Interface for indexing operations
- **FileIndexingServiceImpl**: Core implementation with dependency injection
- Clear separation of concerns between presentation, business, and data layers

### Key Components

#### **FileIndexingServiceImpl.java**
- **Modular indexing engine** with separated concerns
- **Observer pattern** for real-time status updates
- **Factory pattern** for document creation
- **Repository pattern** for cache management
- **Configuration-driven** file processing
- **Virtual thread pool** for concurrent I/O operations

#### **HybridSearchService.java**
- **Strategy pattern** for different search types
- **Multi-modal search** orchestration
- **Result ranking** and relevance scoring
- **Performance optimization** with caching strategies

#### **SearchCLI.java**
- **Command pattern** for menu actions
- **Interactive terminal interface** with real-time updates
- **Observer implementation** for status monitoring
- **Background processing** with responsive menu system

#### **Configuration & Patterns**
- **IndexingConfiguration**: Centralized, externalized settings
- **FileCacheRepository**: Persistent cache with Repository pattern
- **DocumentFactory**: Extensible factory for different file types
- **SearchStrategy**: Pluggable search implementations

### Architecture Highlights

#### **Modular Design with Design Patterns**
```java
// Configuration Pattern - Externalized settings
@ConfigurationProperties(prefix = "indexer")
public class IndexingConfiguration {
    private Set~String~ supportedExtensions;
    private Map~String,Integer~ filePriorities;
    // ... other configuration
}

// Factory Pattern - Document creation
public interface DocumentFactory {
    List<Document> createDocuments(File file);
    boolean supports(File file);
}

// Repository Pattern - Cache management
public interface FileCacheRepository {
    boolean needsReindexing(File file);
    void saveIndexedFile(String filePath);
}

// Strategy Pattern - Search implementations
public interface SearchStrategy {
    List<SearchResult> search(SearchRequest request);
    boolean supports(SearchRequest.SearchType searchType);
}

// Observer Pattern - Status updates
public interface IndexingStatusObserver {
    void onStatusUpdate(IndexingStatus status);
    void onIndexingComplete(IndexingStatus finalStatus);
}
```

#### **Asynchronous Processing**
```java
@Async("virtualThreadExecutor")
public CompletableFuture<Void> indexRemainingFilesAsync() {
    // Background processing using virtual threads
    // with observer notifications for status updates
}
```

#### **Intelligent Caching with Repository Pattern**
```java
@Repository
public class FileCacheRepositoryImpl implements FileCacheRepository {
    public boolean needsReindexing(File file) {
        // Only reindex if file is new or modified
        return !indexedFilePaths.contains(filePath) || 
               currentModTime != cachedModTime;
    }
}
```

#### **Vector Pipeline with Factory Pattern**
```java
// DocumentFactoryManager selects appropriate factory
List<Document> documents = documentFactoryManager.createDocuments(file);
vectorStore.add(documents); // nomic-embed-text → 768D vector → Qdrant
```

## 🎨 Design Patterns & UML Diagrams

## 📐 UML Design Pattern Diagrams

### **1. Factory Pattern - Document Creation**

```mermaid
classDiagram
    class DocumentFactory {
        <<interface>>
        +createDocuments(File file) List~Document~
        +supports(File file) boolean
    }
    
    class TextDocumentFactory {
        -chunkSize: int
        -chunkOverlap: int
        +createDocuments(File file) List~Document~
        +supports(File file) boolean
        -extractTextContent(File file) String
        -createChunkedDocuments(String content, File file) List~Document~
    }
    
    class DocumentFactoryManager {
        -factories: List~DocumentFactory~
        +createDocuments(File file) List~Document~
        +registerFactory(DocumentFactory factory) void
        -findSupportingFactory(File file) DocumentFactory
    }
    
    class Document {
        +content: String
        +metadata: Map~String, Object~
    }
    
    DocumentFactory <|.. TextDocumentFactory
    DocumentFactoryManager o--> DocumentFactory : manages
    DocumentFactory ..> Document : creates
    TextDocumentFactory ..> Document : creates
```

### **2. Repository Pattern - Cache Management**

```mermaid
classDiagram
    class FileCacheRepository {
        <<interface>>
        +needsReindexing(File file) boolean
        +saveIndexedFile(String filePath) void
        +loadCache() void
        +saveCache() void
        +getCacheStats() Map~String, Object~
    }
    
    class FileCacheRepositoryImpl {
        -indexedFilePaths: Set~String~
        -fileModificationTimes: Map~String, Long~
        -cacheFilePath: String
        +needsReindexing(File file) boolean
        +saveIndexedFile(String filePath) void
        +loadCache() void
        +saveCache() void
        +getCacheStats() Map~String, Object~
        -getCurrentModificationTime(File file) long
        -getCachedModificationTime(String filePath) Long
    }
    
    class FileIndexingServiceImpl {
        -fileCacheRepository: FileCacheRepository
        +indexDirectory(String directoryPath) void
        -shouldIndexFile(File file) boolean
    }
    
    FileCacheRepository <|.. FileCacheRepositoryImpl
    FileIndexingServiceImpl --> FileCacheRepository : uses
```

### **3. Strategy Pattern - Search Implementations**

```mermaid
classDiagram
    class SearchStrategy {
        <<interface>>
        +search(SearchRequest request) List~SearchResult~
        +supports(SearchType searchType) boolean
    }
    
    class SemanticSearchStrategy {
        -vectorStore: VectorStore
        -embeddingModel: EmbeddingModel
        +search(SearchRequest request) List~SearchResult~
        +supports(SearchType searchType) boolean
        -createEmbeddingQuery(String query) List~Double~
        -convertToSearchResults(List results) List~SearchResult~
    }
    
    class TextSearchStrategy {
        -fileSearchService: FileSearchService
        +search(SearchRequest request) List~SearchResult~
        +supports(SearchType searchType) boolean
        -performTextSearch(String query) List~SearchResult~
    }
    
    class NaturalLanguageSearchStrategy {
        -chatModel: ChatModel
        -semanticSearchStrategy: SemanticSearchStrategy
        +search(SearchRequest request) List~SearchResult~
        +supports(SearchType searchType) boolean
        -processNaturalLanguageQuery(String query) String
    }
    
    class HybridSearchService {
        -searchStrategies: List~SearchStrategy~
        +search(SearchRequest request) List~SearchResult~
        -findStrategy(SearchType type) SearchStrategy
        -rankAndMergeResults(List results) List~SearchResult~
    }
    
    SearchStrategy <|.. SemanticSearchStrategy
    SearchStrategy <|.. TextSearchStrategy
    SearchStrategy <|.. NaturalLanguageSearchStrategy
    HybridSearchService o--> SearchStrategy : manages
```

### **4. Observer Pattern - Status Updates**

```mermaid
classDiagram
    class IndexingStatusObserver {
        <<interface>>
        +onStatusUpdate(IndexingStatus status) void
        +onIndexingComplete(IndexingStatus finalStatus) void
        +onIndexingError(String error) void
    }
    
    class FileIndexingServiceImpl {
        -observers: List~IndexingStatusObserver~
        -currentStatus: IndexingStatus
        +addObserver(IndexingStatusObserver observer) void
        +removeObserver(IndexingStatusObserver observer) void
        +indexDirectory(String path) void
        -notifyObservers(IndexingStatus status) void
        -notifyCompletion(IndexingStatus status) void
        -notifyError(String error) void
    }
    
    class SearchCLI {
        +onStatusUpdate(IndexingStatus status) void
        +onIndexingComplete(IndexingStatus finalStatus) void
        +onIndexingError(String error) void
        -displayStatusUpdate(IndexingStatus status) void
    }
    
    class StatusMetricsCollector {
        -metricsHistory: List~IndexingStatus~
        +onStatusUpdate(IndexingStatus status) void
        +onIndexingComplete(IndexingStatus finalStatus) void
        +onIndexingError(String error) void
        +getMetricsReport() String
    }
    
    IndexingStatusObserver <|.. SearchCLI
    IndexingStatusObserver <|.. StatusMetricsCollector
    FileIndexingServiceImpl o--> IndexingStatusObserver : notifies
```

### **5. Command Pattern - CLI Actions**

```mermaid
classDiagram
    class Command {
        <<interface>>
        +execute(String[] args) void
        +getDescription() String
        +getUsage() String
    }
    
    class IndexingStatusCommand {
        -fileIndexingService: FileIndexingService
        +execute(String[] args) void
        +getDescription() String
        +getUsage() String
        -displayDetailedStatus() void
        -formatStatusDisplay(IndexingStatus status) String
    }
    
    class SearchCommand {
        -hybridSearchService: HybridSearchService
        +execute(String[] args) void
        +getDescription() String
        +getUsage() String
        -performSearch(SearchRequest request) void
    }
    
    class IndexCommand {
        -fileIndexingService: FileIndexingService
        +execute(String[] args) void
        +getDescription() String
        +getUsage() String
        -startIndexing(String directoryPath) void
    }
    
    class SearchCLI {
        -commands: Map~String, Command~
        +registerCommand(String key, Command command) void
        +executeCommand(String commandKey, String[] args) void
        -displayMenu() void
        -processUserInput() void
    }
    
    Command <|.. IndexingStatusCommand
    Command <|.. SearchCommand
    Command <|.. IndexCommand
    SearchCLI o--> Command : manages
```

### **6. Builder Pattern - Configuration Objects**

```mermaid
classDiagram
    class IndexingStatus {
        -totalFiles: int
        -indexedFiles: int
        -startTime: LocalDateTime
        -estimatedCompletion: LocalDateTime
        -filesPerSecond: double
        -activeThreads: int
        -peakThreads: int
        -fileTypeStats: Map~String, Integer~
        -failedFiles: int
        -skippedFiles: int
        +builder() IndexingStatusBuilder
    }
    
    class IndexingStatusBuilder {
        -totalFiles: int
        -indexedFiles: int
        -startTime: LocalDateTime
        -estimatedCompletion: LocalDateTime
        -filesPerSecond: double
        -activeThreads: int
        -peakThreads: int
        -fileTypeStats: Map~String, Integer~
        -failedFiles: int
        -skippedFiles: int
        +totalFiles(int totalFiles) IndexingStatusBuilder
        +indexedFiles(int indexedFiles) IndexingStatusBuilder
        +startTime(LocalDateTime startTime) IndexingStatusBuilder
        +estimatedCompletion(LocalDateTime time) IndexingStatusBuilder
        +filesPerSecond(double rate) IndexingStatusBuilder
        +activeThreads(int threads) IndexingStatusBuilder
        +peakThreads(int threads) IndexingStatusBuilder
        +fileTypeStats(Map stats) IndexingStatusBuilder
        +failedFiles(int failed) IndexingStatusBuilder
        +skippedFiles(int skipped) IndexingStatusBuilder
        +build() IndexingStatus
    }
    
    class SearchRequest {
        -query: String
        -searchType: SearchType
        -maxResults: int
        -similarityThreshold: double
        -fileExtensions: Set~String~
        -dateRange: DateRange
        -sortCriteria: SortCriteria
        +builder() SearchRequestBuilder
    }
    
    class SearchRequestBuilder {
        -query: String
        -searchType: SearchType
        -maxResults: int
        -similarityThreshold: double
        -fileExtensions: Set~String~
        -dateRange: DateRange
        -sortCriteria: SortCriteria
        +query(String query) SearchRequestBuilder
        +searchType(SearchType type) SearchRequestBuilder
        +maxResults(int max) SearchRequestBuilder
        +similarityThreshold(double threshold) SearchRequestBuilder
        +fileExtensions(Set extensions) SearchRequestBuilder
        +dateRange(DateRange range) SearchRequestBuilder
        +sortCriteria(SortCriteria criteria) SearchRequestBuilder
        +build() SearchRequest
    }
    
    IndexingStatus --> IndexingStatusBuilder : creates
    SearchRequest --> SearchRequestBuilder : creates
```

### **7. Configuration Pattern - Centralized Settings**

```mermaid
classDiagram
    class IndexingConfiguration {
        <<@ConfigurationProperties>>
        -supportedExtensions: Set~String~
        -filePriorities: Map~String, Integer~
        -chunkSize: int
        -chunkOverlap: int
        -maxConcurrentFiles: int
        -cacheEnabled: boolean
        -cacheFilePath: String
        -indexingBatchSize: int
        +getSupportedExtensions() Set~String~
        +getFilePriorities() Map~String, Integer~
        +getChunkSize() int
        +getChunkOverlap() int
        +getMaxConcurrentFiles() int
        +isCacheEnabled() boolean
        +getCacheFilePath() String
        +getIndexingBatchSize() int
        +getFilePriority(String extension) int
        +isFileSupported(String extension) boolean
    }
    
    class FileIndexingServiceImpl {
        -indexingConfiguration: IndexingConfiguration
        +indexDirectory(String path) void
        -isFileSupported(File file) boolean
        -getFilePriority(File file) int
        -createDocumentChunks(File file) List~Document~
    }
    
    class DocumentFactoryManager {
        -indexingConfiguration: IndexingConfiguration
        +createDocuments(File file) List~Document~
        -shouldProcessFile(File file) boolean
    }
    
    class HybridSearchService {
        -indexingConfiguration: IndexingConfiguration
        +search(SearchRequest request) List~SearchResult~
        +getSupportedSearchTypes() List~SearchType~
        -selectStrategy(SearchType type) SearchStrategy
        -rankResults(List results) List~SearchResult~
    }
    
    IndexingConfiguration --> FileIndexingServiceImpl : configures
    IndexingConfiguration --> DocumentFactoryManager : configures
    IndexingConfiguration --> HybridSearchService : configures
```

### **8. Service Layer Pattern - Business Logic Separation**

```mermaid
classDiagram
    class FileIndexingService {
        <<interface>>
        +indexDirectory(String directoryPath) CompletableFuture~Void~
        +getIndexingStatus() IndexingStatus
        +pauseIndexing() void
        +resumeIndexing() void
        +cancelIndexing() void
        +addObserver(IndexingStatusObserver observer) void
        +removeObserver(IndexingStatusObserver observer) void
    }
    
    class FileIndexingServiceImpl {
        -vectorStore: VectorStore
        -embeddingModel: EmbeddingModel
        -documentFactoryManager: DocumentFactoryManager
        -fileCacheRepository: FileCacheRepository
        -indexingConfiguration: IndexingConfiguration
        -observers: List~IndexingStatusObserver~
        -virtualThreadExecutor: ExecutorService
        -indexingStatus: IndexingStatus
        +indexDirectory(String directoryPath) CompletableFuture~Void~
        +getIndexingStatus() IndexingStatus
        +pauseIndexing() void
        +resumeIndexing() void
        +cancelIndexing() void
        +addObserver(IndexingStatusObserver observer) void
        +removeObserver(IndexingStatusObserver observer) void
        -indexPriorityFiles(List files) void
        -indexRemainingFiles(List files) void
        -processFile(File file) void
        -updateStatus() void
        -notifyObservers() void
    }
    
    class HybridSearchService {
        -searchStrategies: List~SearchStrategy~
        -indexingConfiguration: IndexingConfiguration
        +search(SearchRequest request) List~SearchResult~
        +getSupportedSearchTypes() List~SearchType~
        -selectStrategy(SearchType type) SearchStrategy
        -rankResults(List results) List~SearchResult~
    }
    
    class SearchCLI {
        -fileIndexingService: FileIndexingService
        -hybridSearchService: HybridSearchService
        +startInteractiveMode() void
        -handleUserInput() void
        -displaySearchResults() void
    }
    
    FileIndexingService <|.. FileIndexingServiceImpl
    SearchCLI --> FileIndexingService : uses
    SearchCLI --> HybridSearchService : uses
    FileIndexingServiceImpl --> DocumentFactoryManager : uses
    FileIndexingServiceImpl --> FileCacheRepository : uses
    HybridSearchService --> SearchStrategy : uses
```

### **9. Overall System Architecture**

```mermaid
graph TB
    subgraph "Presentation Layer"
        CLI[SearchCLI]
        CMD[Command Objects]
    end
    
    subgraph "Service Layer"
        IS[FileIndexingService]
        HS[HybridSearchService]
        FS[FileSearchService]
    end
    
    subgraph "Strategy Layer"
        SS[SemanticSearchStrategy]
        TS[TextSearchStrategy]
        NLS[NaturalLanguageSearchStrategy]
    end
    
    subgraph "Factory Layer"
        DFM[DocumentFactoryManager]
        TDF[TextDocumentFactory]
        PDF[PdfDocumentFactory]
    end
    
    subgraph "Repository Layer"
        FCR[FileCacheRepository]
        VDB[(Vector Database)]
        CACHE[(File Cache)]
    end
    
    subgraph "Configuration Layer"
        IC[IndexingConfiguration]
        EC[EnvironmentConfig]
    end
    
    subgraph "Observer Layer"
        OBS[Status Observers]
        METRICS[Metrics Collector]
    end
    
    CLI --> CMD
    CLI --> IS
    CLI --> HS
    CLI -.-> OBS
    
    IS --> DFM
    IS --> FCR
    IS -.-> OBS
    
    HS --> SS
    HS --> TS
    HS --> NLS
    
    DFM --> TDF
    DFM --> PDF
    
    SS --> VDB
    TS --> FS
    FCR --> CACHE
    
    IC --> IS
    IC --> HS
    IC --> DFM
    
    OBS --> METRICS
    
    style CLI fill:#e1f5fe
    style IS fill:#f3e5f5
    style HS fill:#f3e5f5
    style SS fill:#e8f5e8
    style DFM fill:#fff3e0
    style FCR fill:#fce4ec
    style IC fill:#f1f8e9
    style OBS fill:#e0f2f1
```

### **10. Pattern Interaction Flow**

```mermaid
sequenceDiagram
    participant CLI as SearchCLI
    participant CMD as IndexingStatusCommand
    participant IS as FileIndexingService
    participant DFM as DocumentFactoryManager
    participant TDF as TextDocumentFactory
    participant FCR as FileCacheRepository
    participant OBS as StatusObserver
    participant VS as VectorStore
    
    CLI->>CMD: execute("status")
    CMD->>IS: getIndexingStatus()
    
    Note over CLI,VS: Indexing Process
    CLI->>IS: indexDirectory("/project/src")
    IS->>FCR: loadCache()
    IS->>DFM: createDocuments(file)
    DFM->>TDF: createDocuments(file)
    TDF-->>DFM: List<Document>
    DFM-->>IS: List<Document>
    IS->>VS: add(documents)
    IS->>FCR: saveIndexedFile(filePath)
    IS->>OBS: onStatusUpdate(status)
    OBS-->>CLI: displayStatusUpdate()
    
    Note over CLI,VS: Search Process  
    CLI->>IS: search(request)
    IS->>DFM: findStrategy(SEMANTIC)
    DFM-->>IS: SemanticSearchStrategy
    IS->>VS: similaritySearch(query)
    VS-->>IS: List<SearchResult>
    IS-->>CLI: List<SearchResult>
```

These UML diagrams provide a comprehensive view of all the design patterns implemented in the refactored codebase, showing:

1. **Factory Pattern**: Document creation with extensible factories
2. **Repository Pattern**: File cache management with persistent storage
3. **Strategy Pattern**: Pluggable search implementations
4. **Observer Pattern**: Real-time status updates and notifications
5. **Command Pattern**: CLI command structure and execution
6. **Builder Pattern**: Fluent object construction for complex types
7. **Configuration Pattern**: Centralized, externalized settings
8. **Service Layer Pattern**: Business logic separation and dependency injection
9. **Overall Architecture**: System-wide component interactions
10. **Pattern Interactions**: Sequence diagram showing how patterns work together

Each pattern is designed to enhance maintainability, extensibility, and testability of the codebase while following SOLID principles and Spring Boot best practices.
