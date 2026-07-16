# Task 5: FileStorageService - Completion Report

## Status: DONE

## Summary
FileStorageService has been fully implemented with all required functionality. All 3 unit tests pass successfully.

## Completed Steps

### Step 1: Write Failing Tests ✓
Created `src/test/java/project2/example/proj/service/FileStorageServiceTest.java` with three test cases:
- `save_storesFileAndReturnsPath()` - Verifies file is saved and absolute path is returned
- `extractText_readsTxtFileContent()` - Verifies TXT file content extraction
- `extractText_throwsForUnsupportedType()` - Verifies IllegalArgumentException for unsupported file types

### Step 2: Run Tests to Verify Failure ✓
Tests failed as expected with: "cannot find symbol class FileStorageService"

### Step 3: Create FileStorageService Implementation ✓
Created `src/main/java/project2/example/proj/service/FileStorageService.java` with:
- **save(MultipartFile)**: Stores uploaded files to configured `uploads/` directory with UUID prefix, returns absolute file path
- **extractText(String)**: Extracts text from .pdf (using PDFBox Loader API) or .txt (UTF-8) files
- Throws `IllegalArgumentException` for unsupported file types

Key implementation details:
- Uses PDFBox 3.0.1's `Loader.loadPDF()` API (not the deprecated `PDDocument.load()`)
- Supports .pdf and .txt file types only
- Proper file path handling with UUID naming to prevent collisions
- Uploads directory is injected via `@Value("${file.upload.dir}")` annotation

### Step 4: Run Tests to Verify Success ✓
All 3 tests pass:
```
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

Test execution: `mvn test -Dtest=FileStorageServiceTest`

## Files Created/Modified

### Created:
1. `src/main/java/project2/example/proj/service/FileStorageService.java` (49 lines)
2. `src/test/java/project2/example/proj/service/FileStorageServiceTest.java` (74 lines)

## Implementation Notes

1. **PDFBox Version Compatibility**: The project uses PDFBox 3.0.1, which requires using `Loader.loadPDF(File)` instead of deprecated `PDDocument.load(File)` API from earlier versions.

2. **Test Isolation**: Tests use JUnit5's `@TempDir` for file system isolation and `ReflectionTestUtils.setField()` to inject the uploadDir without a Spring application context, as required.

3. **File Storage**: Implementation uses UUID prefixing to prevent file name collisions while preserving original file extensions for proper type detection.

4. **Error Handling**: Proper exception handling with clear error messages for unsupported file types.

## Verification

The implementation satisfies all requirements:
- ✓ File storage with absolute path return
- ✓ Text extraction from PDF and TXT files
- ✓ Unsupported type error handling (IllegalArgumentException)
- ✓ Plain unit test (no Spring context required)
- ✓ All tests passing (3/3)
