@echo off
echo === FINAL TEST: LINE NUMBER DISPLAY ===
echo.
echo Testing search for validation_split to verify line numbers are shown
echo Expected: Should show "Line 80: self.validation_split = 0.1" in text_to_sql_train.py
echo.
(
echo 4
echo validation_split
echo n
echo 3
echo 0
) | java -jar target/indexer-0.0.1-SNAPSHOT.jar codebase/dssi-day3-ollama
