# Enhanced Semantic Search Implementation

## Overview

The semantic search functionality has been significantly enhanced to implement a **two-stage intelligent search process** that uses Ollama AI to analyze search queries and identify relevant frameworks before querying the vector database.

## Key Enhancements

### 1. Two-Stage Search Architecture

**Stage 1: Intelligent Query Analysis**
- Uses Ollama to analyze the search prompt
- Identifies relevant frameworks (Flask, Spring Boot, Express.js, etc.)
- Extracts specific syntax patterns (@app.route, @GetMapping, etc.)
- Determines related programming concepts

**Stage 2: Enhanced Vector Search**
- Constructs an enhanced query with framework-specific terms
- Searches the vector database with expanded context
- Prioritizes framework documentation and project analysis results

### 2. Framework-Aware Query Expansion

The system now understands and expands queries intelligently:

#### Example Query Flow:
```
User Query: "REST API endpoints"
↓
Ollama Analysis: 
- Frameworks: Flask, Spring Boot, Express.js
- Syntax: @app.route, @GetMapping, @PostMapping, app.get
- Concepts: HTTP methods, route decorators, endpoint functions
↓
Enhanced Query: "REST API endpoints Flask Spring Boot Express.js @app.route @GetMapping @PostMapping app.get HTTP methods route decorators endpoint functions"
↓
Vector Search: Returns code examples + framework documentation
```

### 3. Comprehensive Framework Pattern Recognition

The system recognizes and searches for patterns across multiple frameworks:

**Web Frameworks:**
- **Flask:** `@app.route('/path')`, `methods=['GET', 'POST']`, `render_template()`, `jsonify()`
- **Spring Boot:** `@RestController`, `@GetMapping("/path")`, `@PostMapping`, `@RequestMapping`
- **Express.js:** `app.get('/path', handler)`, `app.post()`, `router.use()`

**Database/ORM:**
- **Flask-SQLAlchemy:** `db.Model`, `db.Column`, `query.filter()`
- **Spring Data JPA:** `@Repository`, `@Entity`, `findBy`, `@Query`
- **Mongoose:** `Schema`, `model`, `find()`, `save()`

**Authentication:**
- **Flask:** `@login_required`, `session`, `flash()`
- **Spring Security:** `@PreAuthorize`, `@Secured`, `SecurityConfig`
- **JWT:** `token`, `authenticate`, `authorization`

### 4. Intelligent Result Prioritization

Results are prioritized based on document type and query relevance:

**High Priority for Framework Queries:**
1. Framework Documentation (score: 100)
2. Project Analysis (score: 90)
3. Dependency Information (score: 80)
4. Code Examples (score: 60)

**High Priority for Syntax Queries (e.g., "@app.route"):**
1. Framework Documentation (score: 100)
2. REST API Endpoints (score: 90)
3. Function Definitions (score: 60)

### 5. Fallback Enhancement

If Ollama is unavailable, the system uses an enhanced fallback that:
- Maps common programming terms to specific syntax
- Expands framework names to include related patterns
- Adds relevant documentation search terms

## Implementation Details

### Modified Files

1. **HybridSearchService.java**
   - `performIntelligentQueryAnalysis()`: Ollama-based query analysis
   - `createQueryAnalysisPrompt()`: Comprehensive framework pattern prompts
   - `parseFrameworkAnalysisResponse()`: Parses Ollama's structured response
   - `enhanceQueryForProgramming()`: Enhanced fallback query expansion
   - Updated result prioritization scoring

2. **ProjectAnalysisService.java** (Previously implemented)
   - Dynamic framework detection using Ollama
   - Framework documentation generation
   - Project type and dependency analysis

### API Integration

The enhanced search is automatically used when calling:
```
POST /api/search/hybrid
{
  "query": "REST API endpoints",
  "maxResults": 10
}
```

## Usage Examples

### Example 1: Framework Syntax Search
```
Query: "@app.route('/api/status')"
Results: 
- Flask route examples in app.py
- Flask framework documentation on route decorators
- HTTP endpoint patterns and syntax
```

### Example 2: General Concept Search
```
Query: "REST API endpoints"
Results:
- Flask @app.route examples
- Spring Boot @GetMapping examples  
- Framework documentation for REST APIs
- Project analysis showing web frameworks
```

### Example 3: Dependency Search
```
Query: "python dependencies"
Results:
- requirements.txt content
- Project analysis with dependency list
- Framework documentation for detected libraries
- Import statements and package usage
```

## Benefits

1. **More Accurate Results:** AI-powered query analysis finds relevant content even with vague queries
2. **Framework-Aware:** Understands different framework syntaxes and patterns
3. **Comprehensive Coverage:** Searches both code and documentation
4. **Intelligent Prioritization:** Most relevant results appear first
5. **Fallback Resilience:** Works even when AI services are unavailable

## Testing

Use the provided test script to validate functionality:
```bash
python test-enhanced-semantic-search.py
```

The test validates:
- Framework pattern recognition
- Syntax-specific searches
- Documentation retrieval
- Project analysis integration
- Result prioritization

## Future Enhancements

Potential areas for further improvement:
1. **Multi-language Support:** Extend to more programming languages
2. **Custom Framework Patterns:** Allow users to define custom patterns
3. **Learning Capabilities:** Improve based on user search behavior
4. **Performance Optimization:** Cache Ollama responses for common queries
5. **Advanced Filtering:** More sophisticated result filtering options
