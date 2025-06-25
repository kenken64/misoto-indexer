@echo off
echo === TESTING CASE-INSENSITIVE TEXT SEARCH FOR validation_split ===
echo.
echo Starting application with case-insensitive text search...
echo Expected: Should find line numbers and exact lines containing validation_split
echo.
(
echo 4
echo validation_split
echo n
echo 10
echo 0
) | java -jar target/indexer-0.0.1-SNAPSHOT.jar codebase/dssi-day3-ollama
