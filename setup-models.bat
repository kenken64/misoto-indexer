@echo off
echo Setting up Ollama models for Misoto Codebase Indexer...
echo.

echo Pulling nomic-embed-text model for embeddings...
ollama pull nomic-embed-text

echo.
echo Pulling codellama:7b model for chat...
ollama pull codellama:7b

echo.
echo Setup complete! You can now run the indexer.
echo.
echo To start the indexer with a specific directory:
echo java -jar target\indexer-0.0.1-SNAPSHOT.jar codebase/spring-ai
echo.
echo To start with default directory:
echo java -jar target\indexer-0.0.1-SNAPSHOT.jar
pause
