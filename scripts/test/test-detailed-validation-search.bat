@echo off
echo === DETAILED SEARCH FOR self.validation_split WITH LINE NUMBERS ===
echo.
echo Using Text Search (option 4) to get line numbers and exact code...
echo Expected: Should show line 80 with "self.validation_split = 0.1"
echo.
(
echo 4
echo self.validation_split
echo n
echo 10
echo 0
) | java -jar target/indexer-0.0.1-SNAPSHOT.jar codebase/dssi-day3-ollama
