# Backend Design — AI-Powered Document Intelligence Platform

**Date:** 2026-07-15
**Scope:** Spring Boot backend only (no deployment, no frontend)

---

## Stack

| Concern | Choice | Reason |
|---------|--------|--------|
| Framework | Spring Boot 4.1.0 | Already in starter project |
| Java | 25 | Already in starter project |
| Build | Maven | Already in starter project |
| Database (dev) | H2 in-memory | Zero install; swap to PostgreSQL later with 3 config lines |
| ORM | Spring Data JPA + Hibernate | Abstracts DB layer |
| PDF parsing | Apache PDFBox 3.x | Extract text from uploaded PDFs |
| AI | Google Gemini 1.5 Flash via RestClient | Free tier; direct HTTP keeps integration transparent |
| Async | Spring `@Async` + `@EnableAsync` | Background processing without blocking upload response |

---

## Architecture

```
POST /api/documents/upload
        │
        ▼
DocumentController
        │ delegates to
        ▼
DocumentService.upload()
  ├── FileStorageService.save()               saves file to uploads/
  ├── creates DocumentRecord (PROCESSING)
  ├── saves to H2 via DocumentRepository
  ├── returns DocumentResponse immediately (status=PROCESSING)
  └── calls documentProcessingService.processAsync(id)
              │  [@Async on separate bean — avoids self-call proxy issue]
              ├── FileStorageService.extractText()    PDFBox (PDF) or plain read (TXT)
              ├── GeminiService.extract()             RestClient → Gemini API
              ├── updates DocumentRecord (DONE)
              └── on any exception → sets status=FAILED
```

---

## Package Structure

```
project2.example.proj/
├── controller/
│   └── DocumentController.java
├── service/
│   ├── DocumentService.java              ← upload(), getAll(), getById(), getStats()
│   ├── DocumentProcessingService.java    ← @Async processAsync() lives here (avoids self-call proxy issue)
│   ├── GeminiService.java
│   └── FileStorageService.java
├── model/
│   ├── DocumentRecord.java
│   └── LineItem.java
├── repository/
│   └── DocumentRepository.java
├── dto/
│   ├── DocumentResponse.java
│   ├── StatsResponse.java
│   └── GeminiExtractionResult.java
└── config/
    ├── AsyncConfig.java
    └── CorsConfig.java
```

Base package: `project2.example.proj` (kept as-is from starter)

> **Note on `@Async` proxy:** Spring's `@Async` is applied via a proxy. Calling an `@Async` method from within the same bean bypasses the proxy and runs synchronously. `DocumentProcessingService` is a separate bean so `DocumentService.upload()` gets the proxied version when it calls `documentProcessingService.processAsync()`.

---

## Data Model

### `DocumentRecord` (JPA entity, table: `document_record`)

| Field | Type | Notes |
|-------|------|-------|
| id | String | UUID, `@Id @GeneratedValue` |
| originalFileName | String | |
| filePath | String | relative path under `uploads/` |
| documentType | String | `INVOICE` \| `RECEIPT` \| `REPORT` \| `OTHER` |
| vendor | String | nullable |
| totalAmount | Double | nullable |
| currency | String | nullable |
| documentDate | LocalDate | nullable |
| summary | String | |
| uploadedAt | LocalDateTime | set once on creation |
| status | String | `PROCESSING` → `DONE` \| `FAILED` |
| lineItems | List\<LineItem\> | `@OneToMany`, cascade ALL, orphanRemoval=true |

### `LineItem` (JPA entity, table: `line_item`)

| Field | Type | Notes |
|-------|------|-------|
| id | String | UUID |
| description | String | |
| quantity | Integer | |
| unitPrice | Double | |
| totalPrice | Double | |
| document | DocumentRecord | `@ManyToOne`, FK back to parent |

**Design notes:**
- String UUIDs (not Long) — portable across H2 and PostgreSQL
- `status` is plain String, not `@Enumerated` — values are stable and few

---

## API Endpoints

| Method | Path | Response | Notes |
|--------|------|----------|-------|
| `POST` | `/api/documents/upload` | `DocumentResponse` (status=PROCESSING) | Accepts `multipart/form-data`, key=`file` |
| `GET` | `/api/documents` | `List<DocumentResponse>` | All records |
| `GET` | `/api/documents/{id}` | `DocumentResponse` | 404 if not found; used for status polling |
| `GET` | `/api/documents/stats` | `StatsResponse` | Counts by type + total spend |

### `DocumentResponse` fields
`id, originalFileName, documentType, vendor, totalAmount, currency, documentDate, summary, uploadedAt, status, lineItems`

### `StatsResponse` fields
`totalDocuments, byType (Map<String,Long>), totalSpend`

### Error responses
- Unsupported file type → `400 Bad Request`
- Document not found → `404 Not Found`
- Async failures (Gemini error, parse error) → no HTTP error; record status set to `FAILED`

---

## Gemini Integration

**Endpoint:** `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key={apiKey}`

**Method:** `RestClient.post()` with JSON body

**Prompt sent:**
```
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
Document content: [extracted text]
```

**Response parsing:**
- Gemini wraps the result: `candidates[0].content.parts[0].text`
- Jackson deserializes that text into `GeminiExtractionResult`
- If text is not valid JSON or deserialization fails → `GeminiParsingException` thrown → caught in `processAsync()` → status set to `FAILED`

---

## Async Configuration

`AsyncConfig.java` — `@EnableAsync` + `ThreadPoolTaskExecutor` bean named `"docProcessingExecutor"`:
- Core pool size: 2
- Max pool size: 5
- Queue capacity: 10

`DocumentProcessingService.processAsync(String id)` annotated with `@Async("docProcessingExecutor")`.

---

## Configuration (`application.properties`)

```properties
spring.application.name=proj

# H2
spring.datasource.url=jdbc:h2:mem:docdb
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# File upload
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
file.upload.dir=uploads/

# Gemini (always read from env var, never hardcode)
gemini.api.key=${GEMINI_API_KEY}
```

---

## CORS

`CorsConfig.java` — allows all methods + headers from `http://localhost:4200` on `/api/**`.

---

## Supported File Types

- `.pdf` — text extracted via Apache PDFBox
- `.txt` — read directly as UTF-8 string

Any other extension → `400 Bad Request` before processing begins.

---

## Dependencies to Add to `pom.xml`

```xml
<!-- Spring Data JPA -->
spring-boot-starter-data-jpa

<!-- H2 -->
com.h2database:h2 (runtime scope)

<!-- PDFBox -->
org.apache.pdfbox:pdfbox:3.0.1

<!-- Jackson (for JSON parsing of Gemini response) -->
<!-- Included transitively via spring-boot-starter-web, no explicit dep needed -->
```
