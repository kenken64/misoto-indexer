@echo off
echo === TESTING SEARCH FOR self.validation_split ===
echo.
echo Starting application to search for validation_split...
echo Expected: Should find the current value 0.1 in dssi-day3-ollama
echo.
(
echo 1
echo self.validation_split
echo 0
) | java -jar target/indexer-0.0.1-SNAPSHOT.jar codebase/dssi-day3-ollama
