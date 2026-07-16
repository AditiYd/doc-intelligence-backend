# Session 2 Conversation Log — AI Document Intelligence Platform

**Date:** 2026-07-15
**Project:** `C:\Users\ADYADAV\Downloads\proj\proj`
**Session goal:** Fix 2 failing tests → smoke test backend → design + plan Angular frontend

---

## 1. Picked Up From Session 1

Resumed from `docs/session-log.md`. Status at start:
- 18/20 tests passing
- 2 `@SpringBootTest` context load failures under investigation
- Task 10 (smoke test) blocked on API key

---

## 2. Fixed Test Failures (Systematic Debugging)

### Bug 1 — `file.upload.dir` missing in test properties

**Symptom:** `fileStorageService: Injection of autowired dependencies failed`

**Root cause:** `src/test/resources/application.properties` **replaces** (not merges with) `src/main/resources/application.properties` on the test classpath. It only contained `gemini.api.key=test-placeholder`, so `FileStorageService`'s `@Value("${file.upload.dir}")` had nothing to resolve.

**Fix:** Added `file.upload.dir=uploads/` to `src/test/resources/application.properties`.

---

### Bug 2 — `ObjectMapper` bean not available in `@SpringBootTest` context

**Symptom:** `No qualifying bean of type 'com.fasterxml.jackson.databind.ObjectMapper' available`

**Root cause:** `GeminiService`'s Spring constructor requested an `ObjectMapper` bean via DI, but Spring Boot 4's `JacksonAutoConfiguration` was not registering one in the full context when using `spring-boot-starter-webmvc`.

**Fix:** Removed `ObjectMapper` from the Spring constructor's parameter list. The constructor now calls `new ObjectMapper()` directly. The package-private test constructor (used by `GeminiServiceTest`) was unchanged.

```java
// Before
@Autowired
public GeminiService(ObjectMapper objectMapper, @Value("${gemini.api.key}") String apiKey) {
    this(RestClient.create(), objectMapper, apiKey);
}

// After
@Autowired
public GeminiService(@Value("${gemini.api.key}") String apiKey) {
    this(RestClient.create(), new ObjectMapper(), apiKey);
}
```

**Result:** 20/20 tests — BUILD SUCCESS.

---

## 3. End-to-End Smoke Test

### Setup

- API key obtained and set: `$env:GEMINI_API_KEY = "..."`
- Model URL updated from `gemini-1.5-flash` → `gemini-flash-latest` (actual model: `gemini-3.5-flash`)
- App started: `.\mvnw spring-boot:run`
- Upload via curl: `curl -X POST http://localhost:8080/api/documents/upload -F "file=@test-invoice.txt;type=text/plain"`

> **Note:** PowerShell 5.1's `Invoke-RestMethod -Form` does not exist — use `curl` for multipart uploads.

### Bugs found during smoke test

#### Bug 3 — Exception swallowed silently; status stuck on PROCESSING

**Root cause 1:** `DocumentProcessingService` catch block had no logging — exceptions were invisible.

**Fix:** Added `@Slf4j` + `log.error(...)` to the catch block.

