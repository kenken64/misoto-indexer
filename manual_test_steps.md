# Manual Test Steps to Find app.route Content in Qdrant

## Step 1: Start the Application
```bash
mvn spring-boot:run
```

## Step 2: Set Up Indexing
1. Choose option **6** (Index Codebase)
2. Set directory to: `./codebase/dssi-day3-ollama`
3. Wait for indexing to complete
4. Look for messages like: "✅ Complete indexing finished! X files indexed"

## Step 3: Check Indexing Status
1. Choose option **2** (Indexing Status)
2. Verify:
   - **Indexed files > 0** (should be around 33 files)
   - **Current directory**: `./codebase/dssi-day3-ollama`
   - **Collection name**: `codebase-index-dssi-day3-ollama`

## Step 4: Test Specific Queries for app.py Content

Try these queries with **option 3** (Semantic Search):

### Query 1: "@app.route"
```
Query: @app.route
Expected: Should find Flask route decorators
```

### Query 2: "Flask"
```
Query: Flask
Expected: Should find Flask framework usage
```

### Query 3: "/api/generate"
```
Query: /api/generate
Expected: Should find generate-sql endpoint
```

### Query 4: "from flask import"
```
Query: from flask import
Expected: Should find Flask imports
```

### Query 5: "jsonify"
```
Query: jsonify
Expected: Should find JSON response functions
```

### Query 6: "REST API endpoints"
```
Query: REST API endpoints
Expected: Should find app.py with our enhanced query system
```

## Step 5: Look for Debug Output

For each query, you should see:
```
🔍 ORIGINAL QUERY: '[your query]'
🔍 ENHANCED QUERY: '[your query] Flask Spring Boot @app.route...'
🔍 FINAL QUERY TO VECTOR DATABASE: '[enhanced query]'
📊 Raw documents retrieved: X
📊 Documents after alternative ranking: Y
```

## Step 6: Analyze Results

### If "Raw documents retrieved: 0"
- **Problem**: Content not indexed in Qdrant
- **Solution**: Repeat indexing process, check for errors

### If "Raw documents retrieved: X" but "alternative ranking: 0"
- **Problem**: Our ranking algorithm is too strict
- **Solution**: Alternative ranking needs tuning

### If you see results:
- Check if **app.py** appears in the file paths
- Look for content containing `@app.route`, `Flask`, `/api/`

## Step 7: Expected app.py Content

The queries should find content like:
```python
@app.route('/')
def index():
    return render_template('index.html')

@app.route('/api/generate-sql', methods=['POST'])
def generate_sql():
    # ... API logic

@app.route('/api/validate-sql', methods=['POST'])
def validate_sql():
    # ... validation logic

@app.route('/api/status')
def status():
    # ... status check
```

## Troubleshooting

### No Results at All:
1. Check Qdrant Cloud connection in application logs
2. Verify indexing completed successfully
3. Try text search (option 4) with "@app.route" to see if file exists

### Results Found but No app.py:
1. Our REST API scoring might need adjustment
2. Try simpler queries first ("Flask", "def")
3. Check if content is chunked/split differently

### Connection Issues:
1. Verify Qdrant Cloud credentials in .env file
2. Check network connectivity
3. Look for authentication errors in logs

## Success Criteria

✅ **Success**: Query "REST API endpoints" returns app.py as top result
✅ **Success**: Query "@app.route" finds Flask route decorators
✅ **Success**: Query "/api/generate" finds the generate-sql endpoint

The key is to see the debug output showing:
- Enhanced queries being generated
- Raw documents being retrieved from Qdrant
- Alternative ranking processing the results
- Final results containing app.py with REST API content