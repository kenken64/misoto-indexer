package sg.edu.nus.iss.codebase.indexer.service;

import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for analyzing source code files and creating intelligent summaries
 * with line number mappings for semantic search
 */
@Service
public class CodeAnalysisService {

    /**
     * Analyze a file and create a summary with line number mappings
     */
    public FileAnalysis analyzeFile(Path filePath) throws IOException {
        String content = Files.readString(filePath);
        String[] lines = content.split("\n");
        String fileName = filePath.getFileName().toString();
        
        FileAnalysis analysis = new FileAnalysis(fileName, filePath.toString());
        
        // Detect file type and apply appropriate analysis
        if (fileName.endsWith(".py")) {
            analyzePythonFile(lines, analysis);
        } else if (fileName.endsWith(".java")) {
            analyzeJavaFile(lines, analysis);
        } else if (fileName.endsWith(".js") || fileName.endsWith(".ts")) {
            analyzeJavaScriptFile(lines, analysis);
        } else {
            analyzeGenericFile(lines, analysis);
        }
        
        return analysis;
    }

    /**
     * Analyze Python files with special focus on Flask routes, functions, classes
     */
    private void analyzePythonFile(String[] lines, FileAnalysis analysis) {
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            int lineNumber = i + 1;
            
            // Detect Flask routes with high priority
            if (line.startsWith("@app.route")) {
                String context = extractContext(lines, i, 5);
                String route = extractRoute(line);
                analysis.addRestApiEndpoint(lineNumber, route, line, context);
                analysis.addSummaryEntry("REST API endpoint: " + route + " (line " + lineNumber + ")");
            }
            
            // Detect FastAPI routes
            if (line.contains("@app.get") || line.contains("@app.post") || 
                line.contains("@app.put") || line.contains("@app.delete")) {
                String context = extractContext(lines, i, 5);
                String route = extractFastAPIRoute(line);
                analysis.addRestApiEndpoint(lineNumber, route, line, context);
                analysis.addSummaryEntry("FastAPI endpoint: " + route + " (line " + lineNumber + ")");
            }
            
            // Detect function definitions
            if (line.startsWith("def ")) {
                String functionName = extractFunctionName(line);
                String context = extractContext(lines, i, 3);
                analysis.addFunction(lineNumber, functionName, line, context);
                analysis.addSummaryEntry("Function: " + functionName + " (line " + lineNumber + ")");
            }
            
            // Detect class definitions
            if (line.startsWith("class ")) {
                String className = extractClassName(line);
                String context = extractContext(lines, i, 3);
                analysis.addClass(lineNumber, className, line, context);
                analysis.addSummaryEntry("Class: " + className + " (line " + lineNumber + ")");
            }
            
            // Detect imports
            if (line.startsWith("import ") || line.startsWith("from ")) {
                analysis.addImport(lineNumber, line);
            }
        }
    }

    /**
     * Analyze Java files focusing on Spring annotations, methods, classes
     */
    private void analyzeJavaFile(String[] lines, FileAnalysis analysis) {
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            int lineNumber = i + 1;
            
            // Detect Spring REST endpoints
            if (line.contains("@RequestMapping") || line.contains("@GetMapping") || 
                line.contains("@PostMapping") || line.contains("@PutMapping") || 
                line.contains("@DeleteMapping")) {
                String context = extractContext(lines, i, 5);
                String endpoint = extractSpringEndpoint(line);
                analysis.addRestApiEndpoint(lineNumber, endpoint, line, context);
                analysis.addSummaryEntry("Spring REST endpoint: " + endpoint + " (line " + lineNumber + ")");
            }
            
            // Detect method definitions
            if (line.contains("public") && line.contains("(") && !line.contains("class")) {
                String methodName = extractJavaMethodName(line);
                String context = extractContext(lines, i, 3);
                analysis.addFunction(lineNumber, methodName, line, context);
                analysis.addSummaryEntry("Method: " + methodName + " (line " + lineNumber + ")");
            }
            
            // Detect class definitions
            if (line.contains("class ") && (line.contains("public") || line.contains("private"))) {
                String className = extractJavaClassName(line);
                String context = extractContext(lines, i, 3);
                analysis.addClass(lineNumber, className, line, context);
                analysis.addSummaryEntry("Class: " + className + " (line " + lineNumber + ")");
            }
        }
    }

    /**
     * Analyze JavaScript/TypeScript files focusing on Express routes, functions
     */
    private void analyzeJavaScriptFile(String[] lines, FileAnalysis analysis) {
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            int lineNumber = i + 1;
            
            // Detect Express routes
            if (line.contains("app.get") || line.contains("app.post") || 
                line.contains("app.put") || line.contains("app.delete") ||
                line.contains("router.get") || line.contains("router.post")) {
                String context = extractContext(lines, i, 5);
                String route = extractExpressRoute(line);
                analysis.addRestApiEndpoint(lineNumber, route, line, context);
                analysis.addSummaryEntry("Express endpoint: " + route + " (line " + lineNumber + ")");
            }
            
            // Detect function definitions
            if (line.contains("function ") || line.matches(".*\\w+\\s*=\\s*\\(.*\\)\\s*=>.*")) {
                String functionName = extractJSFunctionName(line);
                String context = extractContext(lines, i, 3);
                analysis.addFunction(lineNumber, functionName, line, context);
                analysis.addSummaryEntry("Function: " + functionName + " (line " + lineNumber + ")");
            }
        }
    }

    /**
     * Generic analysis for other file types
     */
    private void analyzeGenericFile(String[] lines, FileAnalysis analysis) {
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            int lineNumber = i + 1;
            
            // Look for common patterns
            if (line.contains("TODO") || line.contains("FIXME")) {
                analysis.addSummaryEntry("Note: " + line + " (line " + lineNumber + ")");
            }
        }
    }

    /**
     * Extract surrounding context for a line
     */
    private String extractContext(String[] lines, int centerIndex, int contextSize) {
        int start = Math.max(0, centerIndex - contextSize);
        int end = Math.min(lines.length, centerIndex + contextSize + 1);
        
        StringBuilder context = new StringBuilder();
        for (int i = start; i < end; i++) {
            context.append(lines[i]).append("\n");
        }
        return context.toString();
    }

    // Extraction methods for different patterns
    private String extractRoute(String line) {
        Pattern pattern = Pattern.compile("@app\\.route\\(['\"]([^'\"]+)['\"]");
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? matcher.group(1) : "unknown";
    }

    private String extractFastAPIRoute(String line) {
        Pattern pattern = Pattern.compile("@app\\.(get|post|put|delete)\\(['\"]([^'\"]+)['\"]");
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? matcher.group(2) : "unknown";
    }

    private String extractSpringEndpoint(String line) {
        Pattern pattern = Pattern.compile("value\\s*=\\s*['\"]([^'\"]+)['\"]");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        pattern = Pattern.compile("@\\w+Mapping\\(['\"]([^'\"]+)['\"]");
        matcher = pattern.matcher(line);
        return matcher.find() ? matcher.group(1) : "unknown";
    }

    private String extractExpressRoute(String line) {
        Pattern pattern = Pattern.compile("\\.(get|post|put|delete)\\(['\"]([^'\"]+)['\"]");
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? matcher.group(2) : "unknown";
    }

    private String extractFunctionName(String line) {
        Pattern pattern = Pattern.compile("def\\s+(\\w+)");
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? matcher.group(1) : "unknown";
    }

    private String extractClassName(String line) {
        Pattern pattern = Pattern.compile("class\\s+(\\w+)");
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? matcher.group(1) : "unknown";
    }

    private String extractJavaMethodName(String line) {
        Pattern pattern = Pattern.compile("\\w+\\s+(\\w+)\\s*\\(");
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? matcher.group(1) : "unknown";
    }

    private String extractJavaClassName(String line) {
        Pattern pattern = Pattern.compile("class\\s+(\\w+)");
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? matcher.group(1) : "unknown";
    }

    private String extractJSFunctionName(String line) {
        if (line.contains("function ")) {
            Pattern pattern = Pattern.compile("function\\s+(\\w+)");
            Matcher matcher = pattern.matcher(line);
            return matcher.find() ? matcher.group(1) : "unknown";
        } else {
            Pattern pattern = Pattern.compile("(\\w+)\\s*=");
            Matcher matcher = pattern.matcher(line);
            return matcher.find() ? matcher.group(1) : "unknown";
        }
    }

    /**
     * Data class to hold file analysis results
     */
    public static class FileAnalysis {
        private final String fileName;
        private final String filePath;
        private final List<String> summary;
        private final List<CodeElement> restApiEndpoints;
        private final List<CodeElement> functions;
        private final List<CodeElement> classes;
        private final List<CodeElement> imports;

        public FileAnalysis(String fileName, String filePath) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.summary = new ArrayList<>();
            this.restApiEndpoints = new ArrayList<>();
            this.functions = new ArrayList<>();
            this.classes = new ArrayList<>();
            this.imports = new ArrayList<>();
        }

        public void addSummaryEntry(String entry) {
            summary.add(entry);
        }

        public void addRestApiEndpoint(int lineNumber, String name, String code, String context) {
            restApiEndpoints.add(new CodeElement(lineNumber, name, code, context));
        }

        public void addFunction(int lineNumber, String name, String code, String context) {
            functions.add(new CodeElement(lineNumber, name, code, context));
        }

        public void addClass(int lineNumber, String name, String code, String context) {
            classes.add(new CodeElement(lineNumber, name, code, context));
        }

        public void addImport(int lineNumber, String code) {
            imports.add(new CodeElement(lineNumber, "import", code, ""));
        }

        // Getters
        public String getFileName() { return fileName; }
        public String getFilePath() { return filePath; }
        public List<String> getSummary() { return summary; }
        public List<CodeElement> getRestApiEndpoints() { return restApiEndpoints; }
        public List<CodeElement> getFunctions() { return functions; }
        public List<CodeElement> getClasses() { return classes; }
        public List<CodeElement> getImports() { return imports; }

        /**
         * Generate a searchable summary text for this file
         */
        public String getSearchableSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("File: ").append(fileName).append("\n");
            sb.append("Path: ").append(filePath).append("\n\n");
            
            if (!restApiEndpoints.isEmpty()) {
                sb.append("REST API Endpoints:\n");
                for (CodeElement endpoint : restApiEndpoints) {
                    sb.append("- Line ").append(endpoint.getLineNumber())
                      .append(": ").append(endpoint.getName())
                      .append(" (").append(endpoint.getCode()).append(")\n");
                }
                sb.append("\n");
            }
            
            if (!functions.isEmpty()) {
                sb.append("Functions:\n");
                for (CodeElement function : functions) {
                    sb.append("- Line ").append(function.getLineNumber())
                      .append(": ").append(function.getName()).append("\n");
                }
                sb.append("\n");
            }
            
            if (!classes.isEmpty()) {
                sb.append("Classes:\n");
                for (CodeElement cls : classes) {
                    sb.append("- Line ").append(cls.getLineNumber())
                      .append(": ").append(cls.getName()).append("\n");
                }
                sb.append("\n");
            }
            
            return sb.toString();
        }
    }

    /**
     * Data class representing a code element with line number and context
     */
    public static class CodeElement {
        private final int lineNumber;
        private final String name;
        private final String code;
        private final String context;

        public CodeElement(int lineNumber, String name, String code, String context) {
            this.lineNumber = lineNumber;
            this.name = name;
            this.code = code;
            this.context = context;
        }

        public int getLineNumber() { return lineNumber; }
        public String getName() { return name; }
        public String getCode() { return code; }
        public String getContext() { return context; }
    }
}
