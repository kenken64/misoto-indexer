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

## 🔄 Sequence Diagrams

### **Main Application Flow - Indexing and Search Operations**

```mermaid
sequenceDiagram
    participant User
    participant CLI as SearchCLI
    participant IndexSvc as FileIndexingService
    participant Config as IndexingConfiguration
    participant Cache as FileCacheRepository
    participant QdrantSvc as QdrantDocumentService
    participant SearchSvc as HybridSearchService
    participant SearchStrat as SearchStrategy
    participant Observer as StatusObserver

    Note over User,Observer: Application Startup & Initialization
    User->>CLI: Start Application
    CLI->>Config: Load Configuration
    Config-->>CLI: Return config settings
    CLI->>IndexSvc: Initialize indexing service
    IndexSvc->>QdrantSvc: Initialize Qdrant collection
    QdrantSvc-->>IndexSvc: Collection ready
    CLI->>IndexSvc: Add CLI as status observer
    
    Note over User,Observer: Directory Indexing Flow
    User->>CLI: Select "Index Directory"
    CLI->>User: Prompt for directory path
    User->>CLI: Provide directory path
    CLI->>IndexSvc: indexDirectory(path)
    
    IndexSvc->>Cache: loadCache()
    Cache-->>IndexSvc: Return cached file info
    IndexSvc->>IndexSvc: scanDirectory(path)
    IndexSvc->>Config: getSupportedExtensions()
    Config-->>IndexSvc: Return extensions list
    
    loop For each file in directory
        IndexSvc->>Cache: isFileModified(file)
        alt File is new or modified
            Cache-->>IndexSvc: true
            IndexSvc->>IndexSvc: queueForIndexing(file)
        else File unchanged
            Cache-->>IndexSvc: false
            Note over IndexSvc: Skip processing
        end
    end
    
    Note over IndexSvc,Observer: Background Processing with Status Updates
    IndexSvc->>Observer: onStatusUpdate(status)
    Observer->>CLI: displayStatusUpdate(status)
    CLI->>User: Show progress information
    
    loop Batch processing files
        IndexSvc->>QdrantSvc: processFileChunks(file)
        QdrantSvc->>QdrantSvc: extractText(file)
        QdrantSvc->>QdrantSvc: generateEmbeddings(text)
        QdrantSvc->>QdrantSvc: storeVectors(embeddings)
        QdrantSvc-->>IndexSvc: Processing complete
        IndexSvc->>Cache: updateFileCache(file)
        IndexSvc->>Observer: onStatusUpdate(updatedStatus)
        Observer->>CLI: displayStatusUpdate(updatedStatus)
    end
    
    IndexSvc->>Observer: onIndexingComplete(finalStatus)
    Observer->>CLI: displayCompletionMessage()
    CLI->>User: Show indexing completed
    
    Note over User,Observer: Search Operation Flow
    User->>CLI: Select search option
    CLI->>User: Prompt for search query
    User->>CLI: Provide search query and type
    CLI->>SearchSvc: search(searchRequest)
    
    SearchSvc->>SearchStrat: findStrategy(searchType)
    SearchStrat-->>SearchSvc: Return appropriate strategy
    
    alt Semantic Search
        SearchSvc->>SearchStrat: search(semanticQuery)
        SearchStrat->>QdrantSvc: vectorSearch(queryEmbedding)
        QdrantSvc-->>SearchStrat: Return vector results
    else Text Search
        SearchSvc->>SearchStrat: search(textQuery)
        SearchStrat->>SearchStrat: performTextSearch(query)
    else Natural Language Search
        SearchSvc->>SearchStrat: search(nlQuery)
        SearchStrat->>SearchStrat: processNaturalLanguageQuery()
        SearchStrat->>SearchStrat: delegateToSemanticSearch()
    end
    
    SearchStrat-->>SearchSvc: Return search results
    SearchSvc->>SearchSvc: rankAndMergeResults()
    SearchSvc-->>CLI: Return formatted results
    CLI->>User: Display search results
    
    Note over User,Observer: Status Monitoring Flow
    User->>CLI: Select "View Status"
    CLI->>IndexSvc: getIndexingStatus()
    IndexSvc->>IndexSvc: calculateCurrentMetrics()
    IndexSvc->>QdrantSvc: getCollectionInfo()
    QdrantSvc-->>IndexSvc: Return collection stats
    IndexSvc-->>CLI: Return status information
    CLI->>CLI: formatStatusDisplay()
    CLI->>User: Display detailed status
```

### **Error Handling and Recovery Flow**