**Root cause 2:** `@Transactional` + `@Async` interaction. When Gemini threw a 503, the transaction was marked rollback-only. The `documentRepository.save(doc)` in the catch block then also failed (can't write on a rolled-back transaction), causing a `SimpleAsyncUncaughtExceptionHandler` error. Status was never updated to `FAILED`.

The actual Hibernate exception was:
```
HibernateException: A collection with orphan deletion was no longer referenced
by the owning entity instance: DocumentRecord.lineItems
```

**Root cause 3 (the real one):** `doc.setLineItems(newList)` replaced Hibernate's `PersistentBag` (which tracks orphan removal) with a plain `new ArrayList<>()`. With `orphanRemoval = true` on the `@OneToMany`, Hibernate threw when it detected the tracked collection was dereferenced.

**Fix:**
```java
// Before
doc.setLineItems(lineItems);

// After
doc.getLineItems().clear();
doc.getLineItems().addAll(lineItems);
```

### Smoke test result — PASSED ✅

Uploaded `test-invoice.txt`, polled `/api/documents/{id}`, received:

```json
{
  "status": "DONE",
  "documentType": "INVOICE",
  "vendor": "Acme Corp Ltd",
  "totalAmount": 202.4,
  "currency": "USD",
  "documentDate": "2024-01-15",
  "summary": "This is an invoice from Acme Corp Ltd for annual web services...",
  "lineItems": [
    { "description": "Web Hosting (annual)", "quantity": 1, "unitPrice": 120.0, "totalPrice": 120.0 },
    { "description": "Domain Registration",  "quantity": 1, "unitPrice": 15.0,  "totalPrice": 15.0  },
    { "description": "SSL Certificate",       "quantity": 1, "unitPrice": 49.0,  "totalPrice": 49.0  }
  ]
}
```

Stats endpoint also verified: `{ "totalDocuments": 1, "byType": { "INVOICE": 1 }, "totalSpend": 202.4 }`

All 20 tests still pass after smoke test fixes.

---

## 4. Session Log + Resume Updated

- `docs/session-log.md` updated to reflect all fixes, smoke test result, corrected model name, and How to Run (curl command replacing broken PowerShell `-Form`).
- `Aditi_Yadav_Resume.tex` — Smart Document Analyzer bullet updated:
  - `MongoDB` → `PostgreSQL` (actual prod DB)
  - `LLM API (Gemini/OpenAI)` → `Google Gemini API`

---

## 5. Angular Frontend — Brainstorming

**Decisions made:**

| Decision | Choice | Reason |
|----------|--------|--------|
| Project location | Sibling folder `doc-intelligence-frontend/` | Keeps backend and frontend cleanly separated |
| UI library | Angular Material (indigo-pink) | Built-in dashboard components, minimal custom CSS |
| Chart library | ng2-charts v6 + Chart.js | Most popular Angular charting option, easy bar/doughnut |
| Component style | Standalone components (Angular 17+) | Default for modern Angular, less boilerplate |
| App structure | 4-route flat navigation | Maps 1:1 to backend endpoints, no unnecessary complexity |

**Routes:**

| Path | Component | Backend endpoint |
|------|-----------|-----------------|
| `/` | redirect → `/upload` | — |
| `/upload` | `UploadComponent` | `POST /api/documents/upload` |
| `/documents` | `DocumentListComponent` | `GET /api/documents` |
| `/documents/:id` | `DocumentDetailComponent` | `GET /api/documents/:id` (polls while PROCESSING) |
| `/stats` | `StatsComponent` | `GET /api/documents/stats` |

**Key design decisions:**
- Single `DocumentService` for all HTTP — one source of truth
- `DocumentDetailComponent` polls via `interval(3000).pipe(switchMap(...), takeWhile(...))`, stops when status ≠ PROCESSING
- All errors shown as `mat-snack-bar` toasts (4 s), loading states via `mat-spinner` / `mat-progress-bar`
- `byType` from backend is a COUNT map (not spend-per-type) → bar chart shows document count by type; total spend shown in summary tile
- No auth, no pagination, no frontend unit tests — YAGNI

**Spec saved:** `docs/superpowers/specs/2026-07-15-angular-frontend-design.md`

---

## 6. Angular Frontend — Implementation Plan

**7 tasks, each independently runnable:**

| Task | Deliverable |
|------|-------------|
| 1 | `ng new`, Angular Material, ng2-charts, `environment.ts`, `provideHttpClient` |
| 2 | `document.model.ts` interfaces + `DocumentService` (all 4 HTTP methods) |
| 3 | App shell — `mat-toolbar` navbar, `app.routes.ts` (lazy-loaded), stub components |
| 4 | `UploadComponent` — file validation, upload, progress bar, navigate on success |
| 5 | `DocumentListComponent` — mat-table, status chips, row click, refresh |
| 6 | `DocumentDetailComponent` — detail card, polling, line items table, back button |
| 7 | `StatsComponent` — 3 summary tiles, bar chart, doughnut chart |

**Plan saved:** `docs/superpowers/plans/2026-07-15-angular-frontend.md`

---

## 7. Current Project State

### Backend — complete ✅
- 20/20 tests — BUILD SUCCESS
- Smoke tested against real Gemini API
- Running at `localhost:8080`

### Frontend — planned, not yet started ⏳
- Implementation plan ready at `docs/superpowers/plans/2026-07-15-angular-frontend.md`
- Next step: execute the plan (subagent-driven or inline)

---

## Files Changed This Session

| File | Change |
|------|--------|
| `src/test/resources/application.properties` | Added `file.upload.dir=uploads/` |
| `src/main/java/.../service/GeminiService.java` | Removed `ObjectMapper` from Spring constructor; changed model URL to `gemini-flash-latest` |
| `src/main/java/.../service/DocumentProcessingService.java` | Added `@Slf4j` + `log.error`; changed `setLineItems` → `clear` + `addAll` |
| `docs/session-log.md` | Fully updated — all bugs, smoke test, How to Run |
| `docs/superpowers/specs/2026-07-15-angular-frontend-design.md` | Created (Angular frontend design spec) |
| `docs/superpowers/plans/2026-07-15-angular-frontend.md` | Created (Angular frontend implementation plan) |
| `C:\Users\ADYADAV\Documents\Resumes\Aditi_Yadav_Resume.tex` | Updated Smart Document Analyzer bullet (MongoDB → PostgreSQL, LLM API → Google Gemini API) |
