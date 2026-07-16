## Task 8: DocumentService

**Files:**
- Create: `src/main/java/project2/example/proj/service/DocumentService.java`
- Test: `src/test/java/project2/example/proj/service/DocumentServiceTest.java`

**Interfaces:**
- Consumes: `DocumentRepository` (Task 3), `FileStorageService.save(MultipartFile): String` (Task 5), `DocumentProcessingService.processAsync(String): void` (Task 7), `DocumentResponse` and `StatsResponse` (Task 4)
- Produces:
  - `DocumentService.upload(MultipartFile): DocumentResponse` — validates extension, saves file, creates `DocumentRecord` with `status=PROCESSING`, triggers async processing, returns `DocumentResponse`
  - `DocumentService.getAll(): List<DocumentResponse>`
  - `DocumentService.getById(String): DocumentResponse` — throws `RuntimeException("Document not found")` if missing
  - `DocumentService.getStats(): StatsResponse`

- [ ] **Step 1: Write failing tests**

Create `src/test/java/project2/example/proj/service/DocumentServiceTest.java`:

```java
package project2.example.proj.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import project2.example.proj.dto.DocumentResponse;
import project2.example.proj.dto.StatsResponse;
import project2.example.proj.model.DocumentRecord;
import project2.example.proj.repository.DocumentRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private DocumentProcessingService documentProcessingService;

    @InjectMocks
    private DocumentService documentService;

    @Test
    void upload_validPdf_returnsProcessingStatus() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "invoice.pdf", "application/pdf", "pdf content".getBytes()
        );
        DocumentRecord saved = new DocumentRecord();
        saved.setId("doc-123");
        saved.setOriginalFileName("invoice.pdf");
        saved.setStatus("PROCESSING");
        saved.setUploadedAt(LocalDateTime.now());

        when(fileStorageService.save(file)).thenReturn("uploads/invoice.pdf");
        when(documentRepository.save(any())).thenReturn(saved);

        DocumentResponse response = documentService.upload(file);

        assertThat(response.getStatus()).isEqualTo("PROCESSING");
        assertThat(response.getOriginalFileName()).isEqualTo("invoice.pdf");
        verify(documentProcessingService).processAsync("doc-123");
    }

    @Test
    void upload_unsupportedExtension_throwsIllegalArgumentException() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "doc.docx", "application/octet-stream", "data".getBytes()
        );
        assertThatThrownBy(() -> documentService.upload(file))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported file type");
    }

    @Test
    void getById_notFound_throwsRuntimeException() {
        when(documentRepository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> documentService.getById("missing"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Document not found");
    }

    @Test
    void getStats_returnsCorrectCounts() {
        DocumentRecord inv1 = new DocumentRecord();
        inv1.setDocumentType("INVOICE");
        inv1.setTotalAmount(100.0);

        DocumentRecord inv2 = new DocumentRecord();
        inv2.setDocumentType("INVOICE");
        inv2.setTotalAmount(50.0);

        DocumentRecord receipt = new DocumentRecord();
        receipt.setDocumentType("RECEIPT");
        receipt.setTotalAmount(25.0);

        when(documentRepository.findAll()).thenReturn(List.of(inv1, inv2, receipt));

        StatsResponse stats = documentService.getStats();

        assertThat(stats.getTotalDocuments()).isEqualTo(3);
        assertThat(stats.getByType()).containsEntry("INVOICE", 2L);
        assertThat(stats.getByType()).containsEntry("RECEIPT", 1L);
        assertThat(stats.getTotalSpend()).isEqualTo(175.0);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```
./mvnw test -Dtest=DocumentServiceTest
```
Expected: FAIL — `DocumentService` does not exist yet.

- [ ] **Step 3: Create DocumentService.java**

Create `src/main/java/project2/example/proj/service/DocumentService.java`:

```java
package project2.example.proj.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import project2.example.proj.dto.DocumentResponse;
import project2.example.proj.dto.StatsResponse;
import project2.example.proj.model.DocumentRecord;
import project2.example.proj.model.LineItem;
import project2.example.proj.repository.DocumentRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final DocumentProcessingService documentProcessingService;

    private static final List<String> SUPPORTED = List.of(".pdf", ".txt");

    public DocumentResponse upload(MultipartFile file) throws IOException {
        String name = file.getOriginalFilename();
        if (name == null || SUPPORTED.stream().noneMatch(ext -> name.toLowerCase().endsWith(ext))) {
            throw new IllegalArgumentException("Unsupported file type. Only PDF and TXT are allowed.");
        }

        String filePath = fileStorageService.save(file);

        DocumentRecord doc = new DocumentRecord();
        doc.setOriginalFileName(name);
        doc.setFilePath(filePath);
        doc.setStatus("PROCESSING");
        doc.setUploadedAt(LocalDateTime.now());
        DocumentRecord saved = documentRepository.save(doc);

        documentProcessingService.processAsync(saved.getId());
        return toResponse(saved);
    }

    public List<DocumentResponse> getAll() {
        return documentRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public DocumentResponse getById(String id) {
        DocumentRecord doc = documentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Document not found: " + id));
        return toResponse(doc);
    }

    public StatsResponse getStats() {
        List<DocumentRecord> all = documentRepository.findAll();

        Map<String, Long> byType = all.stream()
            .filter(d -> d.getDocumentType() != null)
            .collect(Collectors.groupingBy(DocumentRecord::getDocumentType, Collectors.counting()));

        double totalSpend = all.stream()
            .filter(d -> d.getTotalAmount() != null)
            .mapToDouble(DocumentRecord::getTotalAmount)
            .sum();

        StatsResponse stats = new StatsResponse();
        stats.setTotalDocuments(all.size());
        stats.setByType(byType);
        stats.setTotalSpend(totalSpend);
        return stats;
    }

    private DocumentResponse toResponse(DocumentRecord doc) {
        DocumentResponse resp = new DocumentResponse();
        resp.setId(doc.getId());
        resp.setOriginalFileName(doc.getOriginalFileName());
        resp.setDocumentType(doc.getDocumentType());
        resp.setVendor(doc.getVendor());
        resp.setTotalAmount(doc.getTotalAmount());
        resp.setCurrency(doc.getCurrency());
        resp.setDocumentDate(doc.getDocumentDate());
        resp.setSummary(doc.getSummary());
        resp.setUploadedAt(doc.getUploadedAt());
        resp.setStatus(doc.getStatus());

        if (doc.getLineItems() != null) {
            resp.setLineItems(doc.getLineItems().stream().map(li -> {
                DocumentResponse.LineItemResponse liResp = new DocumentResponse.LineItemResponse();
                liResp.setId(li.getId());
                liResp.setDescription(li.getDescription());
                liResp.setQuantity(li.getQuantity());
                liResp.setUnitPrice(li.getUnitPrice());
                liResp.setTotalPrice(li.getTotalPrice());
                return liResp;
            }).collect(Collectors.toList()));
        }
        return resp;
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```
./mvnw test -Dtest=DocumentServiceTest
```
Expected: PASS (4 tests).

---