```mermaid
sequenceDiagram
    participant CLI as SearchCLI
    participant IndexSvc as FileIndexingService
    participant QdrantSvc as QdrantDocumentService
    participant Cache as FileCacheRepository
    participant Observer as StatusObserver

    Note over CLI,Observer: Error Scenarios and Recovery
    
    CLI->>IndexSvc: indexDirectory(invalidPath)
    IndexSvc->>IndexSvc: validateDirectory(path)
    alt Directory doesn't exist
        IndexSvc-->>CLI: DirectoryNotFoundException
        CLI->>CLI: handleDirectoryError()
        CLI->>CLI: showErrorMessage()
        CLI->>CLI: promptForValidPath()
    end
    
    IndexSvc->>QdrantSvc: processFile(corruptedFile)
    QdrantSvc->>QdrantSvc: extractText(file)
    alt File processing fails
        QdrantSvc-->>IndexSvc: FileProcessingException
        IndexSvc->>IndexSvc: incrementFailedCount()
        IndexSvc->>Cache: markFileAsFailed(file)
        IndexSvc->>Observer: onStatusUpdate(statusWithError)
        Observer->>CLI: displayErrorInStatus()
    end
    
    CLI->>QdrantSvc: search(query)
    QdrantSvc->>QdrantSvc: performVectorSearch()
    alt Qdrant collection not found
        QdrantSvc-->>CLI: QdrantException("Collection not found")
        CLI->>CLI: handleQdrantError()
        CLI->>CLI: showNoIndexMessage()
        CLI->>CLI: promptForIndexing()
    end
    
    IndexSvc->>Cache: loadCache()
    Cache->>Cache: readCacheFile()
    alt Cache file corrupted
        Cache-->>IndexSvc: CacheCorruptedException
        IndexSvc->>Cache: rebuildCache()
        IndexSvc->>Observer: onStatusUpdate(rebuildingStatus)
        Observer->>CLI: showCacheRebuildMessage()
    end
```

## 🏗️ Deployment Diagrams

### **System Architecture and Component Deployment**

```mermaid
graph TB
    subgraph "User Environment 💻"
        USER[👤 Developer]
        TERMINAL[🖥️ Terminal/CLI]
        IDE[💻 IDE/VS Code]
        WORKSPACE[📁 Code Workspace]
    end

    subgraph "Local Machine 🖥️"
        subgraph "Spring Boot Application 🚀"
            CLI_APP[🎛️ SearchCLI<br/>Interactive Menu]
            SPRING_BOOT[⚙️ Spring Boot Container<br/>Port: 8080]
            
            subgraph "Service Layer 🔧"
                INDEX_SVC[📚 FileIndexingService<br/>Virtual Thread Pool]
                SEARCH_SVC[🔍 HybridSearchService<br/>Multi-Strategy Search]
                CACHE_SVC[💾 FileCacheRepository<br/>Local File Cache]
            end
            
            subgraph "Configuration 📋"
                CONFIG[⚙️ IndexingConfiguration<br/>application.properties]
                ENV_CONFIG[🌍 Environment Variables<br/>.env file]
            end
        end
        
        subgraph "Ollama AI Platform 🤖"
            OLLAMA_SERVER[🤖 Ollama Server<br/>Port: 11434]
            EMBEDDING_MODEL[📊 nomic-embed-text<br/>768D Embeddings]
            CHAT_MODEL[💬 codellama:7b<br/>Natural Language]
        end
        
        subgraph "Local Storage 💾"
            FILE_CACHE[📄 File Cache<br/>.indexer-cache.json]
            LOG_FILES[📝 Application Logs<br/>logs/]
            TEMP_FILES[🗂️ Temporary Files<br/>temp/]
        end
    end

    subgraph "Cloud Infrastructure ☁️"
        subgraph "Qdrant Cloud 🌐"
            QDRANT_CLUSTER[🗄️ Qdrant Vector DB<br/>Cloud Cluster]
            VECTOR_STORE[📊 Vector Collections<br/>768D Embeddings]
            METADATA_STORE[📋 Document Metadata<br/>File Paths & Content]
        end
    end

    subgraph "External Resources 🌍"
        GITHUB[📚 GitHub Repositories<br/>Source Code]
        DOCS[📖 Documentation<br/>Markdown/Text Files]
        CONFIG_FILES[⚙️ Configuration Files<br/>YAML/Properties/JSON]
    end

    %% User Interactions
    USER --> TERMINAL
    USER --> IDE
    TERMINAL --> CLI_APP
    IDE --> WORKSPACE

    %% Application Flow
    CLI_APP --> INDEX_SVC
    CLI_APP --> SEARCH_SVC
    INDEX_SVC --> CACHE_SVC
    SEARCH_SVC --> INDEX_SVC
    
    %% Configuration
    SPRING_BOOT --> CONFIG
    CONFIG --> ENV_CONFIG
    
    %% AI Model Integration
    INDEX_SVC -.->|HTTP/REST| OLLAMA_SERVER
    SEARCH_SVC -.->|HTTP/REST| OLLAMA_SERVER
    OLLAMA_SERVER --> EMBEDDING_MODEL
    OLLAMA_SERVER --> CHAT_MODEL
    
    %% Vector Database
    INDEX_SVC -.->|HTTPS/gRPC| QDRANT_CLUSTER
    SEARCH_SVC -.->|HTTPS/gRPC| QDRANT_CLUSTER
    QDRANT_CLUSTER --> VECTOR_STORE
    QDRANT_CLUSTER --> METADATA_STORE
    
    %% Local Storage
    CACHE_SVC --> FILE_CACHE
    SPRING_BOOT --> LOG_FILES
    INDEX_SVC --> TEMP_FILES
    
    %% Data Sources
    WORKSPACE --> GITHUB
    WORKSPACE --> DOCS
    WORKSPACE --> CONFIG_FILES
    INDEX_SVC --> WORKSPACE

    %% Styling
    style USER fill:#e1f5fe
    style CLI_APP fill:#f3e5f5
    style SPRING_BOOT fill:#e8f5e8
    style OLLAMA_SERVER fill:#fff3e0
    style QDRANT_CLUSTER fill:#fce4ec
    style WORKSPACE fill:#f1f8e9
```

