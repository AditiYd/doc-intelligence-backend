# Task 8 Report: DocumentService

## Status: DONE

## Files Created

- `src/main/java/project2/example/proj/service/DocumentService.java`
- `src/test/java/project2/example/proj/service/DocumentServiceTest.java`

## Test Results

All 4 tests pass: `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`

### Tests Written (TDD order)

1. `upload_validPdf_returnsProcessingStatus` — verifies .pdf extension accepted, status=PROCESSING returned, processAsync called with saved document ID
2. `upload_unsupportedExtension_throwsIllegalArgumentException` — verifies .docx throws IllegalArgumentException with "Unsupported file type"
3. `getById_notFound_throwsRuntimeException` — verifies RuntimeException("Document not found: ...") thrown for missing ID
4. `getStats_returnsCorrectCounts` — verifies totalDocuments=3, byType grouping (INVOICE=2, RECEIPT=1), totalSpend=175.0

## Implementation Details

### DocumentService.java

- `@Service @RequiredArgsConstructor` — injects DocumentRepository, FileStorageService, DocumentProcessingService
- `upload(MultipartFile)`: validates filename ends with .pdf or .txt (case-insensitive); throws `IllegalArgumentException("Unsupported file type. Only PDF and TXT are allowed.")` otherwise; saves file via FileStorageService; creates DocumentRecord with status=PROCESSING and uploadedAt=now; saves to repository; calls processAsync(saved.getId()); returns toResponse(saved)
- `getAll()`: streams all DocumentRecords through toResponse()
- `getById(String)`: findById or throw `RuntimeException("Document not found: " + id)`
- `getStats()`: computes totalDocuments (list size), byType (groupingBy documentType, null-filtered), totalSpend (sum of non-null totalAmounts)
- `toResponse(DocumentRecord)`: maps all fields including nested LineItem -> LineItemResponse

## Constraints Verified

- Status values: only "PROCESSING" set by this service (DONE/FAILED set by DocumentProcessingService)
- Unsupported file types: `IllegalArgumentException` thrown (controller can catch for 400)
- Document not found: `RuntimeException("Document not found: " + id)` thrown (controller can catch for 404)
- Base package: `project2.example.proj.service`
- No Spring context in tests: `@ExtendWith(MockitoExtension.class)` only
