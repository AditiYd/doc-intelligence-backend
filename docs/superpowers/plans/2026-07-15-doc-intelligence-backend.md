# Document Intelligence Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Spring Boot 4 backend that accepts PDF/TXT uploads, calls Gemini 1.5 Flash to extract structured data, and exposes 4 REST endpoints for an Angular frontend to consume.

**Architecture:** Upload returns immediately with `status=PROCESSING`; a `@Async` background thread (on a separate bean to avoid Spring proxy self-call issues) handles text extraction via PDFBox and calls the Gemini API via `RestClient`; the result is persisted to H2 via Spring Data JPA and status updated to `DONE` or `FAILED`.

**Tech Stack:** Spring Boot 4.1.0, Java 25, Maven, H2 in-memory, Spring Data JPA, Apache PDFBox 3.0.1, Google Gemini 1.5 Flash via `RestClient`, Lombok, JUnit 5 + Mockito

## Global Constraints

- Base package: `project2.example.proj`
- Spring Boot: 4.1.0 (do not change parent version)
- Java: 25 (do not change java.version)
- All JPA annotations from `jakarta.persistence.*` (not `javax.persistence.*`)
- API key must always come from env var `GEMINI_API_KEY` — never hardcoded
- Supported file types: `.pdf` and `.txt` only — anything else returns 400
- Document status values: exactly `"PROCESSING"`, `"DONE"`, `"FAILED"` (plain strings, not enums)

---

## File Map