### **Network Communication and Data Flow**

```mermaid
graph LR
    subgraph "Local Development Environment"
        subgraph "Spring Boot Application:8080"
            CLI[🎛️ CLI Interface]
            APP[🚀 Spring Boot App]
            CACHE[💾 Local Cache]
        end
        
        subgraph "Ollama AI:11434"
            OLLAMA[🤖 Ollama API]
            MODELS[📊 AI Models]
        end
    end
    
    subgraph "Cloud Services"
        QDRANT[☁️ Qdrant Cloud<br/>443/HTTPS]
        CDN[🌐 Model CDN<br/>Ollama Registry]
    end
    
    subgraph "File System"
        WORKSPACE[📁 Code Workspace]
        CACHE_FILE[📄 .indexer-cache.json]
        LOGS[📝 Application Logs]
    end

    %% API Communications
    CLI -.->|REST API| APP
    APP -.->|HTTP POST<br/>Embeddings| OLLAMA
    APP -.->|HTTPS<br/>Vector Ops| QDRANT
    
    %% Data Persistence
    APP --> CACHE_FILE
    APP --> LOGS
    APP --> WORKSPACE
    CACHE --> CACHE_FILE
    
    %% Model Management
    OLLAMA -.->|Model Download<br/>HTTPS| CDN
    
    %% Data Flow Labels
    APP -.->|"📊 Store Vectors<br/>📋 Query Metadata"| QDRANT
    OLLAMA -.->|"🔄 768D Embeddings<br/>💬 Chat Responses"| APP
    WORKSPACE -.->|"📄 File Content<br/>📂 Directory Scan"| APP
```

### **Deployment Architecture by Environment**

```mermaid
graph TB
    subgraph "Development Environment 🛠️"
        subgraph "Developer Workstation"
            DEV_IDE[💻 IDE/Terminal]
            DEV_SPRING[🚀 Spring Boot Dev]
            DEV_OLLAMA[🤖 Ollama Local]
            DEV_CACHE[💾 Local Cache]
        end
        
        DEV_IDE --> DEV_SPRING
        DEV_SPRING --> DEV_OLLAMA
        DEV_SPRING --> DEV_CACHE
        DEV_SPRING -.->|HTTPS| QDRANT_DEV[☁️ Qdrant Dev Cluster]
    end
    
    subgraph "Production Environment 🚀"
        subgraph "Production Server"
            PROD_CLI[🎛️ Production CLI]
            PROD_SPRING[⚙️ Spring Boot Prod]
            PROD_OLLAMA[🤖 Ollama Server]
            PROD_CACHE[💾 Persistent Cache]
            PROD_LOGS[📝 Centralized Logs]
        end
        
        PROD_CLI --> PROD_SPRING
        PROD_SPRING --> PROD_OLLAMA
        PROD_SPRING --> PROD_CACHE
        PROD_SPRING --> PROD_LOGS
        PROD_SPRING -.->|HTTPS| QDRANT_PROD[☁️ Qdrant Prod Cluster]
    end
    
    subgraph "CI/CD Environment 🔄"
        subgraph "Build Pipeline"
            CI_BUILD[🔨 Maven Build]
            CI_TEST[🧪 Unit Tests]
            CI_PACKAGE[📦 JAR Package]
            CI_DEPLOY[🚀 Deployment]
        end
        
        CI_BUILD --> CI_TEST
        CI_TEST --> CI_PACKAGE
        CI_PACKAGE --> CI_DEPLOY
        CI_DEPLOY -.-> PROD_SPRING
    end
    
    subgraph "Monitoring & Observability 📊"
        METRICS[📈 Application Metrics]
        HEALTH[💚 Health Checks]
        ALERTS[🚨 Alert System]
        
        PROD_SPRING --> METRICS
        PROD_SPRING --> HEALTH
        HEALTH --> ALERTS
    end
    
    %% Environment Connections
    DEV_SPRING -.->|"Promote to Prod"| CI_BUILD
    METRICS -.->|"Feedback"| DEV_SPRING
```

### **Security and Access Control**

