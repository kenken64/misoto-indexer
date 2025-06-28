@echo off
echo === TESTING TEXT SEARCH FOR self.validation_split ===
echo.
echo Starting application with text search...
echo Expected: Should find line numbers and exact lines containing validation_split
echo.
(
echo 4
echo self.validation_split
echo y
echo 10
echo 0
) | java -jar target/indexer-0.0.1-SNAPSHOT.jar codebase/dssi-day3-ollama