| File | Purpose |
|------|---------|
| `pom.xml` | Add JPA, H2, PDFBox dependencies |
| `src/main/resources/application.properties` | H2, file upload, Gemini config |
| `src/main/java/.../model/DocumentRecord.java` | JPA entity, main document |
| `src/main/java/.../model/LineItem.java` | JPA entity, child of DocumentRecord |
| `src/main/java/.../repository/DocumentRepository.java` | Spring Data JPA interface |
| `src/main/java/.../dto/GeminiExtractionResult.java` | Maps raw Gemini JSON response |
| `src/main/java/.../dto/DocumentResponse.java` | API response DTO (never expose raw entities) |
| `src/main/java/.../dto/StatsResponse.java` | Stats endpoint response DTO |
| `src/main/java/.../service/FileStorageService.java` | Save file to disk, extract text |
| `src/main/java/.../service/GeminiService.java` | Call Gemini API via RestClient |
| `src/main/java/.../config/AsyncConfig.java` | @EnableAsync + thread pool |
| `src/main/java/.../service/DocumentProcessingService.java` | @Async processing (separate bean) |
| `src/main/java/.../service/DocumentService.java` | Upload orchestration + query methods |
| `src/main/java/.../config/CorsConfig.java` | Allow localhost:4200 on /api/** |
| `src/main/java/.../controller/DocumentController.java` | 4 REST endpoints |
| `src/test/java/.../service/DocumentServiceTest.java` | Unit tests for DocumentService |
| `src/test/java/.../service/GeminiServiceTest.java` | Unit tests for GeminiService parsing |
| `src/test/java/.../controller/DocumentControllerTest.java` | @WebMvcTest for all 4 endpoints |

---

## Task 1: Add Dependencies and Configure application.properties

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/resources/application.properties`

**Interfaces:**
- Produces: working app startup with H2 console at `http://localhost:8080/h2-console`

- [ ] **Step 1: Add dependencies to pom.xml**

Open `pom.xml`. Inside `<dependencies>`, add after the existing Lombok entry:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>

<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.1</version>
</dependency>
```

- [ ] **Step 2: Replace application.properties**

Replace the entire contents of `src/main/resources/application.properties` with:

```properties
spring.application.name=proj

# H2 in-memory database
spring.datasource.url=jdbc:h2:mem:docdb
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# File upload limits
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
file.upload.dir=uploads/

# Gemini API key — set as env var GEMINI_API_KEY before running
gemini.api.key=${GEMINI_API_KEY}
```

- [ ] **Step 3: Verify the app starts**

Run:
```
./mvnw spring-boot:run -DGEMINI_API_KEY=placeholder
```
Expected: App starts on port 8080, no errors. H2 console available at `http://localhost:8080/h2-console`. Stop the app with Ctrl+C.

---

## Task 2: JPA Entities — DocumentRecord and LineItem

**Files:**
- Create: `src/main/java/project2/example/proj/model/DocumentRecord.java`
- Create: `src/main/java/project2/example/proj/model/LineItem.java`
- Test: `src/test/java/project2/example/proj/model/DocumentRecordTest.java`

**Interfaces:**
- Produces:
  - `DocumentRecord` — JPA entity with fields: `id (String UUID)`, `originalFileName`, `filePath`, `documentType`, `vendor`, `totalAmount (Double)`, `currency`, `documentDate (LocalDate)`, `summary`, `uploadedAt (LocalDateTime)`, `status`, `lineItems (List<LineItem>)`
  - `LineItem` — JPA entity with fields: `id (String UUID)`, `description`, `quantity (Integer)`, `unitPrice (Double)`, `totalPrice (Double)`, `document (DocumentRecord)`

- [ ] **Step 1: Write failing test**

Create `src/test/java/project2/example/proj/model/DocumentRecordTest.java`:

```java
package project2.example.proj.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DocumentRecordTest {

    @Test
    void prePersist_setsUuidId() {
        DocumentRecord doc = new DocumentRecord();
        doc.prePersist();
        assertThat(doc.getId()).isNotNull();
        assertThat(doc.getId()).hasSize(36); // UUID length
    }

    @Test
    void lineItems_defaultsToEmptyList() {
        DocumentRecord doc = new DocumentRecord();
        assertThat(doc.getLineItems()).isNotNull();
        assertThat(doc.getLineItems()).isEmpty();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```
./mvnw test -Dtest=DocumentRecordTest
```
Expected: FAIL — `DocumentRecord` class does not exist yet.

- [ ] **Step 3: Create DocumentRecord.java**

Create `src/main/java/project2/example/proj/model/DocumentRecord.java`:

```java
package project2.example.proj.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
public class DocumentRecord {

    @Id
    private String id;

    private String originalFileName;
    private String filePath;
    private String documentType;
    private String vendor;
    private Double totalAmount;
    private String currency;
    private LocalDate documentDate;

    @Column(length = 2000)
    private String summary;

    private LocalDateTime uploadedAt;
    private String status;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LineItem> lineItems = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
```

- [ ] **Step 4: Create LineItem.java**

Create `src/main/java/project2/example/proj/model/LineItem.java`:

```java
package project2.example.proj.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
public class LineItem {

    @Id
    private String id;

    private String description;
    private Integer quantity;
    private Double unitPrice;
    private Double totalPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private DocumentRecord document;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

```
./mvnw test -Dtest=DocumentRecordTest
```
Expected: PASS (2 tests).

---

## Task 3: Repository

**Files:**
- Create: `src/main/java/project2/example/proj/repository/DocumentRepository.java`
- Test: `src/test/java/project2/example/proj/repository/DocumentRepositoryTest.java`

**Interfaces:**
- Consumes: `DocumentRecord` from Task 2
- Produces: `DocumentRepository` extending `JpaRepository<DocumentRecord, String>` — methods: `save(DocumentRecord)`, `findById(String)`, `findAll()`

- [ ] **Step 1: Write failing test**

Create `src/test/java/project2/example/proj/repository/DocumentRepositoryTest.java`:

```java
package project2.example.proj.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import project2.example.proj.model.DocumentRecord;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DocumentRepositoryTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Test
    void save_andFindById_returnsDocument() {
        DocumentRecord doc = new DocumentRecord();
        doc.setOriginalFileName("test.txt");
        doc.setFilePath("uploads/test.txt");
        doc.setStatus("PROCESSING");
        doc.setUploadedAt(LocalDateTime.now());

        DocumentRecord saved = documentRepository.save(doc);
        assertThat(saved.getId()).isNotNull();

        Optional<DocumentRecord> found = documentRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getOriginalFileName()).isEqualTo("test.txt");
        assertThat(found.get().getStatus()).isEqualTo("PROCESSING");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```
./mvnw test -Dtest=DocumentRepositoryTest
```
Expected: FAIL — `DocumentRepository` class does not exist yet.

- [ ] **Step 3: Create DocumentRepository.java**

Create `src/main/java/project2/example/proj/repository/DocumentRepository.java`:

```java
package project2.example.proj.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project2.example.proj.model.DocumentRecord;

public interface DocumentRepository extends JpaRepository<DocumentRecord, String> {
}
```

- [ ] **Step 4: Run test to verify it passes**

```
./mvnw test -Dtest=DocumentRepositoryTest
```
Expected: PASS (1 test).

---

## Task 4: DTOs

**Files:**
- Create: `src/main/java/project2/example/proj/dto/GeminiExtractionResult.java`
- Create: `src/main/java/project2/example/proj/dto/DocumentResponse.java`
- Create: `src/main/java/project2/example/proj/dto/StatsResponse.java`

**Interfaces:**
- Produces:
  - `GeminiExtractionResult` — Jackson-deserializable POJO matching the Gemini JSON response
  - `DocumentResponse` — API response with nested `LineItemResponse`
  - `StatsResponse` — `totalDocuments (int)`, `byType (Map<String,Long>)`, `totalSpend (Double)`

No test needed for plain data classes — they are exercised by Task 6, 8, and 9 tests.

- [ ] **Step 1: Create GeminiExtractionResult.java**

Create `src/main/java/project2/example/proj/dto/GeminiExtractionResult.java`:

```java
package project2.example.proj.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class GeminiExtractionResult {

    private String documentType;
    private String vendor;
    private Double totalAmount;
    private String currency;
    private String documentDate;
    private String summary;
    private List<LineItemResult> lineItems;

    @Data
    @NoArgsConstructor
    public static class LineItemResult {
        private String description;
        private Integer quantity;
        private Double unitPrice;
        private Double totalPrice;
    }
}
```

- [ ] **Step 2: Create DocumentResponse.java**

Create `src/main/java/project2/example/proj/dto/DocumentResponse.java`:

```java
package project2.example.proj.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class DocumentResponse {

    private String id;
    private String originalFileName;
    private String documentType;
    private String vendor;
    private Double totalAmount;
    private String currency;
    private LocalDate documentDate;
    private String summary;
    private LocalDateTime uploadedAt;
    private String status;
    private List<LineItemResponse> lineItems;

    @Data
    @NoArgsConstructor
    public static class LineItemResponse {
        private String id;
        private String description;
        private Integer quantity;
        private Double unitPrice;
        private Double totalPrice;
    }
}
```

- [ ] **Step 3: Create StatsResponse.java**

Create `src/main/java/project2/example/proj/dto/StatsResponse.java`:

```java
package project2.example.proj.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class StatsResponse {
    private int totalDocuments;
    private Map<String, Long> byType;
    private Double totalSpend;
}
```

- [ ] **Step 4: Verify compilation**

```
./mvnw compile
```
Expected: BUILD SUCCESS, no errors.

---

## Task 5: FileStorageService

**Files:**
- Create: `src/main/java/project2/example/proj/service/FileStorageService.java`
- Test: `src/test/java/project2/example/proj/service/FileStorageServiceTest.java`

**Interfaces:**
- Produces:
  - `FileStorageService.save(MultipartFile file): String` — saves file to `uploads/` folder, returns absolute file path
  - `FileStorageService.extractText(String filePath): String` — reads PDF (PDFBox) or TXT (UTF-8); throws `IllegalArgumentException` for other types

- [ ] **Step 1: Write failing tests**

Create `src/test/java/project2/example/proj/service/FileStorageServiceTest.java`:

```java
package project2.example.proj.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService();
        ReflectionTestUtils.setField(fileStorageService, "uploadDir", tempDir.toString());
    }

    @Test
    void save_storesFileAndReturnsPath() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.txt", "text/plain", "hello world".getBytes()
        );
        String savedPath = fileStorageService.save(file);
        assertThat(savedPath).endsWith("test.txt");
        assertThat(Path.of(savedPath)).exists();
    }

    @Test
    void extractText_readsTxtFileContent() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "sample.txt", "text/plain", "invoice content here".getBytes()
        );
        String savedPath = fileStorageService.save(file);
        String text = fileStorageService.extractText(savedPath);
        assertThat(text).contains("invoice content here");
    }

    @Test
    void extractText_throwsForUnsupportedType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "doc.docx", "application/octet-stream", "data".getBytes()
        );
        String savedPath = fileStorageService.save(file);
        assertThatThrownBy(() -> fileStorageService.extractText(savedPath))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported file type");
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```
./mvnw test -Dtest=FileStorageServiceTest
```
Expected: FAIL — `FileStorageService` does not exist yet.

- [ ] **Step 3: Create FileStorageService.java**

Create `src/main/java/project2/example/proj/service/FileStorageService.java`:

```java
package project2.example.proj.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload.dir}")
    private String uploadDir;

    public String save(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        String uniqueName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path destination = uploadPath.resolve(uniqueName);
        Files.copy(file.getInputStream(), destination);
        return destination.toAbsolutePath().toString();
    }

    public String extractText(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        String name = path.getFileName().toString().toLowerCase();

        if (name.endsWith(".pdf")) {
            try (PDDocument doc = PDDocument.load(path.toFile())) {
                return new PDFTextStripper().getText(doc);
            }
        }
        if (name.endsWith(".txt")) {
            return Files.readString(path, StandardCharsets.UTF_8);
        }
        throw new IllegalArgumentException("Unsupported file type: " + name);
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```
./mvnw test -Dtest=FileStorageServiceTest
```
Expected: PASS (3 tests).

---

## Task 6: GeminiService

**Files:**
- Create: `src/main/java/project2/example/proj/service/GeminiService.java`
- Test: `src/test/java/project2/example/proj/service/GeminiServiceTest.java`

**Interfaces:**
- Consumes: `GeminiExtractionResult` from Task 4
- Produces: `GeminiService.extract(String text): GeminiExtractionResult`

- [ ] **Step 1: Write failing test**

Create `src/test/java/project2/example/proj/service/GeminiServiceTest.java`:

```java
package project2.example.proj.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import project2.example.proj.dto.GeminiExtractionResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private GeminiService geminiService;

    @BeforeEach
    void setUp() {
        // Uses the package-private test constructor — avoids ReflectionTestUtils on final fields
        geminiService = new GeminiService(restClient, new ObjectMapper(), "test-key");
    }

    @Test
    void extract_parsesGeminiResponseCorrectly() throws Exception {
        String geminiJson = """
            {
              "candidates": [{
                "content": {
                  "parts": [{
                    "text": "{\\"documentType\\":\\"INVOICE\\",\\"vendor\\":\\"Acme Corp\\",\\"totalAmount\\":150.0,\\"currency\\":\\"USD\\",\\"documentDate\\":\\"2024-01-15\\",\\"summary\\":\\"Invoice from Acme Corp.\\",\\"lineItems\\":[]}"
                  }]
                }
              }]
            }
            """;

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(geminiJson);

        GeminiExtractionResult result = geminiService.extract("some document text");

        assertThat(result.getDocumentType()).isEqualTo("INVOICE");
        assertThat(result.getVendor()).isEqualTo("Acme Corp");
        assertThat(result.getTotalAmount()).isEqualTo(150.0);
        assertThat(result.getCurrency()).isEqualTo("USD");
        assertThat(result.getDocumentDate()).isEqualTo("2024-01-15");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```
./mvnw test -Dtest=GeminiServiceTest
```
Expected: FAIL — `GeminiService` does not exist yet.

- [ ] **Step 3: Create GeminiService.java**

Create `src/main/java/project2/example/proj/service/GeminiService.java`:

```java
package project2.example.proj.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import project2.example.proj.dto.GeminiExtractionResult;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private static final String GEMINI_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    // Spring-managed constructor
    public GeminiService(ObjectMapper objectMapper, @Value("${gemini.api.key}") String apiKey) {
        this(RestClient.create(), objectMapper, apiKey);
    }

    // Package-private constructor for unit tests — allows injecting a mock RestClient
    GeminiService(RestClient restClient, ObjectMapper objectMapper, String apiKey) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    public GeminiExtractionResult extract(String text) throws Exception {
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(Map.of("text", buildPrompt(text))))
            )
        );

        String response = restClient.post()
            .uri(GEMINI_URL + "?key=" + apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBody)
            .retrieve()
            .body(String.class);

        JsonNode root = objectMapper.readTree(response);
        String content = root
            .path("candidates").get(0)
            .path("content").path("parts").get(0)
            .path("text").asText();

        return objectMapper.readValue(content, GeminiExtractionResult.class);
    }

    private String buildPrompt(String text) {
        return """
            Analyze this document and extract the following in strict JSON format:
            {
              "documentType": "INVOICE | RECEIPT | REPORT | OTHER",
              "vendor": "company/vendor name or null",
              "totalAmount": numeric value or null,
              "currency": "USD/INR/EUR or null",
              "documentDate": "YYYY-MM-DD or null",
              "summary": "2-3 sentence summary of the document",
              "lineItems": [
                { "description": "", "quantity": 0, "unitPrice": 0.0, "totalPrice": 0.0 }
              ]
            }
            Return ONLY the JSON object, no markdown, no explanation.
            Document content: \
            """ + text;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```
./mvnw test -Dtest=GeminiServiceTest
```
Expected: PASS (1 test).

---

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

## Task 9: CorsConfig + DocumentController

**Files:**
- Create: `src/main/java/project2/example/proj/config/CorsConfig.java`
- Create: `src/main/java/project2/example/proj/controller/DocumentController.java`
- Test: `src/test/java/project2/example/proj/controller/DocumentControllerTest.java`

**Interfaces:**
- Consumes: `DocumentService.upload(MultipartFile)`, `DocumentService.getAll()`, `DocumentService.getById(String)`, `DocumentService.getStats()` (all from Task 8)
- Produces: 4 REST endpoints on `/api/documents`

- [ ] **Step 1: Write failing tests**

Create `src/test/java/project2/example/proj/controller/DocumentControllerTest.java`:

```java
package project2.example.proj.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import project2.example.proj.dto.DocumentResponse;
import project2.example.proj.dto.StatsResponse;
import project2.example.proj.service.DocumentService;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    @Test
    void upload_validFile_returns200WithProcessingStatus() throws Exception {
        DocumentResponse response = new DocumentResponse();
        response.setId("doc-1");
        response.setOriginalFileName("invoice.pdf");
        response.setStatus("PROCESSING");

        when(documentService.upload(any())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile(
            "file", "invoice.pdf", "application/pdf", "pdf data".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PROCESSING"))
            .andExpect(jsonPath("$.originalFileName").value("invoice.pdf"));
    }

    @Test
    void upload_unsupportedType_returns400() throws Exception {
        when(documentService.upload(any()))
            .thenThrow(new IllegalArgumentException("Unsupported file type"));

        MockMultipartFile file = new MockMultipartFile(
            "file", "doc.docx", "application/octet-stream", "data".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload").file(file))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getAll_returnsListOfDocuments() throws Exception {
        DocumentResponse doc = new DocumentResponse();
        doc.setId("doc-1");
        doc.setStatus("DONE");

        when(documentService.getAll()).thenReturn(List.of(doc));

        mockMvc.perform(get("/api/documents"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("doc-1"))
            .andExpect(jsonPath("$[0].status").value("DONE"));
    }

    @Test
    void getById_existingId_returnsDocument() throws Exception {
        DocumentResponse doc = new DocumentResponse();
        doc.setId("doc-1");
        doc.setStatus("DONE");

        when(documentService.getById("doc-1")).thenReturn(doc);

        mockMvc.perform(get("/api/documents/doc-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("doc-1"));
    }

    @Test
    void getById_missingId_returns404() throws Exception {
        when(documentService.getById("missing"))
            .thenThrow(new RuntimeException("Document not found"));

        mockMvc.perform(get("/api/documents/missing"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getStats_returnsStatsResponse() throws Exception {
        StatsResponse stats = new StatsResponse();
        stats.setTotalDocuments(3);
        stats.setByType(Map.of("INVOICE", 2L, "RECEIPT", 1L));
        stats.setTotalSpend(175.0);

        when(documentService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/api/documents/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalDocuments").value(3))
            .andExpect(jsonPath("$.totalSpend").value(175.0));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```
./mvnw test -Dtest=DocumentControllerTest
```
Expected: FAIL — `DocumentController` does not exist yet.

- [ ] **Step 3: Create CorsConfig.java**

Create `src/main/java/project2/example/proj/config/CorsConfig.java`:

```java
package project2.example.proj.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins("http://localhost:4200")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*");
            }
        };
    }
}
```

- [ ] **Step 4: Create DocumentController.java**

Create `src/main/java/project2/example/proj/controller/DocumentController.java`:

```java
package project2.example.proj.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import project2.example.proj.dto.DocumentResponse;
import project2.example.proj.dto.StatsResponse;
import project2.example.proj.service.DocumentService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        try {
            DocumentResponse response = documentService.upload(file);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "File processing failed"));
        }
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getAll() {
        return ResponseEntity.ok(documentService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(documentService.getById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getStats() {
        return ResponseEntity.ok(documentService.getStats());
    }
}
```

- [ ] **Step 5: Run all tests**

```
./mvnw test
```
Expected: ALL tests PASS. Build SUCCESS.

---

## Task 10: End-to-End Smoke Test

**Goal:** Verify the full flow works — upload a TXT file, poll until DONE, check extracted data.

- [ ] **Step 1: Set the Gemini API key**

In your terminal (PowerShell):
```powershell
$env:GEMINI_API_KEY = "your_actual_key_from_aistudio.google.com"
```

- [ ] **Step 2: Start the app**

```
./mvnw spring-boot:run
```
Expected: App starts on port 8080.

- [ ] **Step 3: Create a test TXT file**

Create `test-invoice.txt` anywhere on your machine with content:
```
INVOICE
Vendor: Acme Corporation
Date: 2024-01-15
Invoice #: INV-001

Items:
- Web Design Services: 1 x $500.00 = $500.00
- Hosting Setup: 2 x $75.00 = $150.00

Total: $650.00 USD
```

- [ ] **Step 4: Upload the file**

Using PowerShell:
```powershell
$response = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/documents/upload" `
  -Form @{ file = Get-Item ".\test-invoice.txt" }
$response | ConvertTo-Json
```
Expected: JSON with `status: "PROCESSING"` and an `id` field. Copy the `id` value.

- [ ] **Step 5: Poll for completion**

Replace `<id>` with the actual id from Step 4:
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/documents/<id>" | ConvertTo-Json -Depth 5
```
Expected within 5–10 seconds: `status: "DONE"`, `documentType: "INVOICE"`, `vendor: "Acme Corporation"`, `totalAmount: 650.0`.

- [ ] **Step 6: Check stats**

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/documents/stats" | ConvertTo-Json
```
Expected: `totalDocuments: 1`, `byType: { "INVOICE": 1 }`, `totalSpend: 650.0`.

- [ ] **Step 7: Verify H2 console (optional)**

Open `http://localhost:8080/h2-console`.
- JDBC URL: `jdbc:h2:mem:docdb`
- User: `sa`, Password: (empty)
- Run: `SELECT * FROM DOCUMENT_RECORD;` — should show 1 row with status `DONE`.