```mermaid
graph TB
    subgraph "Security Layers 🔒"
        subgraph "Authentication & Authorization"
            ENV_VARS[🔐 Environment Variables<br/>API Keys & Secrets]
            API_KEYS[🗝️ Qdrant API Key<br/>Encrypted Storage]
            SSL_CERTS[📜 SSL Certificates<br/>HTTPS/TLS 1.3]
        end
        
        subgraph "Network Security"
            FIREWALL[🛡️ Local Firewall<br/>Port Restrictions]
            VPN[🌐 VPN Connection<br/>Secure Tunneling]
            RATE_LIMIT[⏱️ Rate Limiting<br/>API Call Throttling]
        end
        
        subgraph "Data Protection"
            ENCRYPTION[🔒 Data Encryption<br/>At Rest & In Transit]
            BACKUP[💾 Encrypted Backups<br/>Cache & Logs]
            AUDIT[📋 Audit Logging<br/>Access Tracking]
        end
    end
    
    subgraph "Application Security"
        INPUT_VALID[✅ Input Validation<br/>Search Queries]
        ERROR_HANDLE[🚫 Error Handling<br/>No Data Leakage]
        SECURE_CONFIG[⚙️ Secure Configuration<br/>Default Deny]
    end
    
    %% Security Flow
    ENV_VARS --> API_KEYS
    API_KEYS --> SSL_CERTS
    SSL_CERTS --> ENCRYPTION
    
    FIREWALL --> VPN
    VPN --> RATE_LIMIT
    
    ENCRYPTION --> BACKUP
    BACKUP --> AUDIT
    
    INPUT_VALID --> ERROR_HANDLE
    ERROR_HANDLE --> SECURE_CONFIG
    
    %% Cross-cutting Security
    ENV_VARS -.-> INPUT_VALID
    RATE_LIMIT -.-> ERROR_HANDLE
    AUDIT -.-> SECURE_CONFIG
```

### **Scalability and Performance Architecture**

```mermaid
graph TB
    subgraph "Performance Optimization 🚀"
        subgraph "Concurrent Processing"
            VIRTUAL_THREADS[🧵 Virtual Threads<br/>JDK 21 Fibers]
            THREAD_POOL[🏊‍♂️ Thread Pool<br/>Configurable Size]
            ASYNC_PROC[⚡ Async Processing<br/>Non-blocking I/O]
        end
        
        subgraph "Caching Strategy"
            L1_CACHE[💾 L1: Memory Cache<br/>Hot Data]
            L2_CACHE[📄 L2: File Cache<br/>Persistent Storage]
            L3_CACHE[☁️ L3: Vector Cache<br/>Qdrant Optimization]
        end
        
        subgraph "Resource Management"
            MEMORY_OPT[🧠 Memory Optimization<br/>JVM Tuning]
            DISK_OPT[💿 Disk Optimization<br/>Sequential I/O]
            NETWORK_OPT[🌐 Network Optimization<br/>Connection Pooling]
        end
    end
    
    subgraph "Scaling Capabilities 📈"
        subgraph "Horizontal Scaling"
            LOAD_BALANCE[⚖️ Load Balancing<br/>Multiple Instances]
            DISTRIBUTED[🌍 Distributed Processing<br/>Cluster Mode]
            QUEUE[📋 Job Queuing<br/>Background Tasks]
        end
        
        subgraph "Vertical Scaling"
            CPU_SCALE[⚡ CPU Scaling<br/>Multi-core Usage]
            RAM_SCALE[🧠 Memory Scaling<br/>Heap Optimization]
            STORAGE_SCALE[💾 Storage Scaling<br/>SSD Performance]
        end
    end
    
    %% Performance Connections
    VIRTUAL_THREADS --> ASYNC_PROC
    ASYNC_PROC --> THREAD_POOL
    
    L1_CACHE --> L2_CACHE
    L2_CACHE --> L3_CACHE
    
    MEMORY_OPT --> DISK_OPT
    DISK_OPT --> NETWORK_OPT
    
    %% Scaling Connections
    LOAD_BALANCE --> DISTRIBUTED
    DISTRIBUTED --> QUEUE
    
    CPU_SCALE --> RAM_SCALE
    RAM_SCALE --> STORAGE_SCALE
    
    %% Cross-cutting Optimizations
    VIRTUAL_THREADS -.-> CPU_SCALE
    L1_CACHE -.-> RAM_SCALE
    NETWORK_OPT -.-> DISTRIBUTED
```

These deployment diagrams provide a comprehensive view of:

1. **System Architecture**: Complete component deployment across user environment, local machine, and cloud infrastructure
2. **Network Communication**: Data flow and API communications between services
3. **Multi-Environment Support**: Development, production, and CI/CD pipeline architectures
4. **Security Architecture**: Comprehensive security layers and access controls
5. **Performance & Scalability**: Optimization strategies and scaling capabilities

The diagrams show how the Misoto Codebase Indexer integrates with:
- **Local Development Tools**: IDEs, terminals, and file systems
- **AI Platforms**: Ollama for embeddings and natural language processing
- **Cloud Services**: Qdrant Cloud for vector storage and search
- **Infrastructure**: Security, monitoring, and deployment pipelines

## 👥 Use Case Diagrams

### **Primary Use Cases and Actor Interactions**

