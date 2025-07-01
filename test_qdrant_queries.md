# Direct Qdrant Test Queries

## Manual Test Instructions

1. **Start the application**:
   ```bash
   mvn spring-boot:run
   ```

2. **Set indexing directory** (Option 6):
   - Choose "Index Codebase" 
   - Set directory to: `./codebase/dssi-day3-ollama`
   - Wait for indexing to complete

3. **Test these specific search queries** (Option 3 - Semantic Search):

### Test Queries to Find app.py Content:

1. **Query: "@app.route"**
   - Should find Flask route decorators directly

2. **Query: "Flask"**
   - Should find Flask framework usage

3. **Query: "from flask import"**
   - Should find Flask import statements

4. **Query: "/api/generate"**
   - Should find the generate-sql API endpoint

5. **Query: "/api/validate"**
   - Should find the validate-sql API endpoint

6. **Query: "/api/status"**
   - Should find the status API endpoint

7. **Query: "jsonify"**
   - Should find JSON response functions

8. **Query: "methods=['POST']"**
   - Should find POST method definitions

9. **Query: "def generate_sql"**
   - Should find the specific function

10. **Query: "Flask(__name__)"**
    - Should find Flask app initialization

### Expected Results:

For each query, you should see debug output like:
```
🔍 ENHANCED QUERY: '[original query] Flask Spring Boot @app.route...'
🔍 FINAL QUERY TO VECTOR DATABASE: '[enhanced query]'
📊 Raw documents retrieved: X
📊 Documents after alternative ranking: Y
```

### What to Look For:

1. **If "Raw documents retrieved: 0"** - The content is not indexed in Qdrant
2. **If "Raw documents retrieved: X" but "alternative ranking: 0"** - Our ranking is too aggressive
3. **If you see results** - Check if app.py is in the results

### Debugging Commands:

If no results found, try these in sequence:

1. **Check indexing status**: Option 2 (Indexing Status)
2. **Restart indexing**: Option 6 → restart indexing process
3. **Try text search**: Option 4 with "@app.route" to see if file exists
4. **Try simple queries**: Start with single words like "Flask" or "def"

### Expected app.py Content Patterns:

The queries should find content containing:
- `@app.route('/')`
- `@app.route('/api/generate-sql', methods=['POST'])`
- `@app.route('/api/validate-sql', methods=['POST'])`
- `@app.route('/api/status')`
- `from flask import Flask, render_template, request, jsonify`
- `app = Flask(__name__)`
- `def generate_sql():`
- `def validate_sql():`
- `def status():`