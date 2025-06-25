@echo off
echo === ADVANCED SEARCH FOR self.validation_split WITH LINE NUMBERS ===
echo.
echo Using Advanced Search (option 5) to get detailed results...
echo Expected: Should show line 80 with exact code and line numbers
echo.
(
echo 5
echo self.validation_split
echo semantic
echo 0.5
echo 10
echo 0
) | java -jar target/indexer-0.0.1-SNAPSHOT.jar codebase/dssi-day3-ollama