```mermaid
graph TB
    subgraph "Misoto Codebase Indexer System"
        subgraph "Search Use Cases 🔍"
            UC1[Search Code with<br/>Natural Language]
            UC2[Perform Semantic<br/>Code Search]
            UC3[Execute Text-based<br/>Search]
            UC4[Advanced Multi-filter<br/>Search]
            UC5[Browse Search<br/>Results]
            UC6[Export Search<br/>Results]
        end
        
        subgraph "Indexing Use Cases 📚"
            UC7[Index Codebase<br/>Directory]
            UC8[Monitor Indexing<br/>Progress]
            UC9[Configure Indexing<br/>Settings]
            UC10[Manage File<br/>Cache]
            UC11[Handle Indexing<br/>Errors]
            UC12[Validate File<br/>Types]
        end
        
        subgraph "Configuration Use Cases ⚙️"
            UC13[Setup AI Models<br/>(Ollama)]
            UC14[Configure Vector<br/>Database (Qdrant)]
            UC15[Manage Environment<br/>Variables]
            UC16[Customize File<br/>Priorities]
            UC17[Set Performance<br/>Parameters]
        end
        
        subgraph "Monitoring Use Cases 📊"
            UC18[View System<br/>Status]
            UC19[Track Performance<br/>Metrics]
            UC20[Monitor Resource<br/>Usage]
            UC21[Handle System<br/>Errors]
            UC22[Generate Status<br/>Reports]
        end
        
        subgraph "Management Use Cases 🔧"
            UC23[Clear System<br/>Cache]
            UC24[Restart Indexing<br/>Process]
            UC25[Change Target<br/>Directory]
            UC26[Backup/Restore<br/>Index Data]
            UC27[Update System<br/>Configuration]
        end
    end
    
    subgraph "External Systems 🌐"
        EXT1[Ollama AI Platform]
        EXT2[Qdrant Cloud Service]
        EXT3[File System]
        EXT4[Git Repositories]
        EXT5[IDE Integration]
    end
    
    subgraph "Actors 👥"
        DEV[👨‍💻 Software Developer]
        ADMIN[👨‍🔧 System Administrator]
        ANALYST[👨‍💼 Code Analyst]
        RESEARCHER[👩‍🔬 Researcher]
        TEAM_LEAD[👨‍💼 Team Lead]
    end
    
    %% Developer Use Cases
    DEV --> UC1
    DEV --> UC2
    DEV --> UC3
    DEV --> UC4
    DEV --> UC5
    DEV --> UC7
    DEV --> UC8
    DEV --> UC25
    
    %% System Administrator Use Cases
    ADMIN --> UC9
    ADMIN --> UC10
    ADMIN --> UC13
    ADMIN --> UC14
    ADMIN --> UC15
    ADMIN --> UC16
    ADMIN --> UC17
    ADMIN --> UC23
    ADMIN --> UC24
    ADMIN --> UC26
    ADMIN --> UC27
    
    %% Code Analyst Use Cases
    ANALYST --> UC1
    ANALYST --> UC2
    ANALYST --> UC4
    ANALYST --> UC6
    ANALYST --> UC18
    ANALYST --> UC22
    
    %% Researcher Use Cases
    RESEARCHER --> UC2
    RESEARCHER --> UC4
    RESEARCHER --> UC6
    RESEARCHER --> UC19
    RESEARCHER --> UC22
    
    %% Team Lead Use Cases
    TEAM_LEAD --> UC18
    TEAM_LEAD --> UC19
    TEAM_LEAD --> UC20
    TEAM_LEAD --> UC22
    TEAM_LEAD --> UC27
    
    %% System Dependencies
    UC1 -.-> EXT1
    UC2 -.-> EXT1
    UC2 -.-> EXT2
    UC7 -.-> EXT3
    UC7 -.-> EXT4
    UC13 -.-> EXT1
    UC14 -.-> EXT2
    UC25 -.-> EXT3
    UC5 -.-> EXT5
    
    %% Use Case Relationships
    UC7 --> UC8
    UC8 --> UC11
    UC9 --> UC16
    UC9 --> UC17
    UC13 --> UC14
    UC18 --> UC19
    UC19 --> UC20
    UC23 --> UC24
    
    %% Styling
    style DEV fill:#e1f5fe
    style ADMIN fill:#f3e5f5
    style ANALYST fill:#e8f5e8
    style RESEARCHER fill:#fff3e0
    style TEAM_LEAD fill:#fce4ec
```

### **Detailed Use Case Scenarios**

