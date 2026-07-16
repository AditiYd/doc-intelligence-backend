# Session Log — AI Document Intelligence Platform (Backend)

**Sessions:** 2026-07-15 (Sessions 1 & 2)
**Status:** Backend complete — all 20 tests pass, end-to-end smoke test verified.
**Next:** Angular frontend.

---

## What We Built

A full-stack portfolio project where:
- Users upload PDFs or TXT files
- Spring Boot extracts text (PDFBox) and calls **Google Gemini** to extract structured data (vendor, amount, line items, document type, summary)
- Results are stored in H2 (in-memory) and surfaced via 4 REST endpoints
- An Angular dashboard (next session) will display results and charts

The full project brief is at: `C:\Users\ADYADAV\Documents\Resumes\doc-intelligence-project.md`

---

## Key Decisions Made (Brainstorming Phase)

| Decision | Choice | Reason |
|----------|--------|--------|
| Spring Boot version | 4.1.0 (kept from starter) | Already in the generated project |
| Java version | 25 (kept from starter) | Already configured |
| Gemini API integration | Direct HTTP via `RestClient` | Transparent, no heavy abstraction, better for portfolio |
| Package name | `project2.example.proj` (kept as-is) | No need to rename |
| Processing model | **Async with status polling** | Upload returns `PROCESSING` immediately; background thread calls Gemini and updates to `DONE`/`FAILED` |

---

## Architecture

```
POST /api/documents/upload
        │
        ▼
DocumentController
        │
        ▼
DocumentService.upload()
  ├── FileStorageService.save()           saves file to uploads/
  ├── creates DocumentRecord (PROCESSING)
  ├── saves to H2
  ├── returns DocumentResponse immediately
  └── DocumentProcessingService.processAsync() [@Async — separate bean]
              ├── FileStorageService.extractText()   PDFBox / plain read
              ├── GeminiService.extract()             RestClient → Gemini API
              ├── updates record → DONE
              └── on error → FAILED (logs exception via @Slf4j)
```

**Why `DocumentProcessingService` is a separate bean:** Spring's `@Async` works via proxy. Calling an `@Async` method from within the same bean bypasses the proxy and runs synchronously. Separating it into its own `@Service` ensures the proxy intercepts the call correctly.

---

## Spring Boot 4 / Framework 7 Discoveries

Spring Boot 4.1.0 made several breaking changes vs. a Spring Boot 3.x approach. Documented here as a reference.

| What was planned / assumed | What SB 4 actually has | Fix applied |
|----------------------------|------------------------|-------------|
| `@DataJpaTest` | **Removed** | Use `@SpringBootTest(properties="gemini.api.key=test") + @Transactional` |
| `@WebMvcTest` at `org.springframework.boot.test.autoconfigure.web.servlet` | Moved to `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` | Updated import |
| `@MockitoBean` at `org.springframework.test.context.bean.override.mockito` | **Unchanged** | No change needed |
| PDFBox: `PDDocument.load()` | **Deprecated** in PDFBox 3.x — use `Loader.loadPDF()` | Updated |
| Spring picks the single public constructor automatically | Spring Framework 7 requires `@Autowired` when multiple constructors exist | Added `@Autowired` to `GeminiService` Spring constructor |
| `src/test/resources/application.properties` merges with main | It **replaces** the main one on the test classpath — any `@Value` not defined in the test file fails to inject | Added `file.upload.dir=uploads/` to test properties |
| Spring Boot auto-configures `ObjectMapper` bean via `JacksonAutoConfiguration` | Bean not registered in full `@SpringBootTest` context when using `spring-boot-starter-webmvc` | `GeminiService` Spring constructor now calls `new ObjectMapper()` directly; removed DI dependency |
| `doc.setLineItems(newList)` works safely | With `orphanRemoval = true`, replacing the Hibernate `PersistentBag` with a new `ArrayList` throws `HibernateException` | Changed to `doc.getLineItems().clear(); doc.getLineItems().addAll(lineItems)` |
| Gemini model endpoint `gemini-1.5-flash` | Resolved model at runtime is `gemini-3.5-flash` via `gemini-flash-latest` alias | Updated `GEMINI_URL` in `GeminiService` to use `gemini-flash-latest` |

---

## Files Created

### Main source (`src/main/java/project2/example/proj/`)

