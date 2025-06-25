@echo off
echo === TESTING COLLECTION NAME FIX ===
echo.
echo Starting application with dssi-day3-ollama directory...
echo Expected collection: codebase-index-dssi-day3-ollama
echo.
timeout /t 2 >nul
java -jar target/indexer-0.0.1-SNAPSHOT.jar codebase/dssi-day3-ollama
