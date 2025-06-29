package sg.edu.nus.iss.codebase.indexer.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Service for discovering project types and analyzing dependencies/libraries
 */
@Service
public class ProjectAnalysisService {

    @Autowired
    private ChatModel chatModel;

    // Configuration for AI provider preference
    private static final String AI_PROVIDER_PREFERENCE = System.getProperty("ai.provider.preference", "ollama,anthropic");
    
    /**
     * Call AI service with fallback between providers
     */
    private String callAIService(String prompt) {
        String[] providers = AI_PROVIDER_PREFERENCE.split(",");
        
        for (String provider : providers) {
            try {
                switch (provider.trim().toLowerCase()) {
                    case "ollama":
                        return callOllama(prompt);
                    case "anthropic":
                        return callAnthropic(prompt);
                    default:
                        System.err.println("⚠️ Unknown AI provider: " + provider);
                }
            } catch (Exception e) {
                System.err.println("❌ Error with " + provider + " provider: " + e.getMessage());
                // Continue to next provider
            }
        }
        
        // If all providers fail, return null to trigger fallback
        return null;
    }
    
    private String callOllama(String prompt) {
        // Use the existing Spring AI ChatModel (configured for Ollama)
        return chatModel.call(prompt);
    }
    
    private String callAnthropic(String prompt) {
        // For now, we'll use the same ChatModel but this could be enhanced
        // to use a specific Anthropic client in the future
        System.out.println("🤖 Using Anthropic AI provider");
        return chatModel.call(prompt);
    }

    /**
     * Analyze a project directory to determine its type and extract dependencies
     */
    public ProjectAnalysis analyzeProject(Path projectPath) throws IOException {
        ProjectAnalysis analysis = new ProjectAnalysis(projectPath.toString());
        
        // Detect project type based on files and structure
        ProjectType projectType = detectProjectType(projectPath);
        analysis.setProjectType(projectType);
        
        // Extract dependencies based on project type
        switch (projectType) {
            case PYTHON:
                extractPythonDependencies(projectPath, analysis);
                break;
            case JAVA_MAVEN:
                extractMavenDependencies(projectPath, analysis);
                break;
            case JAVA_GRADLE:
                extractGradleDependencies(projectPath, analysis);
                break;
            case JAVASCRIPT_NODE:
                extractNodeDependencies(projectPath, analysis);
                break;
            case SPRING_BOOT:
                extractSpringBootDependencies(projectPath, analysis);
                break;
            case FLASK:
                extractFlaskDependencies(projectPath, analysis);
                break;
            case REACT:
                extractReactDependencies(projectPath, analysis);
                break;
            case REACT_NATIVE:
                extractReactNativeDependencies(projectPath, analysis);
                break;
            case ANGULAR:
                extractAngularDependencies(projectPath, analysis);
                break;
            case VUE:
                extractVueDependencies(projectPath, analysis);
                break;
            case FLUTTER:
                extractFlutterDependencies(projectPath, analysis);
                break;
            case RUST:
                extractRustDependencies(projectPath, analysis);
                break;
            case GO:
                extractGoDependencies(projectPath, analysis);
                break;
            case CSHARP_DOTNET:
                extractDotNetDependencies(projectPath, analysis);
                break;
            case MIXED:
                extractMixedProjectDependencies(projectPath, analysis);
                break;
            default:
                // For unknown projects, try to detect common patterns
                extractGenericDependencies(projectPath, analysis);
                break;
        }
        
        return analysis;
    }

    /**
     * Detect project type based on directory structure and key files
     */
    private ProjectType detectProjectType(Path projectPath) throws IOException {
        Set<String> files = new HashSet<>();
        Set<String> directories = new HashSet<>();
        
        // Scan the project directory for key indicators
        try (Stream<Path> paths = Files.walk(projectPath, 2)) {
            paths.forEach(path -> {
                String name = path.getFileName().toString();
                if (Files.isDirectory(path)) {
                    directories.add(name);
                } else {
                    files.add(name);
                }
            });
        } catch (IOException e) {
            // If we can't walk the directory, try to list immediate contents
            try (Stream<Path> paths = Files.list(projectPath)) {
                paths.forEach(path -> {
                    String name = path.getFileName().toString();
                    if (Files.isDirectory(path)) {
                        directories.add(name);
                    } else {
                        files.add(name);
                    }
                });
            }
        }
        
        // Check for Flutter (most specific first)
        if (files.contains("pubspec.yaml")) {
            return ProjectType.FLUTTER;
        }
        
        // Check for Angular (most definitive)
        if (files.contains("angular.json")) {
            return ProjectType.ANGULAR;
        }
        
        // Check for React Native
        if (files.contains("react-native.config.js") || 
            (files.contains("package.json") && hasReactNativeIndicators(projectPath))) {
            return ProjectType.REACT_NATIVE;
        }
        
        // Check for Vue.js
        if (files.contains("vue.config.js") || 
            (files.contains("package.json") && hasVueIndicators(projectPath))) {
            return ProjectType.VUE;
        }
        
        // Check for React
        if (files.contains("package.json") && hasReactIndicators(projectPath)) {
            return ProjectType.REACT;
        }
        
        // Check for Spring Boot indicators
        if (files.contains("pom.xml") && (
            hasSpringBootIndicators(projectPath) || 
            directories.contains("src") && 
            files.stream().anyMatch(f -> f.contains("Application.java"))
        )) {
            return ProjectType.SPRING_BOOT;
        }
        
        // Check for Flask indicators
        if (files.contains("app.py") || files.contains("main.py")) {
            try {
                if (hasFlaskIndicators(projectPath)) {
                    return ProjectType.FLASK;
                }
            } catch (IOException e) {
                // Continue with other checks
            }
        }
        
        // Check for Rust
        if (files.contains("Cargo.toml")) {
            return ProjectType.RUST;
        }
        
        // Check for Go
        if (files.contains("go.mod")) {
            return ProjectType.GO;
        }
        
        // Check for C#/.NET
        if (files.stream().anyMatch(f -> f.endsWith(".csproj")) || 
            files.stream().anyMatch(f -> f.endsWith(".sln")) ||
            files.contains("Directory.Build.props") ||
            files.contains("global.json")) {
            return ProjectType.CSHARP_DOTNET;
        }
        
        // Check for Maven project
        if (files.contains("pom.xml")) {
            return ProjectType.JAVA_MAVEN;
        }
        
        // Check for Gradle project
        if (files.contains("build.gradle") || files.contains("build.gradle.kts")) {
            return ProjectType.JAVA_GRADLE;
        }
        
        // Check for Node.js project (general)
        if (files.contains("package.json")) {
            return ProjectType.JAVASCRIPT_NODE;
        }
        
        // Check for Python project
        if (files.contains("requirements.txt") || files.contains("setup.py") || 
            files.contains("pyproject.toml") || files.contains("Pipfile")) {
            return ProjectType.PYTHON;
        }
        
        // Check for mixed project (multiple languages)
        boolean hasPython = files.stream().anyMatch(f -> f.endsWith(".py"));
        boolean hasJava = files.stream().anyMatch(f -> f.endsWith(".java"));
        boolean hasJavaScript = files.stream().anyMatch(f -> f.endsWith(".js") || f.endsWith(".ts"));
        boolean hasRust = files.stream().anyMatch(f -> f.endsWith(".rs"));
        boolean hasGo = files.stream().anyMatch(f -> f.endsWith(".go"));
        boolean hasCSharp = files.stream().anyMatch(f -> f.endsWith(".cs"));
        boolean hasDart = files.stream().anyMatch(f -> f.endsWith(".dart"));
        
        int languageCount = 0;
        if (hasPython) languageCount++;
        if (hasJava) languageCount++;
        if (hasJavaScript) languageCount++;
        if (hasRust) languageCount++;
        if (hasGo) languageCount++;
        if (hasCSharp) languageCount++;
        if (hasDart) languageCount++;
        
        if (languageCount > 1) {
            return ProjectType.MIXED;
        }
        
        // Default detection based on primary file types
        if (hasDart) return ProjectType.FLUTTER;
        if (hasPython) return ProjectType.PYTHON;
        if (hasJava) return ProjectType.JAVA_MAVEN;
        if (hasJavaScript) return ProjectType.JAVASCRIPT_NODE;
        if (hasRust) return ProjectType.RUST;
        if (hasGo) return ProjectType.GO;
        if (hasCSharp) return ProjectType.CSHARP_DOTNET;
        
        return ProjectType.UNKNOWN;
    }