```mermaid
graph LR
    subgraph "Search Workflow 🔍"
        subgraph "Natural Language Search"
            NL1[Enter Query:<br/>"Find authentication logic"]
            NL2[AI Processing:<br/>Query Understanding]
            NL3[Context Generation:<br/>Search Terms]
            NL4[Vector Search:<br/>Semantic Matching]
            NL5[Results Ranking:<br/>Relevance Scoring]
            NL6[Display Results:<br/>Code Snippets]
            
            NL1 --> NL2 --> NL3 --> NL4 --> NL5 --> NL6
        end
        
        subgraph "Semantic Search"
            SEM1[Enter Technical Query:<br/>"repository pattern"]
            SEM2[Set Similarity:<br/>Threshold 0.7]
            SEM3[Generate Embeddings:<br/>768D Vectors]
            SEM4[Vector Similarity:<br/>Search in Qdrant]
            SEM5[Filter Results:<br/>By Similarity Score]
            SEM6[Present Matches:<br/>With Context]
            
            SEM1 --> SEM2 --> SEM3 --> SEM4 --> SEM5 --> SEM6
        end
        
        subgraph "Text Search"
            TXT1[Enter Keywords:<br/>"@RestController"]
            TXT2[Configure Options:<br/>Case Sensitivity]
            TXT3[Scan Files:<br/>Pattern Matching]
            TXT4[Collect Matches:<br/>Line Numbers]
            TXT5[Format Output:<br/>File Locations]
            
            TXT1 --> TXT2 --> TXT3 --> TXT4 --> TXT5
        end
    end
    
    subgraph "Indexing Workflow 📚"
        subgraph "Initial Indexing"
            IDX1[Select Directory:<br/>Choose Codebase]
            IDX2[Scan Structure:<br/>File Discovery]
            IDX3[Validate Files:<br/>Extension Check]
            IDX4[Priority Sorting:<br/>Critical Files First]
            IDX5[Batch Processing:<br/>Virtual Threads]
            IDX6[Vector Generation:<br/>Embedding Creation]
            IDX7[Store Vectors:<br/>Qdrant Upload]
            IDX8[Update Cache:<br/>File Tracking]
            
            IDX1 --> IDX2 --> IDX3 --> IDX4 --> IDX5 --> IDX6 --> IDX7 --> IDX8
        end
        
        subgraph "Incremental Indexing"
            INC1[Monitor Changes:<br/>File Modification]
            INC2[Check Cache:<br/>Comparison]
            INC3[Queue Updates:<br/>Modified Files]
            INC4[Background Process:<br/>Non-blocking]
            INC5[Merge Vectors:<br/>Update Collection]
            
            INC1 --> INC2 --> INC3 --> INC4 --> INC5
        end
    end
    
    subgraph "Configuration Workflow ⚙️"
        subgraph "System Setup"
            CFG1[Install Ollama:<br/>AI Platform]
            CFG2[Download Models:<br/>nomic-embed-text]
            CFG3[Setup Qdrant:<br/>Cloud Account]
            CFG4[Configure API:<br/>Keys & URLs]
            CFG5[Test Connection:<br/>Verify Setup]
            
            CFG1 --> CFG2 --> CFG3 --> CFG4 --> CFG5
        end
        
        subgraph "Performance Tuning"
            PERF1[Set Thread Pool:<br/>Virtual Threads]
            PERF2[Configure Cache:<br/>Size & Location]
            PERF3[Adjust Batch Size:<br/>Processing Groups]
            PERF4[Set Timeouts:<br/>Network Calls]
            PERF5[Monitor Metrics:<br/>Performance Check]
            
            PERF1 --> PERF2 --> PERF3 --> PERF4 --> PERF5
        end
    end
```

### **Actor Responsibilities and Permissions**

