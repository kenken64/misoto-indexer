# Comprehensive End-to-End Test Script
Write-Host "=== Comprehensive End-to-End Test ===" -ForegroundColor Green
Write-Host "Testing dynamic cache file naming and collection handling" -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Green

# Create a simple test directory with a few files
$TEST_DIR = ".\test-codebase"
if (Test-Path $TEST_DIR) {
    Remove-Item $TEST_DIR -Recurse -Force
}
New-Item -ItemType Directory -Path $TEST_DIR -Force | Out-Null

@"
public class TestClass { 
    public static void main(String[] args) {
        System.out.println("Hello from test codebase!");
    }
}
"@ | Out-File -FilePath "$TEST_DIR\TestClass.java" -Encoding UTF8

@"
# Test README

This is a test codebase for validating dynamic indexing.

## Features
- Dynamic collection naming
- Project-specific cache files
- Multi-language support
"@ | Out-File -FilePath "$TEST_DIR\README.md" -Encoding UTF8

@"
// Test JavaScript file
console.log('Hello from test codebase!');

function testFunction() {
    return "This is a test function for indexing";
}

module.exports = { testFunction };
"@ | Out-File -FilePath "$TEST_DIR\test.js" -Encoding UTF8

@"
# Test Python file
def test_function():
    """This is a test function for indexing"""
    print("Hello from test codebase!")
    return "test"

if __name__ == "__main__":
    test_function()
"@ | Out-File -FilePath "$TEST_DIR\test.py" -Encoding UTF8

Write-Host ""
Write-Host "Created test directory: $TEST_DIR" -ForegroundColor Yellow
Write-Host "Test files:" -ForegroundColor Yellow
Get-ChildItem $TEST_DIR | Format-Table Name, Length, LastWriteTime

Write-Host ""
Write-Host "Expected behaviors:" -ForegroundColor Cyan
Write-Host "1. Collection name: codebase-index-test-codebase" -ForegroundColor Cyan
Write-Host "2. Cache file: .indexed_test-codebase_files_cache.txt" -ForegroundColor Cyan
Write-Host ""

$currentPath = (Get-Location).Path
$fullTestPath = Join-Path $currentPath $TEST_DIR

Write-Host "Test directory full path: $fullTestPath" -ForegroundColor Green
Write-Host ""
Write-Host "To test manually:" -ForegroundColor White
Write-Host "1. Run: mvn compile exec:java `"-Dexec.mainClass=sg.edu.nus.iss.codebase.indexer.IndexerApplication`"" -ForegroundColor White
Write-Host "2. Choose option 6 (Index Codebase)" -ForegroundColor White
Write-Host "3. Enter path: $fullTestPath" -ForegroundColor White
Write-Host "4. Verify that cache file .indexed_test-codebase_files_cache.txt is created" -ForegroundColor White
Write-Host ""

# Function to cleanup
function Cleanup {
    Write-Host "Cleaning up test files..." -ForegroundColor Yellow
    if (Test-Path $TEST_DIR) {
        Remove-Item $TEST_DIR -Recurse -Force
    }
    if (Test-Path ".indexed_test-codebase_files_cache.txt") {
        Remove-Item ".indexed_test-codebase_files_cache.txt" -Force
    }
    # Stop any running Java processes
    Get-Process -Name "java" -ErrorAction SilentlyContinue | Where-Object { $_.ProcessName -eq "java" } | Stop-Process -Force -ErrorAction SilentlyContinue
}

Write-Host "Press Enter to cleanup and exit, or Ctrl+C to keep files for manual testing..."
$null = Read-Host

Cleanup
Write-Host "Test completed and cleaned up." -ForegroundColor Green
