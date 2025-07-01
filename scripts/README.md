# Scripts Directory

This directory contains all the script files for the Misoto Indexer project, organized by functionality and language.

## Directory Structure

### `/setup/`
Scripts for initial setup and configuration:
- `setup.bat` / `setup.sh` - Main setup scripts for the project
- `setup-models.bat` / `setup-models.sh` - Scripts for setting up AI models

### `/test/`
Test scripts for various testing scenarios (organized by original functionality):
- `test-advanced-validation-search.bat` - Advanced validation search tests
- `test-case-insensitive-search.bat` - Case insensitive search tests
- `test-codebase-collection.bat` - Codebase collection tests
- `test-collection-fix.bat` - Collection fix tests
- `test-detailed-validation-search.bat` - Detailed validation search tests
- `test-end-to-end.ps1` / `test-end-to-end.sh` - End-to-end testing scripts
- `test-final-line-numbers.bat` - Final line numbers tests

### `/utils/`
Utility scripts for maintenance and debugging:
- `run-clean.bat` / `run-clean.sh` - Clean and reset scripts
- `debug-collection.bat` - Collection debugging utilities

### `/python/`
Python scripts for testing, debugging, and utilities:
- `clear-and-reindex.py` - Clear cache and reindex operations
- `debug-semantic-search-issue.py` - Debug semantic search issues
- `debug-vector-search.py` - Vector search debugging
- `fix-semantic-search.py` - Semantic search fixes
- `manual-guide.py` - Manual testing guidance
- `manual-test-helper.py` - Manual testing helper utilities
- `quick-manual-test.py` - Quick manual testing scripts
- `quick-status-check.py` - Quick status checking utilities
- `test-comprehensive-semantic-search.py` - Comprehensive semantic search tests
- `test-dynamic-framework-docs.py` - Dynamic framework documentation tests
- `test-enhanced-search.py` - Enhanced search functionality tests
- `test-enhanced-semantic-search.py` - Enhanced semantic search tests
- `test-manual-guidance.py` - Manual guidance testing
- `test-optimized-search.py` - Optimized search tests
- `test-project-analysis-debug.py` - Project analysis debugging
- `test-project-analysis.py` - Project analysis tests
- `test-project-aware-search.py` - Project-aware search tests
- `test-rest-search.py` - REST API search tests
- `test-search-fix.py` - Search functionality fixes
- `test-semantic-search-corrected.py` - Corrected semantic search tests
- `test-semantic-search-direct.py` - Direct semantic search tests
- `test-semantic-search-end-to-end.py` - End-to-end semantic search tests
- `validate-line-aware-search.py` - Line-aware search validation

### `/shell/`
Shell scripts for build, configuration, and utilities:
- `build.sh` - Build automation script
- `check_qdrant_content.sh` - Qdrant content verification
- `cleanup-root-java.sh` - Java file organization cleanup
- `java-config.sh` - Java configuration script
- `organize-scripts.sh` - Script organization utility

### `/batch/`
Windows batch scripts:
- `auto_reindex.bat` - Automatic reindexing
- `test-search-manually.bat` - Manual search testing
- `test_enhanced_search.bat` - Enhanced search testing

### `/cmd/`
Windows command scripts (currently empty, reserved for future use)
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