    private boolean hasSpringBootIndicators(Path projectPath) throws IOException {
        Path pomPath = projectPath.resolve("pom.xml");
        if (Files.exists(pomPath)) {
            String pomContent = Files.readString(pomPath);
            return pomContent.contains("spring-boot") || pomContent.contains("org.springframework");
        }
        return false;
    }

    private boolean hasFlaskIndicators(Path projectPath) throws IOException {
        // Check for Flask imports in Python files
        try (Stream<Path> paths = Files.walk(projectPath)) {
            return paths
                .filter(path -> path.toString().endsWith(".py"))
                .anyMatch(path -> {
                    try {
                        String content = Files.readString(path);
                        return content.contains("from flask import") || 
                               content.contains("import flask") ||
                               content.contains("@app.route");
                    } catch (IOException e) {
                        return false;
                    }
                });
        }
    }

    private boolean hasReactIndicators(Path projectPath) throws IOException {
        Path packageJsonPath = projectPath.resolve("package.json");
        if (Files.exists(packageJsonPath)) {
            String content = Files.readString(packageJsonPath);
            return content.contains("\"react\"") && content.contains("\"react-dom\"") &&
                   !content.contains("\"react-native\"");
        }
        return false;
    }

    private boolean hasReactNativeIndicators(Path projectPath) throws IOException {
        Path packageJsonPath = projectPath.resolve("package.json");
        if (Files.exists(packageJsonPath)) {
            String content = Files.readString(packageJsonPath);
            return content.contains("\"react-native\"");
        }
        return false;
    }

    private boolean hasVueIndicators(Path projectPath) throws IOException {
        Path packageJsonPath = projectPath.resolve("package.json");
        if (Files.exists(packageJsonPath)) {
            String content = Files.readString(packageJsonPath);
            return content.contains("\"vue\"") || content.contains("\"@vue/");
        }
        return false;
    }

    /**
     * Extract Python dependencies from requirements.txt, setup.py, etc.
     */
    private void extractPythonDependencies(Path projectPath, ProjectAnalysis analysis) throws IOException {
        // Extract from requirements.txt
        Path requirementsPath = projectPath.resolve("requirements.txt");
        if (Files.exists(requirementsPath)) {
            extractFromRequirementsTxt(requirementsPath, analysis);
        }
        
        // Extract from setup.py
        Path setupPath = projectPath.resolve("setup.py");
        if (Files.exists(setupPath)) {
            extractFromSetupPy(setupPath, analysis);
        }
        
        // Extract from pyproject.toml
        Path pyprojectPath = projectPath.resolve("pyproject.toml");
        if (Files.exists(pyprojectPath)) {
            extractFromPyprojectToml(pyprojectPath, analysis);
        }
        
        // Scan Python files for import statements
        scanPythonImports(projectPath, analysis);
    }

