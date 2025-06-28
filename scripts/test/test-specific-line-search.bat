@echo off
echo === TESTING LINE NUMBER DISPLAY FOR SPECIFIC TEXT ===
echo.
echo Starting application with search for "self.validation_split = 0.1"...
echo Expected: Should find exact line with line number in text_to_sql_train.py
echo.
(
echo 4
echo self.validation_split = 0.1
echo n
echo 5
echo 0
) | java -jar target/indexer-0.0.1-SNAPSHOT.jar codebase/dssi-day3-ollama
