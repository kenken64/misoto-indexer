@echo off
echo Testing Ollama model availability for embedding pipeline...
echo.

echo Checking if Ollama is running...
curl -s http://localhost:11434/api/tags > nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Ollama is not running. Please start Ollama first.
    echo    Run: ollama serve
    pause
    exit /b 1
)
echo ✅ Ollama is running

echo.
echo Checking if nomic-embed-text model is available...
ollama list | findstr "nomic-embed-text" > nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ nomic-embed-text model not found
    echo 🔄 Pulling nomic-embed-text model...
    ollama pull nomic-embed-text
) else (
    echo ✅ nomic-embed-text model is available
)

echo.
echo Checking if codellama:7b model is available...
ollama list | findstr "codellama:7b" > nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ codellama:7b model not found
    echo 🔄 Pulling codellama:7b model...
    ollama pull codellama:7b
) else (
    echo ✅ codellama:7b model is available
)

echo.
echo 🎉 All models ready for the embedding pipeline:
echo    📄 Raw Text → 🤖 nomic-embed-text → 📊 Vector → ☁️ Qdrant
echo.
echo You can now run: java -jar target\indexer-0.0.1-SNAPSHOT.jar codebase/spring-ai
pause
