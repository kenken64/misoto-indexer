@echo off
echo Testing codebase collection naming...
echo.
echo This will start the indexer with the codebase directory
echo Collection name should be set to: codebase-index-ollama
echo.
pause
mvn spring-boot:run -Dspring-boot.run.arguments="codebase"
