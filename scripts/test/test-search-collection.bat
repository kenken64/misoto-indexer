@echo off
echo Testing search with correct collection name...
echo.
(
echo 1
echo gradient accumulation steps
echo 0
) | java -jar target/indexer-0.0.1-SNAPSHOT.jar codebase/dssi-day3-ollama
