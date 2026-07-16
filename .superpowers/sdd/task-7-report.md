# Task 7 Report: AsyncConfig + DocumentProcessingService

## Status: DONE

## Files Created

1. `src/main/java/project2/example/proj/config/AsyncConfig.java`
   - `@Configuration @EnableAsync`
   - Bean `docProcessingExecutor`: ThreadPoolTaskExecutor (core=2, max=5, queue=10, prefix="doc-processing-")

2. `src/main/java/project2/example/proj/service/DocumentProcessingService.java`
   - `@Service @RequiredArgsConstructor`
   - `processAsync(String documentId)` annotated `@Async("docProcessingExecutor") @Transactional`
   - On success: populates all fields from GeminiExtractionResult (documentType, vendor, totalAmount, currency, summary, documentDate, lineItems), sets status="DONE", saves
   - On any exception: sets status="FAILED", saves

3. `src/test/java/project2/example/proj/service/DocumentProcessingServiceTest.java`
   - `@ExtendWith(MockitoExtension.class)` — plain Mockito, no Spring context
   - `@InjectMocks DocumentProcessingService` — uses @RequiredArgsConstructor (largest constructor)
   - 2 tests: `processAsync_setsStatusDone_onSuccess`, `processAsync_setsStatusFailed_onGeminiException`

## TDD Flow

- Step 1: Test written first → compile failure (DocumentProcessingService not found) — CONFIRMED FAIL
- Step 2: AsyncConfig.java and DocumentProcessingService.java created
- Step 3: Tests re-run → `Tests run: 2, Failures: 0, Errors: 0` — CONFIRMED PASS

## Test Run Output

```
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.502 s
[INFO] BUILD SUCCESS
```

## Design Notes

- `DocumentProcessingService` is a separate bean from any `DocumentService` — avoids Spring proxy self-call issue for `@Async`
- `@Async` is on `processAsync()` itself (not on the class), paired with `@Transactional`
- Status values: exactly `"DONE"` on success, `"FAILED"` on any caught exception
- `lineItems` on `DocumentRecord` are replaced with freshly built list (each `LineItem` gets `.setDocument(doc)` for the bidirectional FK)
- Mockito self-attach warning is a JDK compatibility notice only — does not affect test correctness
