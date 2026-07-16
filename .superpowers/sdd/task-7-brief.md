## Task 7: AsyncConfig + DocumentProcessingService

**Files:**
- Create: `src/main/java/project2/example/proj/config/AsyncConfig.java`
- Create: `src/main/java/project2/example/proj/service/DocumentProcessingService.java`
- Test: `src/test/java/project2/example/proj/service/DocumentProcessingServiceTest.java`

**Interfaces:**
- Consumes: `DocumentRepository` (Task 3), `FileStorageService.extractText(String): String` (Task 5), `GeminiService.extract(String): GeminiExtractionResult` (Task 6), `GeminiExtractionResult` (Task 4), `DocumentRecord` (Task 2)
- Produces: `DocumentProcessingService.processAsync(String documentId): void` — annotated `@Async("docProcessingExecutor")`; sets `DocumentRecord.status` to `"DONE"` on success or `"FAILED"` on any exception

- [ ] **Step 1: Write failing test**

Create `src/test/java/project2/example/proj/service/DocumentProcessingServiceTest.java`:

```java
package project2.example.proj.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import project2.example.proj.dto.GeminiExtractionResult;
import project2.example.proj.model.DocumentRecord;
import project2.example.proj.repository.DocumentRepository;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentProcessingServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private GeminiService geminiService;

    @InjectMocks
    private DocumentProcessingService documentProcessingService;

    @Test
    void processAsync_setsStatusDone_onSuccess() throws Exception {
        DocumentRecord doc = new DocumentRecord();
        doc.setFilePath("uploads/test.txt");
        doc.setStatus("PROCESSING");

        GeminiExtractionResult result = new GeminiExtractionResult();
        result.setDocumentType("INVOICE");
        result.setVendor("Acme");
        result.setTotalAmount(100.0);
        result.setCurrency("USD");
        result.setDocumentDate("2024-01-15");
        result.setSummary("Test summary.");
        result.setLineItems(List.of());

        when(documentRepository.findById("doc-1")).thenReturn(Optional.of(doc));
        when(fileStorageService.extractText("uploads/test.txt")).thenReturn("some text");
        when(geminiService.extract("some text")).thenReturn(result);
        when(documentRepository.save(any())).thenReturn(doc);

        documentProcessingService.processAsync("doc-1");

        verify(documentRepository).save(argThat(d -> "DONE".equals(d.getStatus())));
    }

    @Test
    void processAsync_setsStatusFailed_onGeminiException() throws Exception {
        DocumentRecord doc = new DocumentRecord();
        doc.setFilePath("uploads/test.txt");
        doc.setStatus("PROCESSING");

        when(documentRepository.findById("doc-1")).thenReturn(Optional.of(doc));
        when(fileStorageService.extractText("uploads/test.txt")).thenReturn("some text");
        when(geminiService.extract("some text")).thenThrow(new RuntimeException("Gemini unavailable"));
        when(documentRepository.save(any())).thenReturn(doc);

        documentProcessingService.processAsync("doc-1");

        verify(documentRepository).save(argThat(d -> "FAILED".equals(d.getStatus())));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```
./mvnw test -Dtest=DocumentProcessingServiceTest
```
Expected: FAIL — `DocumentProcessingService` does not exist yet.

- [ ] **Step 3: Create AsyncConfig.java**

Create `src/main/java/project2/example/proj/config/AsyncConfig.java`:

```java
package project2.example.proj.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "docProcessingExecutor")
    public Executor docProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("doc-processing-");
        executor.initialize();
        return executor;
    }
}
```

- [ ] **Step 4: Create DocumentProcessingService.java**

Create `src/main/java/project2/example/proj/service/DocumentProcessingService.java`:

```java
package project2.example.proj.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project2.example.proj.dto.GeminiExtractionResult;
import project2.example.proj.model.DocumentRecord;
import project2.example.proj.model.LineItem;
import project2.example.proj.repository.DocumentRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final GeminiService geminiService;

    @Async("docProcessingExecutor")
    @Transactional
    public void processAsync(String documentId) {
        DocumentRecord doc = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalStateException("Document not found: " + documentId));
        try {
            String text = fileStorageService.extractText(doc.getFilePath());
            GeminiExtractionResult result = geminiService.extract(text);

            doc.setDocumentType(result.getDocumentType());
            doc.setVendor(result.getVendor());
            doc.setTotalAmount(result.getTotalAmount());
            doc.setCurrency(result.getCurrency());
            doc.setSummary(result.getSummary());

            if (result.getDocumentDate() != null) {
                doc.setDocumentDate(LocalDate.parse(result.getDocumentDate()));
            }

            List<LineItem> lineItems = new ArrayList<>();
            if (result.getLineItems() != null) {
                for (GeminiExtractionResult.LineItemResult li : result.getLineItems()) {
                    LineItem lineItem = new LineItem();
                    lineItem.setDescription(li.getDescription());
                    lineItem.setQuantity(li.getQuantity());
                    lineItem.setUnitPrice(li.getUnitPrice());
                    lineItem.setTotalPrice(li.getTotalPrice());
                    lineItem.setDocument(doc);
                    lineItems.add(lineItem);
                }
            }
            doc.setLineItems(lineItems);
            doc.setStatus("DONE");
            documentRepository.save(doc);

        } catch (Exception e) {
            doc.setStatus("FAILED");
            documentRepository.save(doc);
        }
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

```
./mvnw test -Dtest=DocumentProcessingServiceTest
```
Expected: PASS (2 tests).

---