```mermaid
graph TB
    subgraph "Role-Based Access Control 🔐"
        subgraph "Software Developer 👨‍💻"
            DEV_PERM[Permissions:]
            DEV_P1[• Search all indexed code]
            DEV_P2[• View search results]
            DEV_P3[• Index personal projects]
            DEV_P4[• Monitor indexing status]
            DEV_P5[• Change target directories]
            DEV_P6[• Export search results]
            
            DEV_PERM --> DEV_P1
            DEV_PERM --> DEV_P2
            DEV_PERM --> DEV_P3
            DEV_PERM --> DEV_P4
            DEV_PERM --> DEV_P5
            DEV_PERM --> DEV_P6
        end
        
        subgraph "System Administrator 👨‍🔧"
            ADMIN_PERM[Permissions:]
            ADMIN_P1[• Full system configuration]
            ADMIN_P2[• Manage AI model setup]
            ADMIN_P3[• Configure Qdrant connection]
            ADMIN_P4[• Set performance parameters]
            ADMIN_P5[• Clear system cache]
            ADMIN_P6[• Backup/restore data]
            ADMIN_P7[• Monitor system health]
            ADMIN_P8[• Manage user access]
            
            ADMIN_PERM --> ADMIN_P1
            ADMIN_PERM --> ADMIN_P2
            ADMIN_PERM --> ADMIN_P3
            ADMIN_PERM --> ADMIN_P4
            ADMIN_PERM --> ADMIN_P5
            ADMIN_PERM --> ADMIN_P6
            ADMIN_PERM --> ADMIN_P7
            ADMIN_PERM --> ADMIN_P8
        end
        
        subgraph "Code Analyst 👨‍💼"
            ANALYST_PERM[Permissions:]
            ANALYST_P1[• Advanced search features]
            ANALYST_P2[• Generate analysis reports]
            ANALYST_P3[• Export detailed results]
            ANALYST_P4[• Access metrics dashboard]
            ANALYST_P5[• Configure search filters]
            ANALYST_P6[• View system statistics]
            
            ANALYST_PERM --> ANALYST_P1
            ANALYST_PERM --> ANALYST_P2
            ANALYST_PERM --> ANALYST_P3
            ANALYST_PERM --> ANALYST_P4
            ANALYST_PERM --> ANALYST_P5
            ANALYST_PERM --> ANALYST_P6
        end
        
        subgraph "Researcher 👩‍🔬"
            RESEARCHER_PERM[Permissions:]
            RESEARCHER_P1[• Semantic search access]
            RESEARCHER_P2[• Pattern analysis tools]
            RESEARCHER_P3[• Research data export]
            RESEARCHER_P4[• Custom query building]
            RESEARCHER_P5[• Similarity threshold tuning]
            
            RESEARCHER_PERM --> RESEARCHER_P1
            RESEARCHER_PERM --> RESEARCHER_P2
            RESEARCHER_PERM --> RESEARCHER_P3
            RESEARCHER_PERM --> RESEARCHER_P4
            RESEARCHER_PERM --> RESEARCHER_P5
        end
        
        subgraph "Team Lead 👨‍💼"
            LEAD_PERM[Permissions:]
            LEAD_P1[• Team usage monitoring]
            LEAD_P2[• Performance oversight]
            LEAD_P3[• Resource planning]
            LEAD_P4[• Usage reports generation]
            LEAD_P5[• Configuration approval]
            
            LEAD_PERM --> LEAD_P1
            LEAD_PERM --> LEAD_P2
            LEAD_PERM --> LEAD_P3
            LEAD_PERM --> LEAD_P4
            LEAD_PERM --> LEAD_P5
        end
    end
    
    subgraph "Common Use Cases 🔄"
        COMMON[All Users Can:]
        COMMON_P1[• View help documentation]
        COMMON_P2[• Access basic search]
        COMMON_P3[• See indexing status]
        COMMON_P4[• Use interactive CLI]
        
        COMMON --> COMMON_P1
        COMMON --> COMMON_P2
        COMMON --> COMMON_P3
        COMMON --> COMMON_P4
    end
```

### **System Integration Use Cases**

```mermaid
graph TB
    subgraph "External System Integrations 🔌"
        subgraph "Ollama AI Integration"
            OLL1[Install Ollama Platform]
            OLL2[Download AI Models]
            OLL3[Start Ollama Service]
            OLL4[Generate Embeddings]
            OLL5[Process Natural Language]
            OLL6[Monitor AI Performance]
            
            OLL1 --> OLL2 --> OLL3
            OLL3 --> OLL4
            OLL3 --> OLL5
            OLL4 --> OLL6
            OLL5 --> OLL6
        end
        
        subgraph "Qdrant Cloud Integration"
            QDR1[Create Qdrant Account]
            QDR2[Setup Cloud Cluster]
            QDR3[Configure API Access]
            QDR4[Initialize Collections]
            QDR5[Store Vector Data]
            QDR6[Perform Vector Search]
            QDR7[Manage Collection Metadata]
            
            QDR1 --> QDR2 --> QDR3 --> QDR4
            QDR4 --> QDR5
            QDR4 --> QDR6
            QDR5 --> QDR7
            QDR6 --> QDR7
        end
        
        subgraph "File System Integration"
            FS1[Access Local Directories]
            FS2[Read Source Code Files]
            FS3[Monitor File Changes]
            FS4[Cache File Metadata]
            FS5[Handle File Permissions]
            FS6[Manage Temporary Files]
            
            FS1 --> FS2 --> FS3
            FS2 --> FS4
            FS3 --> FS4
            FS1 --> FS5
            FS2 --> FS6
        end
        
        subgraph "IDE Integration"
            IDE1[VS Code Extension]
            IDE2[IntelliJ Plugin]
            IDE3[Search Result Display]
            IDE4[Code Navigation]
            IDE5[Context Menu Integration]
            
            IDE1 --> IDE3 --> IDE4
            IDE2 --> IDE3 --> IDE4
            IDE3 --> IDE5
        end
    end
    
    subgraph "Actor Interactions with External Systems 👥🔌"
        DEV_EXT[Developer] --> OLL4
        DEV_EXT --> QDR6
        DEV_EXT --> FS2
        DEV_EXT --> IDE3
        
        ADMIN_EXT[Administrator] --> OLL1
        ADMIN_EXT --> QDR2
        ADMIN_EXT --> FS5
        
        ANALYST_EXT[Analyst] --> QDR7
        ANALYST_EXT --> FS4
        ANALYST_EXT --> IDE4
        
        RESEARCHER_EXT[Researcher] --> OLL5
        RESEARCHER_EXT --> QDR6
        
        LEAD_EXT[Team Lead] --> OLL6
        LEAD_EXT --> QDR7
    end
```