    private void extractFromRequirementsTxt(Path requirementsPath, ProjectAnalysis analysis) throws IOException {
        List<String> lines = Files.readAllLines(requirementsPath);
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
                // Extract package name (before version specifiers)
                String packageName = line.split("[>=<!=]")[0].trim();
                if (!packageName.isEmpty()) {
                    analysis.addDependency(new Dependency(packageName, extractVersion(line), "python"));
                }
            }
        }
    }

    private void extractFromSetupPy(Path setupPath, ProjectAnalysis analysis) throws IOException {
        String content = Files.readString(setupPath);
        
        // Extract install_requires dependencies
        Pattern pattern = Pattern.compile("install_requires\\s*=\\s*\\[([^\\]]+)\\]", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        
        if (matcher.find()) {
            String dependencies = matcher.group(1);
            Pattern depPattern = Pattern.compile("'([^']+)'|\"([^\"]+)\"");
            Matcher depMatcher = depPattern.matcher(dependencies);
            
            while (depMatcher.find()) {
                String dep = depMatcher.group(1) != null ? depMatcher.group(1) : depMatcher.group(2);
                String packageName = dep.split("[>=<!=]")[0].trim();
                analysis.addDependency(new Dependency(packageName, extractVersion(dep), "python"));
            }
        }
    }

    private void extractFromPyprojectToml(Path pyprojectPath, ProjectAnalysis analysis) throws IOException {
        String content = Files.readString(pyprojectPath);
        
        // Simple TOML parsing for dependencies section
        Pattern pattern = Pattern.compile("dependencies\\s*=\\s*\\[([^\\]]+)\\]", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        
        if (matcher.find()) {
            String dependencies = matcher.group(1);
            Pattern depPattern = Pattern.compile("'([^']+)'|\"([^\"]+)\"");
            Matcher depMatcher = depPattern.matcher(dependencies);
            
            while (depMatcher.find()) {
                String dep = depMatcher.group(1) != null ? depMatcher.group(1) : depMatcher.group(2);
                String packageName = dep.split("[>=<!=]")[0].trim();
                analysis.addDependency(new Dependency(packageName, extractVersion(dep), "python"));
            }
        }
    }

    private void scanPythonImports(Path projectPath, ProjectAnalysis analysis) throws IOException {
        try (Stream<Path> paths = Files.walk(projectPath)) {
            paths
                .filter(path -> path.toString().endsWith(".py"))
                .forEach(path -> {
                    try {
                        extractImportsFromPythonFile(path, analysis);
                    } catch (IOException e) {
                        // Log error but continue
                        System.err.println("Error reading Python file: " + path + " - " + e.getMessage());
                    }
                });
        } catch (IOException e) {
            // Return what we have so far
        }
    }

    private void extractImportsFromPythonFile(Path filePath, ProjectAnalysis analysis) throws IOException {
        List<String> lines = Files.readAllLines(filePath);
        
        for (String line : lines) {
            line = line.trim();
            
            // Match "import package" or "from package import ..."
            if (line.startsWith("import ") || line.startsWith("from ")) {
                String packageName = extractPackageFromImport(line);
                if (packageName != null && !isStandardLibrary(packageName)) {
                    analysis.addDependency(new Dependency(packageName, "detected", "python"));
                }
            }
        }
    }

    private String extractPackageFromImport(String importLine) {
        if (importLine.startsWith("import ")) {
            String packageName = importLine.substring(7).split("\\s+")[0].split("\\.")[0];
            return packageName.trim();
        } else if (importLine.startsWith("from ")) {
            String[] parts = importLine.split("\\s+");
            if (parts.length >= 2) {
                return parts[1].split("\\.")[0];
            }
        }
        return null;
    }

    private boolean isStandardLibrary(String packageName) {
        // Common Python standard library modules to exclude
        Set<String> stdLibModules = Set.of(
            "os", "sys", "json", "re", "datetime", "time", "math", "random",
            "collections", "itertools", "functools", "operator", "copy",
            "pickle", "csv", "xml", "html", "urllib", "http", "email",
            "logging", "unittest", "threading", "multiprocessing", "subprocess",
            "io", "pathlib", "tempfile", "shutil", "glob", "fnmatch", "sqlite3"
        );
        return stdLibModules.contains(packageName);
    }

    /**
     * Extract Maven dependencies from pom.xml
     */
    private void extractMavenDependencies(Path projectPath, ProjectAnalysis analysis) throws IOException {
        Path pomPath = projectPath.resolve("pom.xml");
        if (Files.exists(pomPath)) {
            String content = Files.readString(pomPath);
            extractMavenDependenciesFromPom(content, analysis);
        }
    }

    private void extractMavenDependenciesFromPom(String pomContent, ProjectAnalysis analysis) {
        // Extract dependencies from <dependency> blocks
        Pattern pattern = Pattern.compile(
            "<dependency>\\s*" +
            "<groupId>([^<]+)</groupId>\\s*" +
            "<artifactId>([^<]+)</artifactId>\\s*" +
            "(?:<version>([^<]+)</version>)?[^<]*" +
            "</dependency>", 
            Pattern.DOTALL
        );
        
        Matcher matcher = pattern.matcher(pomContent);
        while (matcher.find()) {
            String groupId = matcher.group(1).trim();
            String artifactId = matcher.group(2).trim();
            String version = matcher.group(3) != null ? matcher.group(3).trim() : "unspecified";
            
            String fullName = groupId + ":" + artifactId;
            analysis.addDependency(new Dependency(fullName, version, "java"));
        }
    }

    /**
     * Extract Gradle dependencies from build.gradle
     */
    private void extractGradleDependencies(Path projectPath, ProjectAnalysis analysis) throws IOException {
        Path buildGradlePath = projectPath.resolve("build.gradle");
        if (Files.exists(buildGradlePath)) {
            String content = Files.readString(buildGradlePath);
            extractGradleDependenciesFromBuild(content, analysis);
        }
    }

    private void extractGradleDependenciesFromBuild(String buildContent, ProjectAnalysis analysis) {
        // Extract dependencies from implementation, compile, etc.
        Pattern pattern = Pattern.compile(
            "(implementation|compile|api|runtimeOnly|testImplementation)\\s*['\"]([^'\"]+)['\"]"
        );
        
        Matcher matcher = pattern.matcher(buildContent);
        while (matcher.find()) {
            String scope = matcher.group(1);
            String dependency = matcher.group(2);
            
            String[] parts = dependency.split(":");
            if (parts.length >= 2) {
                String name = parts[0] + ":" + parts[1];
                String version = parts.length > 2 ? parts[2] : "unspecified";
                analysis.addDependency(new Dependency(name, version, "java"));
            }
        }
    }

    /**
     * Extract Node.js dependencies from package.json
     */
    private void extractNodeDependencies(Path projectPath, ProjectAnalysis analysis) throws IOException {
        Path packageJsonPath = projectPath.resolve("package.json");
        if (Files.exists(packageJsonPath)) {
            String content = Files.readString(packageJsonPath);
            extractNodeDependenciesFromPackageJson(content, analysis);
            
            // Dynamically identify frameworks using Ollama based on dependencies and code patterns
            identifyFrameworksFromDependenciesAndCode(projectPath, analysis);
        }
    }

    private void extractNodeDependenciesFromPackageJson(String packageJsonContent, ProjectAnalysis analysis) {
        // Simple regex-based extraction of dependencies
        Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(packageJsonContent);
        
        boolean inDependencies = false;
        String[] lines = packageJsonContent.split("\n");
        
        for (String line : lines) {
            if (line.contains("\"dependencies\"") || line.contains("\"devDependencies\"")) {
                inDependencies = true;
                continue;
            }
            
            if (inDependencies && line.trim().equals("}")) {
                inDependencies = false;
                continue;
            }
            
            if (inDependencies) {
                Matcher lineMatcher = pattern.matcher(line);
                if (lineMatcher.find()) {
                    String name = lineMatcher.group(1);
                    String version = lineMatcher.group(2);
                    analysis.addDependency(new Dependency(name, version, "javascript"));
                }
            }
        }
    }

    /**
     * Extract Spring Boot specific dependencies and analyze configuration
     */
    private void extractSpringBootDependencies(Path projectPath, ProjectAnalysis analysis) throws IOException {
        // First extract Maven/Gradle dependencies
        extractMavenDependencies(projectPath, analysis);
        extractGradleDependencies(projectPath, analysis);
        
        // Dynamically identify frameworks using Ollama based on dependencies and code patterns
        identifyFrameworksFromDependenciesAndCode(projectPath, analysis);
        
        // Scan for additional patterns
        scanCodePatterns(projectPath, analysis);
    }

    /**
     * Extract Flask specific dependencies and analyze configuration
     */
    private void extractFlaskDependencies(Path projectPath, ProjectAnalysis analysis) throws IOException {
        // First extract Python dependencies
        extractPythonDependencies(projectPath, analysis);
        
        // Dynamically identify frameworks using Ollama based on dependencies and code patterns
        identifyFrameworksFromDependenciesAndCode(projectPath, analysis);
        
        // Scan for additional patterns
        scanCodePatterns(projectPath, analysis);
    }

    /**
     * Extract React dependencies from package.json
     */
    private void extractReactDependencies(Path projectPath, ProjectAnalysis analysis) throws IOException {
        extractNodeDependencies(projectPath, analysis); // Reuse Node.js extraction
        analysis.addFramework("React");
        generateFrameworkDocumentation("React", analysis);
    }

    /**
     * Extract React Native dependencies from package.json
     */
    private void extractReactNativeDependencies(Path projectPath, ProjectAnalysis analysis) throws IOException {
        extractNodeDependencies(projectPath, analysis); // Reuse Node.js extraction
        analysis.addFramework("React Native");
        generateFrameworkDocumentation("React Native", analysis);
    }

    /**
     * Extract Angular dependencies from package.json and angular.json
     */
    private void extractAngularDependencies(Path projectPath, ProjectAnalysis analysis) throws IOException {
        extractNodeDependencies(projectPath, analysis); // Reuse Node.js extraction
        analysis.addFramework("Angular");
        generateFrameworkDocumentation("Angular", analysis);
        
        // Extract Angular-specific configuration
        Path angularJsonPath = projectPath.resolve("angular.json");
        if (Files.exists(angularJsonPath)) {
            String content = Files.readString(angularJsonPath);
            // Could extract Angular-specific settings here
            analysis.addMetadata("angularConfig", "detected");
        }
    }

    /**
     * Extract Vue.js dependencies from package.json
     */
    private void extractVueDependencies(Path projectPath, ProjectAnalysis analysis) throws IOException {
        extractNodeDependencies(projectPath, analysis); // Reuse Node.js extraction
        analysis.addFramework("Vue.js");
        generateFrameworkDocumentation("Vue.js", analysis);
    }

    /**
     * Extract Flutter dependencies from pubspec.yaml
     */
    private void extractFlutterDependencies(Path projectPath, ProjectAnalysis analysis) throws IOException {
        Path pubspecPath = projectPath.resolve("pubspec.yaml");
        if (Files.exists(pubspecPath)) {
            extractFromPubspecYaml(pubspecPath, analysis);
        }
        analysis.addFramework("Flutter");
        generateFrameworkDocumentation("Flutter", analysis);
    }

    private void extractFromPubspecYaml(Path pubspecPath, ProjectAnalysis analysis) throws IOException {
        List<String> lines = Files.readAllLines(pubspecPath);
        boolean inDependencies = false;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            if (trimmed.equals("dependencies:") || trimmed.equals("dev_dependencies:")) {
                inDependencies = true;
                continue;
            }
            
            if (inDependencies && !line.startsWith(" ") && !line.startsWith("\t")) {
                inDependencies = false;
            }
            
            if (inDependencies && (line.startsWith("  ") || line.startsWith("\t"))) {
                String[] parts = trimmed.split(":");
                if (parts.length >= 1) {
                    String packageName = parts[0].trim();
                    if (!packageName.isEmpty() && !packageName.equals("flutter") && !packageName.equals("sdk")) {
                        String version = parts.length > 1 ? parts[1].trim() : "unspecified";
                        analysis.addDependency(new Dependency(packageName, version, "dart"));
                    }
                }
            }
        }
    }

    /**
     * Extract Rust dependencies from Cargo.toml
     */
    private void extractRustDependencies(Path projectPath, ProjectAnalysis analysis) throws IOException {
        Path cargoPath = projectPath.resolve("Cargo.toml");
        if (Files.exists(cargoPath)) {
            extractFromCargoToml(cargoPath, analysis);
        }
        analysis.addFramework("Rust");
        generateFrameworkDocumentation("Rust", analysis);
    }

    private void extractFromCargoToml(Path cargoPath, ProjectAnalysis analysis) throws IOException {
        List<String> lines = Files.readAllLines(cargoPath);
        boolean inDependencies = false;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            if (trimmed.equals("[dependencies]") || trimmed.equals("[dev-dependencies]")) {
                inDependencies = true;
                continue;
            }
            
            if (inDependencies && trimmed.startsWith("[")) {
                inDependencies = false;
            }
            
            if (inDependencies && trimmed.contains("=")) {
                String[] parts = trimmed.split("=", 2);
                if (parts.length == 2) {
                    String packageName = parts[0].trim();
                    String version = parts[1].trim().replaceAll("[\"\']", "");
                    analysis.addDependency(new Dependency(packageName, version, "rust"));
                }
            }
        }
    }

    /**
     * Extract Go dependencies from go.mod
     */
    private void extractGoDependencies(Path projectPath, ProjectAnalysis analysis) throws IOException {
        Path goModPath = projectPath.resolve("go.mod");
        if (Files.exists(goModPath)) {
            extractFromGoMod(goModPath, analysis);
        }
        analysis.addFramework("Go");
        generateFrameworkDocumentation("Go", analysis);
    }

    private void extractFromGoMod(Path goModPath, ProjectAnalysis analysis) throws IOException {
        List<String> lines = Files.readAllLines(goModPath);
        boolean inRequire = false;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            if (trimmed.equals("require (")) {
                inRequire = true;
                continue;
            }
            
            if (inRequire && trimmed.equals(")")) {
                inRequire = false;
                continue;
            }
            
            if (trimmed.startsWith("require ") || inRequire) {
                String depLine = trimmed.replace("require ", "").trim();
                String[] parts = depLine.split("\\s+");
                if (parts.length >= 2) {
                    String packageName = parts[0];
                    String version = parts[1];
                    if (!packageName.isEmpty() && !version.isEmpty()) {
                        analysis.addDependency(new Dependency(packageName, version, "go"));
                    }
                }
            }
        }
    }

    /**
     * Extract .NET dependencies from .csproj files
     */
    private void extractDotNetDependencies(Path projectPath, ProjectAnalysis analysis) throws IOException {
        try (Stream<Path> paths = Files.walk(projectPath)) {
            paths
                .filter(path -> path.toString().endsWith(".csproj"))
                .forEach(path -> {
                    try {
                        extractFromCsproj(path, analysis);
                    } catch (IOException e) {
                        System.err.println("Error reading .csproj file: " + path + " - " + e.getMessage());
                    }
                });
        }
        analysis.addFramework("C#/.NET");
        generateFrameworkDocumentation("C#/.NET", analysis);
    }

    private void extractFromCsproj(Path csprojPath, ProjectAnalysis analysis) throws IOException {
        String content = Files.readString(csprojPath);
        
        // Extract PackageReference elements
        Pattern pattern = Pattern.compile(
            "<PackageReference\\s+Include\\s*=\\s*['\"]([^'\"]+)['\"]\\s*Version\\s*=\\s*['\"]([^'\"]+)['\"]",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String packageName = matcher.group(1);
            String version = matcher.group(2);
            analysis.addDependency(new Dependency(packageName, version, "csharp"));
        }
        
        // Also check for simpler format
        Pattern simplePattern = Pattern.compile(
            "<PackageReference\\s+Include\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>\\s*<Version>([^<]+)</Version>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher simpleMatcher = simplePattern.matcher(content);
        while (simpleMatcher.find()) {
            String packageName = simpleMatcher.group(1);
            String version = simpleMatcher.group(2);
            analysis.addDependency(new Dependency(packageName, version, "csharp"));
        }
    }

    /**
     * Use AI to dynamically identify frameworks from dependencies and code patterns
     */
    private void identifyFrameworksFromDependenciesAndCode(Path projectPath, ProjectAnalysis analysis) {
        try {
            // Create a comprehensive prompt with dependencies and code samples
            String prompt = createFrameworkIdentificationPrompt(analysis, projectPath);
            String response = callAIService(prompt);
            
            if (response != null && !response.trim().isEmpty()) {
                parseFrameworkIdentificationResponse(response, analysis);
            } else {
                // Fallback to basic pattern detection if AI service fails
                fallbackFrameworkDetection(projectPath, analysis);
            }
        } catch (Exception e) {
            System.err.println("❌ Error identifying frameworks with AI providers: " + e.getMessage());
            // Fallback to basic pattern detection
            fallbackFrameworkDetection(projectPath, analysis);
        }
    }

    /**
     * Generate framework documentation using AI
     */
    private void generateFrameworkDocumentation(String framework, ProjectAnalysis analysis) {
        try {
            String prompt = createFrameworkDocumentationPrompt(framework);
            String documentation = callAIService(prompt);
            
            if (documentation != null && !documentation.trim().isEmpty()) {
                analysis.addFrameworkDocumentation(framework, documentation);
                System.out.println("📚 Generated " + framework + " documentation using AI");
            } else {
                // Fallback to basic documentation if AI service fails
                generateFallbackDocumentation(framework, analysis);
            }
        } catch (Exception e) {
            System.err.println("❌ Error generating " + framework + " documentation with AI: " + e.getMessage());
            // Fallback to basic documentation
            generateFallbackDocumentation(framework, analysis);
        }
    }

    /**
     * Create a comprehensive prompt for framework documentation generation
     */
    private String createFrameworkDocumentationPrompt(String framework) {
        return String.format("""
            You are a technical documentation expert. Generate comprehensive documentation for the %s framework that will be stored in a vector database for semantic search.

            Please provide:

            1. SYNTAX PATTERNS for creating applications/APIs in %s
            2. COMMON PATTERNS/CONVENTIONS used in %s
            3. PROJECT STRUCTURE and file organization
            4. CONFIGURATION patterns and setup
            5. DEPENDENCY MANAGEMENT patterns
            6. BUILD/COMPILATION patterns
            7. TESTING patterns
            8. DEPLOYMENT patterns
            9. PERFORMANCE best practices
            10. COMMON LIBRARIES/PACKAGES used with %s

            For web frameworks, include:
            - REST API endpoints and routing
            - Request/response handling
            - Authentication/authorization
            - Database integration

            For mobile frameworks, include:
            - UI components and layouts
            - State management
            - Navigation patterns
            - Platform-specific features

            Format the response as clear, searchable documentation with specific code examples. Include exact syntax that developers would search for.

            Make it comprehensive but concise. Focus on patterns that developers commonly search for when building applications.

            Framework: %s
            """, framework, framework, framework, framework, framework);
    }

    /**
     * Create a prompt to identify frameworks from dependencies and code patterns
     */
    private String createFrameworkIdentificationPrompt(ProjectAnalysis analysis, Path projectPath) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an expert software architect. Analyze the following project dependencies and code patterns to identify frameworks and technologies.\n\n");
        
        // Add dependencies to the prompt
        if (!analysis.getDependencies().isEmpty()) {
            prompt.append("DEPENDENCIES FOUND:\n");
            analysis.getDependencies().stream()
                .sorted((a, b) -> a.getName().compareTo(b.getName()))
                .forEach(dep -> prompt.append("- ").append(dep.getName())
                    .append(" (").append(dep.getLanguage()).append(")\n"));
            prompt.append("\n");
        }
        
        // Add code patterns to the prompt
        String codePatterns = extractCodePatternsForAnalysis(projectPath);
        if (!codePatterns.isEmpty()) {
            prompt.append("CODE PATTERNS FOUND:\n");
            prompt.append(codePatterns);
            prompt.append("\n");
        }
        
        prompt.append("""
            Please identify:
            1. WEB FRAMEWORKS (e.g., Flask, Django, Spring Boot, Express, React, Vue, Angular)
            2. MOBILE FRAMEWORKS (e.g., React Native, Flutter)
            3. DATABASE FRAMEWORKS (e.g., SQLAlchemy, JPA, Mongoose)
            4. TESTING FRAMEWORKS (e.g., pytest, JUnit, Jest)
            5. UI FRAMEWORKS (e.g., Bootstrap, Material-UI, Tailwind)
            6. BACKEND FRAMEWORKS (e.g., Rust web frameworks, Go frameworks)
            7. OTHER FRAMEWORKS/LIBRARIES that are commonly used
            
            For each framework identified, provide:
            - Framework name
            - Framework type (web, mobile, database, testing, ui, etc.)
            - Confidence level (high/medium/low)
            
            Format your response as:
            FRAMEWORK: [name] | TYPE: [type] | CONFIDENCE: [level]
            
            Only include frameworks you are confident about based on the dependencies and code patterns.
            """);
        
        return prompt.toString();
    }

    /**
     * Extract relevant code patterns for framework identification
     */
    private String extractCodePatternsForAnalysis(Path projectPath) {
        StringBuilder patterns = new StringBuilder();
        
        try (Stream<Path> paths = Files.walk(projectPath)) {
            paths
                .filter(path -> isRelevantCodeFile(path))
                .limit(10) // Limit to avoid overwhelming the prompt
                .forEach(path -> {
                    try {
                        String content = Files.readString(path);
                        String relevantPatterns = extractRelevantPatternsFromFile(content, path);
                        if (!relevantPatterns.isEmpty()) {
                            patterns.append("File: ").append(path.getFileName()).append("\n");
                            patterns.append(relevantPatterns).append("\n");
                        }
                    } catch (IOException e) {
                        // Continue with other files
                    }
                });
        } catch (IOException e) {
            // Return what we have so far
        }
        
        return patterns.toString();
    }

    private boolean isRelevantCodeFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".py") || fileName.endsWith(".java") || 
               fileName.endsWith(".js") || fileName.endsWith(".ts") ||
               fileName.endsWith(".jsx") || fileName.endsWith(".tsx") ||
               fileName.endsWith(".rs") || fileName.endsWith(".go") ||
               fileName.endsWith(".cs") || fileName.endsWith(".dart") ||
               fileName.equals("requirements.txt") || fileName.equals("pom.xml") ||
               fileName.equals("package.json") || fileName.equals("cargo.toml") ||
               fileName.equals("go.mod") || fileName.equals("pubspec.yaml");
    }

    private String extractRelevantPatternsFromFile(String content, Path path) {
        StringBuilder patterns = new StringBuilder();
        String fileName = path.getFileName().toString().toLowerCase();
        
        // Extract imports and annotations
        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            
            // Python imports and decorators
            if (fileName.endsWith(".py")) {
                if (line.startsWith("from ") || line.startsWith("import ") || line.startsWith("@")) {
                    patterns.append(line).append("\n");
                }
            }
            // Java annotations and imports
            else if (fileName.endsWith(".java")) {
                if (line.startsWith("import ") || line.startsWith("@")) {
                    patterns.append(line).append("\n");
                }
            }
            // JavaScript/TypeScript imports
            else if (fileName.endsWith(".js") || fileName.endsWith(".ts") || 
                     fileName.endsWith(".jsx") || fileName.endsWith(".tsx")) {
                if (line.startsWith("import ") || line.startsWith("const ") || 
                    line.startsWith("require(")) {
                    patterns.append(line).append("\n");
                }
            }
            // Rust use statements
            else if (fileName.endsWith(".rs")) {
                if (line.startsWith("use ") || line.startsWith("extern crate")) {
                    patterns.append(line).append("\n");
                }
            }
            // Go imports
            else if (fileName.endsWith(".go")) {
                if (line.startsWith("import ") || line.startsWith("package ")) {
                    patterns.append(line).append("\n");
                }
            }
            // C# using statements
            else if (fileName.endsWith(".cs")) {
                if (line.startsWith("using ") || line.startsWith("[")) {
                    patterns.append(line).append("\n");
                }
            }
            // Dart imports
            else if (fileName.endsWith(".dart")) {
                if (line.startsWith("import ") || line.startsWith("export ")) {
                    patterns.append(line).append("\n");
                }
            }
        }
        
        return patterns.toString();
    }

    /**
     * Parse AI's framework identification response
     */
    private void parseFrameworkIdentificationResponse(String response, ProjectAnalysis analysis) {
        String[] lines = response.split("\n");
        
        for (String line : lines) {
            if (line.startsWith("FRAMEWORK:")) {
                try {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 3) {
                        String frameworkName = parts[0].replace("FRAMEWORK:", "").trim();
                        String frameworkType = parts[1].replace("TYPE:", "").trim();
                        String confidence = parts[2].replace("CONFIDENCE:", "").trim();
                        
                        // Only add frameworks with medium or high confidence
                        if (confidence.equalsIgnoreCase("high") || confidence.equalsIgnoreCase("medium")) {
                            analysis.addFramework(frameworkName);
                            
                            // Generate documentation for this framework
                            generateFrameworkDocumentation(frameworkName, analysis);
                            
                            System.out.println("🔍 Identified framework: " + frameworkName + 
                                " (type: " + frameworkType + ", confidence: " + confidence + ")");
                        }
                    }
                } catch (Exception e) {
                    // Continue parsing other lines
                }
            }
        }
    }

    /**
     * Fallback framework detection when AI is unavailable
     */
    private void fallbackFrameworkDetection(Path projectPath, ProjectAnalysis analysis) {
        Set<Dependency> dependencies = analysis.getDependencies();
        
        // Simple keyword-based detection for common frameworks
        for (Dependency dep : dependencies) {
            String name = dep.getName().toLowerCase();
            
            // Web frameworks
            if (name.contains("flask")) {
                analysis.addFramework("Flask");
                generateFallbackDocumentation("Flask", analysis);
            } else if (name.contains("django")) {
                analysis.addFramework("Django");
                generateFallbackDocumentation("Django", analysis);
            } else if (name.contains("spring")) {
                analysis.addFramework("Spring Boot");
                generateFallbackDocumentation("Spring Boot", analysis);
            } else if (name.contains("express")) {
                analysis.addFramework("Express.js");
                generateFallbackDocumentation("Express.js", analysis);
            } else if (name.contains("react") && !name.contains("native")) {
                analysis.addFramework("React");
                generateFallbackDocumentation("React", analysis);
            } else if (name.contains("react-native")) {
                analysis.addFramework("React Native");
                generateFallbackDocumentation("React Native", analysis);
            } else if (name.contains("angular") || name.contains("@angular")) {
                analysis.addFramework("Angular");
                generateFallbackDocumentation("Angular", analysis);
            } else if (name.contains("vue")) {
                analysis.addFramework("Vue.js");
                generateFallbackDocumentation("Vue.js", analysis);
            }
            // Add more patterns as needed
        }
        
        System.out.println("🔄 Used fallback framework detection");
    }

    /**
     * Generate fallback documentation if AI is unavailable
     */
    private void generateFallbackDocumentation(String framework, ProjectAnalysis analysis) {
        String documentation;
        
        switch (framework.toLowerCase()) {
            case "flask":
                documentation = generateBasicFlaskDocumentation();
                break;
            case "spring boot":
            case "spring":
                documentation = generateBasicSpringBootDocumentation();
                break;
            case "express":
            case "express.js":
                documentation = generateBasicExpressDocumentation();
                break;
            case "react":
                documentation = generateBasicReactDocumentation();
                break;
            case "react native":
                documentation = generateBasicReactNativeDocumentation();
                break;
            case "angular":
                documentation = generateBasicAngularDocumentation();
                break;
            case "vue.js":
            case "vue":
                documentation = generateBasicVueDocumentation();
                break;
            case "flutter":
                documentation = generateBasicFlutterDocumentation();
                break;
            case "rust":
                documentation = generateBasicRustDocumentation();
                break;
            case "go":
                documentation = generateBasicGoDocumentation();
                break;
            case "c#/.net":
            case "csharp":
                documentation = generateBasicDotNetDocumentation();
                break;
            default:
                documentation = "Basic " + framework + " framework detected. Common patterns: application structure, dependency management, build configuration.";
                break;
        }
        
        analysis.addFrameworkDocumentation(framework, documentation);
        System.out.println("📚 Generated fallback documentation for " + framework);
    }

    /**
     * Generic method to scan for additional code patterns
     */
    private void scanCodePatterns(Path projectPath, ProjectAnalysis analysis) throws IOException {
        try (Stream<Path> paths = Files.walk(projectPath)) {
            paths
                .filter(path -> isRelevantCodeFile(path))
                .forEach(path -> {
                    try {
                        String content = Files.readString(path);
                        extractAdditionalPatternsFromFile(content, analysis);
                    } catch (IOException e) {
                        // Continue with other files
                    }
                });
        }
    }

    /**
     * Extract additional patterns that might indicate specific framework features
     */
    private void extractAdditionalPatternsFromFile(String content, ProjectAnalysis analysis) {
        // These are additional pattern indicators that can supplement AI's analysis
        if (content.contains("@app.route")) {
            analysis.addFramework("Flask Routing");
        }
        if (content.contains("render_template")) {
            analysis.addFramework("Flask Templates");
        }
        if (content.contains("request.get_json")) {
            analysis.addFramework("Flask JSON API");
        }
        if (content.contains("SQLAlchemy") || content.contains("db.Model")) {
            analysis.addFramework("Flask-SQLAlchemy");
        }
        if (content.contains("@RestController") || content.contains("@Controller")) {
            analysis.addFramework("Spring MVC");
        }
        if (content.contains("@Repository")) {
            analysis.addFramework("Spring Data");
        }
        if (content.contains("@Service")) {
            analysis.addFramework("Spring Core");
        }
        if (content.contains("@EnableJpaRepositories")) {
            analysis.addFramework("Spring Data JPA");
        }
        if (content.contains("@EnableWebSecurity")) {
            analysis.addFramework("Spring Security");
        }
        // React patterns
        if (content.contains("useState") || content.contains("useEffect")) {
            analysis.addFramework("React Hooks");
        }
        if (content.contains("React.Component") || content.contains("Component")) {
            analysis.addFramework("React Components");
        }
        // Angular patterns
        if (content.contains("@Component") || content.contains("@Injectable")) {
            analysis.addFramework("Angular Core");
        }
        // Flutter patterns
        if (content.contains("StatelessWidget") || content.contains("StatefulWidget")) {
            analysis.addFramework("Flutter Widgets");
        }
        // Rust patterns
        if (content.contains("actix_web") || content.contains("warp") || content.contains("rocket")) {
            analysis.addFramework("Rust Web Framework");
        }
        // Go patterns
        if (content.contains("http.HandleFunc") || content.contains("gin.Default")) {
            analysis.addFramework("Go Web Framework");
        }
    }

    private String extractVersion(String dependencyString) {
        Pattern pattern = Pattern.compile("[>=<!=]+([0-9.]+)");
        Matcher matcher = pattern.matcher(dependencyString);
        return matcher.find() ? matcher.group(1) : "unspecified";
    }

    /**
     * Enum for different project types
     */
    public enum ProjectType {
        PYTHON("Python"),
        JAVA_MAVEN("Java (Maven)"),
        JAVA_GRADLE("Java (Gradle)"),
        JAVASCRIPT_NODE("JavaScript (Node.js)"),
        SPRING_BOOT("Spring Boot"),
        FLASK("Flask"),
        REACT("React"),
        REACT_NATIVE("React Native"),
        ANGULAR("Angular"),
        VUE("Vue.js"),
        FLUTTER("Flutter"),
        RUST("Rust"),
        GO("Go"),
        CSHARP_DOTNET("C#/.NET"),
        MIXED("Mixed Languages"),
        UNKNOWN("Unknown");

        private final String displayName;

        ProjectType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Data class to hold project analysis results
     */
    public static class ProjectAnalysis {
        private final String projectPath;
        private ProjectType projectType;
        private final Set<Dependency> dependencies;
        private final Set<String> frameworks;
        private final Map<String, String> metadata;
        private final Map<String, String> frameworkDocumentation;

        public ProjectAnalysis(String projectPath) {
            this.projectPath = projectPath;
            this.dependencies = new HashSet<>();
            this.frameworks = new HashSet<>();
            this.metadata = new HashMap<>();
            this.frameworkDocumentation = new HashMap<>();
        }

        public void setProjectType(ProjectType projectType) {
            this.projectType = projectType;
        }

        public void addDependency(Dependency dependency) {
            this.dependencies.add(dependency);
        }

        public void addFramework(String framework) {
            this.frameworks.add(framework);
        }

        public void addFrameworkDocumentation(String framework, String documentation) {
            this.frameworkDocumentation.put(framework, documentation);
        }

        public void addMetadata(String key, String value) {
            this.metadata.put(key, value);
        }

        // Getters
        public String getProjectPath() { return projectPath; }
        public ProjectType getProjectType() { return projectType; }
        public Set<Dependency> getDependencies() { return dependencies; }
        public Set<String> getFrameworks() { return frameworks; }
        public Map<String, String> getMetadata() { return metadata; }
        public Map<String, String> getFrameworkDocumentation() { return frameworkDocumentation; }

        /**
         * Generate a searchable summary for this project analysis
         */
        public String getSearchableSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Project Analysis Summary\n");
            sb.append("Project Type: ").append(projectType.getDisplayName()).append("\n");
            sb.append("Project Path: ").append(projectPath).append("\n\n");
            
            if (!frameworks.isEmpty()) {
                sb.append("Frameworks: ").append(String.join(", ", frameworks)).append("\n\n");
            }
            
            if (!dependencies.isEmpty()) {
                sb.append("Dependencies (").append(dependencies.size()).append(" total):\n");
                dependencies.stream()
                    .sorted((a, b) -> a.getName().compareTo(b.getName()))
                    .forEach(dep -> sb.append("- ").append(dep.getName())
                        .append(" (").append(dep.getVersion()).append(", ")
                        .append(dep.getLanguage()).append(")\n"));
            }
            
            return sb.toString();
        }
    }

    /**
     * Data class representing a project dependency
     */
    public static class Dependency {
        private final String name;
        private final String version;
        private final String language;

        public Dependency(String name, String version, String language) {
            this.name = name;
            this.version = version;
            this.language = language;
        }

        public String getName() { return name; }
        public String getVersion() { return version; }
        public String getLanguage() { return language; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Dependency that = (Dependency) o;
            return Objects.equals(name, that.name) && Objects.equals(language, that.language);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, language);
        }

        @Override
        public String toString() {
            return name + " (" + version + ", " + language + ")";
        }
    }

    /**
     * Extract dependencies for mixed projects
     */
    private void extractMixedProjectDependencies(Path projectPath, ProjectAnalysis analysis) {
        // Try all dependency extraction methods for mixed projects
        try {
            extractPythonDependencies(projectPath, analysis);
            extractNodeDependencies(projectPath, analysis);
            extractMavenDependencies(projectPath, analysis);
            extractGradleDependencies(projectPath, analysis);
            extractRustDependencies(projectPath, analysis);
            extractGoDependencies(projectPath, analysis);
            extractDotNetDependencies(projectPath, analysis);
            extractFlutterDependencies(projectPath, analysis);
        } catch (IOException e) {
            System.err.println("Error extracting mixed project dependencies: " + e.getMessage());
        }
    }

    /**
     * Extract generic dependencies when project type is unknown
     */
    private void extractGenericDependencies(Path projectPath, ProjectAnalysis analysis) {
        // Use pattern matching to find common dependency files
        try (Stream<Path> files = Files.walk(projectPath, 3)) {
            files.filter(Files::isRegularFile)
                 .forEach(file -> {
                     String fileName = file.getFileName().toString().toLowerCase();
                     if (fileName.contains("requirement") || fileName.contains("package") || 
                         fileName.contains("dependency") || fileName.contains("lock")) {
                         analysis.getDependencies().add(new Dependency(
                             "Generic dependency file: " + fileName, 
                             "unknown", 
                             "unknown"
                         ));
                     }
                 });
        } catch (IOException e) {
            System.err.println("Error scanning for generic dependencies: " + e.getMessage());
        }
    }

    /**
     * Generate basic Flask documentation
     */
    private String generateBasicFlaskDocumentation() {
        return "# Flask Project\n\n" +
               "This project uses Flask, a lightweight WSGI web application framework in Python.\n\n" +
               "## Key Components:\n" +
               "- Flask application setup\n" +
               "- Route handlers\n" +
               "- Template rendering\n" +
               "- Database integration (if applicable)\n\n" +
               "## Getting Started:\n" +
               "1. Install dependencies: `pip install -r requirements.txt`\n" +
               "2. Set environment variables\n" +
               "3. Run the application: `flask run` or `python app.py`\n";
    }

    /**
     * Generate basic Spring Boot documentation
     */
    private String generateBasicSpringBootDocumentation() {
        return "# Spring Boot Project\n\n" +
               "This project uses Spring Boot, a Java-based framework for building enterprise applications.\n\n" +
               "## Key Components:\n" +
               "- Spring Boot application configuration\n" +
               "- REST controllers\n" +
               "- Service layer\n" +
               "- Data access layer (JPA/JDBC)\n" +
               "- Security configuration\n\n" +
               "## Getting Started:\n" +
               "1. Build the project: `mvn clean install`\n" +
               "2. Run the application: `mvn spring-boot:run`\n" +
               "3. Access the application at `http://localhost:8080`\n";
    }

    /**
     * Generate basic Express.js documentation
     */
    private String generateBasicExpressDocumentation() {
        return "# Express.js Project\n\n" +
               "This project uses Express.js, a fast, unopinionated web framework for Node.js.\n\n" +
               "## Key Components:\n" +
               "- Express server setup\n" +
               "- Route definitions\n" +
               "- Middleware configuration\n" +
               "- Database integration\n\n" +
               "## Getting Started:\n" +
               "1. Install dependencies: `npm install`\n" +
               "2. Start the server: `npm start` or `node app.js`\n" +
               "3. Access the application at `http://localhost:3000`\n";
    }

    /**
     * Generate basic React documentation
     */
    private String generateBasicReactDocumentation() {
        return "# React Project\n\n" +
               "This project uses React, a JavaScript library for building user interfaces.\n\n" +
               "## Key Components:\n" +
               "- React components\n" +
               "- State management\n" +
               "- Event handling\n" +
               "- Routing (if applicable)\n\n" +
               "## Getting Started:\n" +
               "1. Install dependencies: `npm install`\n" +
               "2. Start development server: `npm start`\n" +
               "3. Build for production: `npm run build`\n";
    }

    /**
     * Generate basic React Native documentation
     */
    private String generateBasicReactNativeDocumentation() {
        return "# React Native Project\n\n" +
               "This project uses React Native for building cross-platform mobile applications.\n\n" +
               "## Key Components:\n" +
               "- React Native components\n" +
               "- Navigation\n" +
               "- Platform-specific code\n" +
               "- Native modules\n\n" +
               "## Getting Started:\n" +
               "1. Install dependencies: `npm install`\n" +
               "2. Run on iOS: `npx react-native run-ios`\n" +
               "3. Run on Android: `npx react-native run-android`\n";
    }

    /**
     * Generate basic Angular documentation
     */
    private String generateBasicAngularDocumentation() {
        return "# Angular Project\n\n" +
               "This project uses Angular, a platform for building mobile and desktop web applications.\n\n" +
               "## Key Components:\n" +
               "- Angular components\n" +
               "- Services\n" +
               "- Routing\n" +
               "- Dependency injection\n\n" +
               "## Getting Started:\n" +
               "1. Install dependencies: `npm install`\n" +
               "2. Start development server: `ng serve`\n" +
               "3. Build for production: `ng build --prod`\n";
    }

    /**
     * Generate basic Vue.js documentation
     */
    private String generateBasicVueDocumentation() {
        return "# Vue.js Project\n\n" +
               "This project uses Vue.js, a progressive JavaScript framework for building user interfaces.\n\n" +
               "## Key Components:\n" +
               "- Vue components\n" +
               "- Vuex state management\n" +
               "- Vue Router\n" +
               "- Directives and filters\n\n" +
               "## Getting Started:\n" +
               "1. Install dependencies: `npm install`\n" +
               "2. Start development server: `npm run serve`\n" +
               "3. Build for production: `npm run build`\n";
    }

    /**
     * Generate basic Flutter documentation
     */
    private String generateBasicFlutterDocumentation() {
        return "# Flutter Project\n\n" +
               "This project uses Flutter for building cross-platform mobile applications.\n\n" +
               "## Key Components:\n" +
               "- Flutter widgets\n" +
               "- State management\n" +
               "- Navigation\n" +
               "- Platform integration\n\n" +
               "## Getting Started:\n" +
               "1. Get dependencies: `flutter pub get`\n" +
               "2. Run the app: `flutter run`\n" +
               "3. Build for release: `flutter build apk` or `flutter build ios`\n";
    }

    /**
     * Generate basic Rust documentation
     */
    private String generateBasicRustDocumentation() {
        return "# Rust Project\n\n" +
               "This project uses Rust, a systems programming language focused on safety and performance.\n\n" +
               "## Key Components:\n" +
               "- Cargo package management\n" +
               "- Modules and crates\n" +
               "- Memory safety\n" +
               "- Concurrency\n\n" +
               "## Getting Started:\n" +
               "1. Build the project: `cargo build`\n" +
               "2. Run the project: `cargo run`\n" +
               "3. Run tests: `cargo test`\n";
    }

    /**
     * Generate basic Go documentation
     */
    private String generateBasicGoDocumentation() {
        return "# Go Project\n\n" +
               "This project uses Go, an open source programming language for building simple, reliable, and efficient software.\n\n" +
               "## Key Components:\n" +
               "- Go modules\n" +
               "- Packages\n" +
               "- Goroutines and channels\n" +
               "- Standard library\n\n" +
               "## Getting Started:\n" +
               "1. Download dependencies: `go mod download`\n" +
               "2. Build the project: `go build`\n" +
               "3. Run the project: `go run main.go`\n" +
               "4. Run tests: `go test ./...`\n";
    }

    /**
     * Generate basic .NET documentation
     */
    private String generateBasicDotNetDocumentation() {
        return "# .NET Project\n\n" +
               "This project uses .NET, a developer platform for building many different types of applications.\n\n" +
               "## Key Components:\n" +
               "- .NET runtime and libraries\n" +
               "- C# language features\n" +
               "- NuGet package management\n" +
               "- ASP.NET Core (if web application)\n\n" +
               "## Getting Started:\n" +
               "1. Restore packages: `dotnet restore`\n" +
               "2. Build the project: `dotnet build`\n" +
               "3. Run the application: `dotnet run`\n" +
               "4. Run tests: `dotnet test`\n";
    }
}
