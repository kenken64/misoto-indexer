package sg.edu.nus.iss.codebase.indexer.test;

import sg.edu.nus.iss.codebase.indexer.scoring.RESTEndpointScoring;
import org.springframework.ai.document.Document;
import java.util.HashMap;
import java.util.Map;

/**
 * Standalone test to verify our REST API scoring logic works correctly
 * This test validates the REST endpoint detection and scoring algorithms.
 */
public class RESTScoringTest {
    
    public static void main(String[] args) {
        try {
            System.out.println("🧪 TESTING REST API SCORING LOGIC");
            System.out.println("=====================================");
            
            // Test with sample Flask app content
            String flaskAppContent = createSampleFlaskApp();
            testRESTScoring(flaskAppContent, "REST API endpoints");
            
            // Test with different query patterns
            testRESTScoring(flaskAppContent, "Flask API");
            testRESTScoring(flaskAppContent, "endpoint");
            testRESTScoring(flaskAppContent, "route");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testRESTScoring(String content, String query) {
        System.out.println("\n🔍 Testing query: '" + query + "'");
        System.out.println("-".repeat(50));
        
        // Create a Document object for testing
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("filename", "app.py");
        metadata.put("filepath", "test-codebase/python/app.py");
        
        Document testDoc = new Document(content, metadata);
        
        // Calculate score using our scoring logic
        double score = RESTEndpointScoring.calculateRESTScore(testDoc, query);
        
        System.out.println("📊 Final Score: " + String.format("%.3f", score));
        
        // Generate detailed breakdown
        String breakdown = RESTEndpointScoring.generateScoringBreakdown(testDoc, query);
        System.out.println(breakdown);
        
        // Evaluate result
        if (score > 0.7) {
            System.out.println("✅ EXCELLENT: High relevance score");
        } else if (score > 0.5) {
            System.out.println("✅ GOOD: Moderate relevance score");
        } else if (score > 0.3) {
            System.out.println("⚠️ FAIR: Low relevance score");
        } else {
            System.out.println("❌ POOR: Very low relevance score");
        }
    }
    
    private static String createSampleFlaskApp() {
        return """
            from flask import Flask, render_template, request, jsonify, flash
            import subprocess
            import json
            import logging
            import os
            from datetime import datetime
            
            app = Flask(__name__)
            app.secret_key = 'your-secret-key-change-this'
            
            @app.route('/')
            def index():
                return render_template('index.html')
            
            @app.route('/api/generate-sql', methods=['POST'])
            def generate_sql():
                try:
                    data = request.get_json()
                    
                    if not data:
                        return jsonify({
                            'success': False,
                            'error': 'No data provided'
                        }), 400
                    
                    schema = data.get('schema', '').strip()
                    query = data.get('query', '').strip()
                    
                    if not schema:
                        return jsonify({
                            'success': False,
                            'error': 'Database schema is required'
                        }), 400
                    
                    return jsonify({
                        'success': True,
                        'sql_query': 'SELECT * FROM users',
                        'timestamp': datetime.now().isoformat()
                    })
                
                except Exception as e:
                    return jsonify({
                        'success': False,
                        'error': f'Internal error: {str(e)}'
                    }), 500
            
            @app.route('/api/validate-sql', methods=['POST'])
            def validate_sql():
                try:
                    data = request.get_json()
                    sql_query = data.get('sql', '').strip()
                    
                    if not sql_query:
                        return jsonify({
                            'success': False,
                            'error': 'SQL query is required'
                        })
                    
                    return jsonify({
                        'success': True,
                        'is_valid': True,
                        'message': 'SQL is valid'
                    })
                
                except Exception as e:
                    return jsonify({
                        'success': False,
                        'error': str(e)
                    })
            
            @app.route('/api/status')
            def status():
                return jsonify({
                    'status': 'running',
                    'timestamp': datetime.now().isoformat()
                })
            
            if __name__ == '__main__':
                app.run(debug=True, host='0.0.0.0', port=5000)
            """;
    }
}