### **Error Handling and Recovery Use Cases**

```mermaid
graph LR
    subgraph "Error Scenarios and Recovery 🚨"
        subgraph "Indexing Errors"
            ERR1[File Access Denied]
            ERR2[Corrupted File Content]
            ERR3[Network Connection Lost]
            ERR4[Qdrant Service Unavailable]
            ERR5[Ollama Model Not Found]
            ERR6[Insufficient Disk Space]
            
            REC1[Retry with Permissions]
            REC2[Skip and Log Error]
            REC3[Queue for Retry]
            REC4[Switch to Offline Mode]
            REC5[Download Missing Model]
            REC6[Clean Temporary Files]
            
            ERR1 --> REC1
            ERR2 --> REC2
            ERR3 --> REC3
            ERR4 --> REC4
            ERR5 --> REC5
            ERR6 --> REC6
        end
        
        subgraph "Search Errors"
            SERR1[No Results Found]
            SERR2[Search Timeout]
            SERR3[Invalid Query Syntax]
            SERR4[Collection Not Initialized]
            SERR5[AI Model Overloaded]
            
            SREC1[Suggest Alternative Queries]
            SREC2[Extend Timeout Period]
            SREC3[Provide Query Examples]
            SREC4[Initialize Collection]
            SREC5[Queue Request for Retry]
            
            SERR1 --> SREC1
            SERR2 --> SREC2
            SERR3 --> SREC3
            SERR4 --> SREC4
            SERR5 --> SREC5
        end
        
        subgraph "System Errors"
            SYSERR1[Configuration Missing]
            SYSERR2[Cache Corruption]
            SYSERR3[Memory Overflow]
            SYSERR4[Thread Pool Exhaustion]
            
            SYSREC1[Load Default Config]
            SYSREC2[Rebuild Cache]
            SYSREC3[Restart with More Memory]
            SYSREC4[Scale Thread Pool]
            
            SYSERR1 --> SYSREC1
            SYSERR2 --> SYSREC2
            SYSERR3 --> SYSREC3
            SYSERR4 --> SYSREC4
        end
    end
```

The codebase has been refactored to implement several design patterns for better maintainability and extensibility:

- **Command Pattern**: For encapsulating indexing status commands
- **Strategy Pattern**: To define and switch between search algorithms
- **Observer Pattern**: For notifying status updates to the CLI
- **Factory Pattern**: To create document instances for indexing
- **Repository Pattern**: For abstracting file cache operations
- **Dependency Injection**: Using Spring's @Autowired for service dependencies

### **Command Pattern Example**

```java
// Command.java - Command interface
public interface Command {
    void execute();
}

// IndexingStatusCommand.java - Concrete command
public class IndexingStatusCommand implements Command {
    private final IndexingService indexingService;

    public IndexingStatusCommand(IndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @Override
    public void execute() {
        indexingService.displayStatus();
    }
}
```

### **Strategy Pattern Example**

```java
// SearchStrategy.java - Strategy interface
public interface SearchStrategy {
    List<SearchResult> search(String query, double threshold);
}

// SemanticSearchStrategy.java - Concrete strategy
public class SemanticSearchStrategy implements SearchStrategy {
    @Override
    public List<SearchResult> search(String query, double threshold) {
        // Implementation for semantic search using embeddings
    }
}
```

### **Observer Pattern Example**

```java
// IndexingStatusObserver.java - Observer interface
public interface IndexingStatusObserver {
    void onStatusUpdate(IndexingStatus status);
}

// CLI.java - Concrete observer
public class CLI implements IndexingStatusObserver {
    @Override
    public void onStatusUpdate(IndexingStatus status) {
        displayStatus(status);
    }
}
```

### **Factory Pattern Example**

```java
// DocumentFactory.java - Factory interface
public interface DocumentFactory {
    Document createDocument(File file);
}

// TextDocumentFactory.java - Concrete factory
public class TextDocumentFactory implements DocumentFactory {
    @Override
    public Document createDocument(File file) {
        return new TextDocument(file);
    }
}
```

### **Repository Pattern Example**

```java
// FileCacheRepository.java - Repository interface
public interface FileCacheRepository {
    void save(FileCacheEntry entry);
    FileCacheEntry find(String filePath);
}

// FileCacheRepositoryImpl.java - Repository implementation
public class FileCacheRepositoryImpl implements FileCacheRepository {
    @Override
    public void save(FileCacheEntry entry) {
        // Save to cache
    }

    @Override
    public FileCacheEntry find(String filePath) {
        // Find from cache
    }
}
```

### **Dependency Injection Example**

```java
// SearchService.java - Service with dependencies
@Service
public class SearchService {
    private final FileSearchService fileSearchService;
    private final HybridSearchService hybridSearchService;

    @Autowired
    public SearchService(FileSearchService fileSearchService, HybridSearchService hybridSearchService) {
        this.fileSearchService = fileSearchService;
        this.hybridSearchService = hybridSearchService;
    }

    public List<SearchResult> search(String query) {
        // Use injected services to perform search
    }
}
```
