@echo off
echo 🚀 Setting up Misoto Codebase Indexer
echo =====================================

REM Check if Ollama is installed
where ollama >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Ollama is not installed.
    echo Please install Ollama from https://ollama.ai
    pause
    exit /b 1
)

echo ✅ Ollama is installed

REM Check if Ollama is running
curl -s http://localhost:11434/api/tags >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ⚠️  Ollama is not running. Please start Ollama manually.
    echo Run: ollama serve
    pause
    exit /b 1
)

echo ✅ Ollama is running

REM Pull CodeLlama model
echo 📥 Pulling CodeLlama:7b model...
ollama pull codellama:7b

if %ERRORLEVEL% EQU 0 (
    echo ✅ CodeLlama:7b model downloaded successfully
) else (
    echo ❌ Failed to download CodeLlama:7b model
    pause
    exit /b 1
)

REM Check if .env file exists
if exist ".env" (
    echo ✅ .env file found
) else (
    echo ⚠️  .env file not found. Creating from template...
    if exist ".env.example" (
        copy .env.example .env
        echo 📝 Please edit .env file with your Qdrant Cloud details:
        echo    QDRANT_HOST=your-cluster-url.qdrant.tech
        echo    QDRANT_API_KEY=your-qdrant-api-key
    ) else (
        echo ❌ .env.example template not found
    )
)

echo.
echo 🎉 Setup complete! You can now run:
echo    mvn spring-boot:run
echo.
echo Available models:
ollama list | findstr codellama

pause
