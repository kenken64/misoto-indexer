# Scripts Directory

This directory contains all the script files for the Misoto Indexer project, organized by functionality.

## Directory Structure

### `/setup/`
Scripts for initial setup and configuration:
- `setup.bat` / `setup.sh` - Main setup scripts for the project
- `setup-models.bat` / `setup-models.sh` - Scripts for setting up AI models

### `/test/`
Test scripts for various testing scenarios:
- `test-advanced-validation-search.bat` - Advanced validation search tests
- `test-case-insensitive-search.bat` - Case insensitive search tests
- `test-codebase-collection.bat` - Codebase collection tests
- `test-collection-fix.bat` - Collection fix tests
- `test-detailed-validation-search.bat` - Detailed validation search tests
- `test-end-to-end.ps1` / `test-end-to-end.sh` - End-to-end testing scripts
- `test-final-line-numbers.bat` - Final line numbers tests
- `test-models.bat` - Model testing scripts
- `test-search-collection.bat` - Search collection tests
- `test-specific-line-search.bat` - Specific line search tests
- `test-text-search-line-numbers.bat` - Text search line numbers tests
- `test-validation-split-search.bat` - Validation split search tests
- `test.js` - JavaScript test utilities

### `/utils/`
Utility scripts for maintenance and debugging:
- `debug-collection.bat` - Debug collection utilities
- `run-clean.bat` / `run-clean.sh` - Clean up scripts

## Usage

To run any script, navigate to the appropriate subdirectory and execute the script:

```bash
# For setup
cd scripts/setup
./setup.sh

# For tests
cd scripts/test
./test-end-to-end.sh

# For utilities
cd scripts/utils
./run-clean.sh
```

On Windows, use the `.bat` files instead of `.sh` files where available.