| File | Purpose |
|------|---------|
| `model/DocumentRecord.java` | JPA entity — main document (UUID id, status, all extracted fields) |
| `model/LineItem.java` | JPA entity — child of DocumentRecord (`@ManyToOne`, `orphanRemoval=true`) |
| `repository/DocumentRepository.java` | Spring Data JPA interface |
| `dto/GeminiExtractionResult.java` | Maps raw Gemini JSON response |
| `dto/DocumentResponse.java` | API response DTO (with nested `LineItemResponse`) |
| `dto/StatsResponse.java` | Stats endpoint DTO |
| `service/FileStorageService.java` | Save file to `uploads/`, extract text via PDFBox or plain read |
| `service/GeminiService.java` | Call Gemini API via `RestClient`, parse JSON response |
| `config/AsyncConfig.java` | `@EnableAsync` + `docProcessingExecutor` thread pool (core=2, max=5) |
| `service/DocumentProcessingService.java` | `@Async` orchestrator — text → Gemini → DB update, logs failures via `@Slf4j` |
| `service/DocumentService.java` | Upload flow + getAll / getById / getStats |
| `config/CorsConfig.java` | Allow `localhost:4200` on `/api/**` |
| `controller/DocumentController.java` | 4 REST endpoints |

### Config & Resources

| File | Key settings |
|------|-------------|
| `pom.xml` | `spring-boot-starter-data-jpa`, `h2` (runtime), `pdfbox:3.0.1`, `spring-boot-starter-test`, `spring-boot-starter-webmvc-test`, `jackson-databind` |
| `src/main/resources/application.properties` | H2 in-memory (`docdb`), file upload 10 MB, Gemini key from `GEMINI_API_KEY` env var |
| `src/test/resources/application.properties` | `gemini.api.key=test-placeholder`, `file.upload.dir=uploads/` — both required; this file replaces (not supplements) the main one during tests |

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/documents/upload` | Upload PDF/TXT → returns immediately with `status=PROCESSING` |
| `GET` | `/api/documents` | List all documents |
| `GET` | `/api/documents/{id}` | Single document (poll this for status changes) |
| `GET` | `/api/documents/stats` | `{ totalDocuments, byType: Map<String,Long>, totalSpend }` |

---

## Test Results

| Test class | Tests | Status |
|-----------|-------|--------|
| `DocumentRecordTest` | UUID generation, empty lineItems list | ✅ 2/2 |
| `DocumentRepositoryTest` | Save and find by ID | ✅ 1/1 |
| `FileStorageServiceTest` | Save file, extract TXT, reject unsupported type | ✅ 3/3 |
| `GeminiServiceTest` | Parse Gemini nested JSON response | ✅ 1/1 |
| `DocumentProcessingServiceTest` | Status=DONE on success, status=FAILED on exception | ✅ 2/2 |
| `DocumentServiceTest` | Upload valid/invalid, getById 404, getStats counts | ✅ 4/4 |
| `DocumentControllerTest` | All 4 endpoints including 400/404 error paths | ✅ 6/6 |
| `ProjApplicationTests` | Context loads | ✅ 1/1 |

**Overall: 20/20 — BUILD SUCCESS**

---

## Build Process Used

**Approach:** Subagent-Driven Development (one fresh AI subagent per task, reviewed after each)

**Task completion:**
- ✅ Task 1 — Dependencies + application.properties
- ✅ Task 2 — JPA entities
- ✅ Task 3 — Repository
- ✅ Task 4 — DTOs
- ✅ Task 5 — FileStorageService
- ✅ Task 6 — GeminiService
- ✅ Task 7 — AsyncConfig + DocumentProcessingService
- ✅ Task 8 — DocumentService
- ✅ Task 9 — CorsConfig + DocumentController
- ✅ Task 9b — Fixed 2 `@SpringBootTest` context load failures (all 20 tests pass)
- ✅ Task 10 — End-to-end smoke test passed (Gemini extracted vendor, amount, 3 line items, summary)

---

## How to Run

**Prerequisite:** Get a free Gemini API key from [aistudio.google.com](https://aistudio.google.com)

```powershell
# In the project directory: C:\Users\ADYADAV\Downloads\proj\proj

# Set API key
$env:GEMINI_API_KEY = "your_key_here"

# Run
.\mvnw spring-boot:run

# Run all tests (no API key needed)
.\mvnw test
```

**Upload a document (use curl — PowerShell 5.1 does not support `-Form`):**
```bash
curl -X POST "http://localhost:8080/api/documents/upload" \
  -F "file=@./your-file.pdf;type=application/pdf"
```

**Check status (poll until DONE):**
```bash
curl "http://localhost:8080/api/documents/<id>"
```

**List all documents:**
```bash
curl "http://localhost:8080/api/documents"
```

**Stats:**
```bash
curl "http://localhost:8080/api/documents/stats"
```

**H2 console:** http://localhost:8080/h2-console
JDBC URL: `jdbc:h2:mem:docdb` | User: `sa` | Password: (empty)

---

## What's Next

1. **Angular frontend** — Upload component, document list, detail view, spend charts (ng2-charts) ← **next session**
2. **Deployment** — Docker + Railway (backend) + Supabase PostgreSQL (prod DB) + Vercel (frontend)

---

## Resume Bullet (once deployed)

> Built an AI-powered document intelligence platform using **Spring Boot** and **Google Gemini API** to extract, categorize, and summarize uploaded PDFs — surfacing vendor, amount, and line-item data through an **Angular** dashboard with spend analytics.
