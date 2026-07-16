# Design Spec — AI Document Intelligence Platform: Angular Frontend

**Date:** 2026-07-15
**Status:** Approved
**Companion backend spec:** `docs/superpowers/specs/2026-07-15-doc-intelligence-backend-design.md`

---

## Overview

A standalone Angular 19 frontend for the AI Document Intelligence Platform. Users upload PDF or TXT files, the backend processes them asynchronously via Google Gemini, and the frontend polls for results and surfaces extracted data and spend analytics through a Material dashboard.

---

## Project Setup

- **Location:** `C:\Users\ADYADAV\Downloads\proj\doc-intelligence-frontend\` (sibling to the Spring Boot backend)
- **Angular version:** latest stable, standalone components, generated via `ng new`
- **UI library:** Angular Material (`indigo-pink` pre-built theme)
- **Chart library:** ng2-charts v6+ (Chart.js wrapper)
- **Backend base URL:** `http://localhost:8080` (via `environments/environment.ts`)

---

## Folder Structure

```
src/
  app/
    core/
      services/
        document.service.ts
      models/
        document.model.ts
    features/
      upload/
        upload.component.ts
        upload.component.html
        upload.component.scss
      document-list/
        document-list.component.ts
        document-list.component.html
        document-list.component.scss
      document-detail/
        document-detail.component.ts
        document-detail.component.html
        document-detail.component.scss
      stats/
        stats.component.ts
        stats.component.html
        stats.component.scss
    app.component.ts
    app.component.html
    app.routes.ts
  environments/
    environment.ts
```

---

## Routes

| Path | Component | Backend call |
|------|-----------|-------------|
| `/` | redirect → `/upload` | — |
| `/upload` | `UploadComponent` | `POST /api/documents/upload` |
| `/documents` | `DocumentListComponent` | `GET /api/documents` |
| `/documents/:id` | `DocumentDetailComponent` | `GET /api/documents/:id` (polls while PROCESSING) |
| `/stats` | `StatsComponent` | `GET /api/documents/stats` |

---

## TypeScript Models

**`core/models/document.model.ts`**

```typescript
export interface LineItem {
  id: string;
  description: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

export interface DocumentResponse {
  id: string;
  originalFileName: string;
  documentType: string | null;
  vendor: string | null;
  totalAmount: number | null;
  currency: string | null;
  documentDate: string | null;
  summary: string | null;
  status: 'PROCESSING' | 'DONE' | 'FAILED';
  uploadedAt: string;
  lineItems: LineItem[];
}

export interface StatsResponse {
  totalDocuments: number;
  totalSpend: number;
  byType: Record<string, number>;
}
```

---

## Service

**`core/services/document.service.ts`** — single injectable service wrapping all backend calls via `HttpClient`:

| Method | HTTP | Path |
|--------|------|------|
| `upload(file: File)` | POST | `/api/documents/upload` |
| `getAll()` | GET | `/api/documents` |
| `getById(id: string)` | GET | `/api/documents/:id` |
| `getStats()` | GET | `/api/documents/stats` |

All methods return `Observable<T>`. Errors propagate to the caller.

---

## Components

### `AppComponent` — shell
- `mat-toolbar` navbar with app title and navigation links: Upload / Documents / Stats
- `<router-outlet>` below the toolbar
- No logic — purely structural

### `UploadComponent` (`/upload`)
- `mat-card` containing a file input (`accept=".pdf,.txt"`) and an Upload button
- Client-side validation: reject files that are not `.pdf` or `.txt` before calling the service
- On submit: calls `documentService.upload()`, shows `mat-progress-bar` while in flight
- On success: navigates to `/documents/:id` so the user lands on the detail/polling view
- On error: shows a `mat-snack-bar` with the error message

### `DocumentListComponent` (`/documents`)
- Calls `getAll()` on init; shows `mat-spinner` while loading
- `mat-table` with columns: File Name, Type, Vendor, Amount, Status, Uploaded At
- Status column rendered as a `mat-chip`:
  - `DONE` → green
  - `PROCESSING` → amber
  - `FAILED` → red
- Clicking any row navigates to `/documents/:id`
- Refresh button re-fetches the list
- On error: `mat-snack-bar` toast

### `DocumentDetailComponent` (`/documents/:id`)
- Reads `:id` from route params, calls `getById(id)` on init
- If `status === 'PROCESSING'`: starts polling via `interval(3000).pipe(switchMap(...))`, stops automatically when status changes to `DONE` or `FAILED`
- Displays all document fields in a `mat-card` (type, vendor, amount, currency, date, summary)
- Line items rendered in a nested `mat-table` (description, quantity, unit price, total price)
- Back button navigates to `/documents`
- Shows `mat-spinner` on initial load; polling updates the view in place
- On error: `mat-snack-bar` toast

### `StatsComponent` (`/stats`)
- Calls `getStats()` on init; shows `mat-spinner` while loading
- Three `mat-card` summary tiles: Total Documents, Total Spend, Unique Types
- Bar chart (ng2-charts): spend amount by document type (x-axis = type, y-axis = USD amount)
- Doughnut chart (ng2-charts): document count by type
- On error: `mat-snack-bar` toast

---

## Error Handling

- All HTTP errors caught per-component and displayed as `mat-snack-bar` toasts (duration: 4 seconds)
- Loading states shown via `mat-spinner` (initial load) or `mat-progress-bar` (upload in-flight)
- No global error interceptor — keep it simple for a portfolio project

---

## Environment

**`environments/environment.ts`:**
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080'
};
```

---

## Out of Scope

- Authentication / login
- File delete or re-process actions
- Pagination on the document list
- Frontend unit tests
- Production environment file (added at deployment time)

---

## How to Run

```bash
# In doc-intelligence-frontend/
npm install
ng serve
# App runs at http://localhost:4200
# Backend must be running at http://localhost:8080 with GEMINI_API_KEY set
```
