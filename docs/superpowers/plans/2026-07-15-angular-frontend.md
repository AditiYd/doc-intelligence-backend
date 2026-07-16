# Angular Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a standalone Angular frontend for the AI Document Intelligence Platform with upload, document list, document detail (with polling), and spend analytics views.

**Architecture:** Four lazy-loaded standalone components behind Angular Router, all sharing a single `DocumentService` that wraps the Spring Boot backend over HTTP. Angular Material provides the UI shell; ng2-charts renders the analytics charts.

**Tech Stack:** Angular (latest stable), Angular Material (indigo-pink theme), ng2-charts v6 + Chart.js, TypeScript, SCSS.

## Global Constraints

- Project root: `C:\Users\ADYADAV\Downloads\proj\doc-intelligence-frontend\`
- All components: `standalone: true`
- Backend base URL: `http://localhost:8080` — always read from `environment.apiUrl`, never hardcoded
- UI library: Angular Material — use `mat-*` components throughout; no Bootstrap or Tailwind
- Chart library: ng2-charts v6 (`BaseChartDirective`) + Chart.js — only used in `StatsComponent`
- No frontend unit tests — manual browser verification after each task
- `ng serve` must compile with zero errors before each commit
- All SCSS lives in the component's own `.scss` file — no global styles beyond theme

---

## File Map

| File | Created in Task |
|------|----------------|
| `src/environments/environment.ts` | 1 |
| `src/app/core/models/document.model.ts` | 2 |
| `src/app/core/services/document.service.ts` | 2 |
| `src/app/app.config.ts` | 3 (modified) |
| `src/app/app.routes.ts` | 3 (modified) |
| `src/app/app.component.ts` | 3 (modified) |
| `src/app/app.component.html` | 3 (modified) |
| `src/app/app.component.scss` | 3 (modified) |
| `src/app/features/upload/upload.component.ts` | 4 |
| `src/app/features/upload/upload.component.html` | 4 |
| `src/app/features/upload/upload.component.scss` | 4 |
| `src/app/features/document-list/document-list.component.ts` | 5 |
| `src/app/features/document-list/document-list.component.html` | 5 |
| `src/app/features/document-list/document-list.component.scss` | 5 |
| `src/app/features/document-detail/document-detail.component.ts` | 6 |
| `src/app/features/document-detail/document-detail.component.html` | 6 |
| `src/app/features/document-detail/document-detail.component.scss` | 6 |
| `src/app/features/stats/stats.component.ts` | 7 |
| `src/app/features/stats/stats.component.html` | 7 |
| `src/app/features/stats/stats.component.scss` | 7 |

---

## Task 1: Scaffold project + install dependencies + environment

**Files:**
- Create: `C:\Users\ADYADAV\Downloads\proj\doc-intelligence-frontend\` (whole project via `ng new`)
- Create: `src/environments/environment.ts`
- Modify: `src/app/app.config.ts` (add `provideHttpClient`)

**Interfaces:**
- Produces: a compiling Angular project at `localhost:4200` with Angular Material + ng2-charts installed and `environment.apiUrl` available

- [ ] **Step 1: Check Angular CLI is installed**

```powershell
ng version
```

Expected: prints Angular CLI version. If not found, install it:
```powershell
npm install -g @angular/cli
```

- [ ] **Step 2: Scaffold the project**

```powershell
cd C:\Users\ADYADAV\Downloads\proj
ng new doc-intelligence-frontend --style=scss --ssr=false
```

When prompted:
- "Which stylesheet format?" → SCSS (already set by flag, may not prompt)
- "Do you want to enable Server-Side Rendering?" → No (already set by flag)

Wait for `npm install` to complete. Then:

```powershell
cd doc-intelligence-frontend
```

- [ ] **Step 3: Add Angular Material**

```powershell
ng add @angular/material
```

When prompted:
- "Choose a prebuilt theme name" → **indigo-pink**
- "Set up global Angular Material Typography?" → **Yes**
- "Include and enable animations?" → **Yes**

- [ ] **Step 4: Install ng2-charts and Chart.js**

```powershell
npm install ng2-charts chart.js
```

Expected: installs without errors. `package.json` will contain `"ng2-charts"` and `"chart.js"`.

- [ ] **Step 5: Create environments folder and file**

```powershell
New-Item -ItemType Directory -Path src\environments -Force
```

Create `src/environments/environment.ts` with this exact content:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080'
};
```

- [ ] **Step 6: Add `provideHttpClient` to app.config.ts**

Open `src/app/app.config.ts`. It will look similar to this after `ng add @angular/material`:

```typescript
import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideAnimationsAsync()
  ]
};
```

Replace it entirely with:

```typescript
import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(),
    provideAnimationsAsync()
  ]
};
```

- [ ] **Step 7: Verify the project compiles**

```powershell
ng serve
```

Expected: compiles successfully, browser opens at `http://localhost:4200` showing the default Angular welcome page. Stop the server with `Ctrl+C`.

---

## Task 2: TypeScript models + DocumentService

**Files:**
- Create: `src/app/core/models/document.model.ts`
- Create: `src/app/core/services/document.service.ts`

**Interfaces:**
- Consumes: `environment.apiUrl` from `src/environments/environment.ts`
- Produces:
  - `DocumentResponse`, `LineItem`, `StatsResponse` interfaces — used by all 4 feature components
  - `DocumentService` with methods: `upload(file: File)`, `getAll()`, `getById(id)`, `getStats()`

- [ ] **Step 1: Create folder structure**

```powershell
New-Item -ItemType Directory -Path src\app\core\models -Force
New-Item -ItemType Directory -Path src\app\core\services -Force
New-Item -ItemType Directory -Path src\app\features\upload -Force
New-Item -ItemType Directory -Path src\app\features\document-list -Force
New-Item -ItemType Directory -Path src\app\features\document-detail -Force
New-Item -ItemType Directory -Path src\app\features\stats -Force
```

- [ ] **Step 2: Create `document.model.ts`**

Create `src/app/core/models/document.model.ts`:

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

- [ ] **Step 3: Create `document.service.ts`**

Create `src/app/core/services/document.service.ts`:

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DocumentResponse, StatsResponse } from '../models/document.model';

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private base = environment.apiUrl;

  constructor(private http: HttpClient) {}

  upload(file: File): Observable<DocumentResponse> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<DocumentResponse>(`${this.base}/api/documents/upload`, form);
  }

  getAll(): Observable<DocumentResponse[]> {
    return this.http.get<DocumentResponse[]>(`${this.base}/api/documents`);
  }

  getById(id: string): Observable<DocumentResponse> {
    return this.http.get<DocumentResponse>(`${this.base}/api/documents/${id}`);
  }

  getStats(): Observable<StatsResponse> {
    return this.http.get<StatsResponse>(`${this.base}/api/documents/stats`);
  }
}
```

- [ ] **Step 4: Verify compilation**

```powershell
ng build --configuration=development
```

Expected: `Build at: ... - Hash: ... - Time: ...ms` with zero errors.

---

## Task 3: App shell — navbar + routing

**Files:**
- Modify: `src/app/app.routes.ts`
- Modify: `src/app/app.component.ts`
- Modify: `src/app/app.component.html`
- Modify: `src/app/app.component.scss`

**Interfaces:**
- Consumes: feature component paths (lazy-loaded — files don't need to exist yet for routing to compile)
- Produces: a `mat-toolbar` navbar with Upload / Documents / Stats links and a `<router-outlet>`

- [ ] **Step 1: Replace `app.routes.ts`**

Replace the full content of `src/app/app.routes.ts`:

```typescript
import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'upload', pathMatch: 'full' },
  {
    path: 'upload',
    loadComponent: () =>
      import('./features/upload/upload.component').then(m => m.UploadComponent)
  },
  {
    path: 'documents',
    loadComponent: () =>
      import('./features/document-list/document-list.component').then(m => m.DocumentListComponent)
  },
  {
    path: 'documents/:id',
    loadComponent: () =>
      import('./features/document-detail/document-detail.component').then(m => m.DocumentDetailComponent)
  },
  {
    path: 'stats',
    loadComponent: () =>
      import('./features/stats/stats.component').then(m => m.StatsComponent)
  }
];
```

- [ ] **Step 2: Replace `app.component.ts`**

Replace the full content of `src/app/app.component.ts`:

```typescript
import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatToolbarModule, MatButtonModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {}
```

- [ ] **Step 3: Replace `app.component.html`**

Replace the full content of `src/app/app.component.html`:

```html
<mat-toolbar color="primary">
  <span class="app-title">Doc Intelligence</span>
  <span class="spacer"></span>
  <a mat-button routerLink="/upload" routerLinkActive="active-link">Upload</a>
  <a mat-button routerLink="/documents" routerLinkActive="active-link">Documents</a>
  <a mat-button routerLink="/stats" routerLinkActive="active-link">Stats</a>
</mat-toolbar>

<div class="page-content">
  <router-outlet></router-outlet>
</div>
```

- [ ] **Step 4: Replace `app.component.scss`**

Replace the full content of `src/app/app.component.scss`:

```scss
.spacer {
  flex: 1 1 auto;
}

.app-title {
  font-size: 1.2rem;
  font-weight: 600;
}

.active-link {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 4px;
}

.page-content {
  max-width: 1100px;
  margin: 32px auto;
  padding: 0 16px;
}
```

- [ ] **Step 5: Create stub components so routing compiles**

The lazy-loaded routes reference files that don't exist yet. Create minimal stubs for each.

Create `src/app/features/upload/upload.component.ts`:
```typescript
import { Component } from '@angular/core';
@Component({ selector: 'app-upload', standalone: true, template: '<p>Upload stub</p>' })
export class UploadComponent {}
```

Create `src/app/features/document-list/document-list.component.ts`:
```typescript
import { Component } from '@angular/core';
@Component({ selector: 'app-document-list', standalone: true, template: '<p>List stub</p>' })
export class DocumentListComponent {}
```

Create `src/app/features/document-detail/document-detail.component.ts`:
```typescript
import { Component } from '@angular/core';
@Component({ selector: 'app-document-detail', standalone: true, template: '<p>Detail stub</p>' })
export class DocumentDetailComponent {}
```

Create `src/app/features/stats/stats.component.ts`:
```typescript
import { Component } from '@angular/core';
@Component({ selector: 'app-stats', standalone: true, template: '<p>Stats stub</p>' })
export class StatsComponent {}
```

- [ ] **Step 6: Verify in browser**

```powershell
ng serve
```

Open `http://localhost:4200`. Expected:
- `mat-toolbar` with "Doc Intelligence" title and three nav buttons
- Clicking Upload / Documents / Stats changes the URL and shows the stub text
- No console errors

Stop the server with `Ctrl+C`.

---

## Task 4: UploadComponent

**Files:**
- Modify: `src/app/features/upload/upload.component.ts` (replace stub)
- Create: `src/app/features/upload/upload.component.html`
- Create: `src/app/features/upload/upload.component.scss`

**Interfaces:**
- Consumes: `DocumentService.upload(file: File): Observable<DocumentResponse>`
- Produces: navigates to `/documents/:id` on success; shows snack-bar on error

- [ ] **Step 1: Replace `upload.component.ts`**

Replace the full content of `src/app/features/upload/upload.component.ts`:

```typescript
import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { DocumentService } from '../../core/services/document.service';

@Component({
  selector: 'app-upload',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, MatProgressBarModule, MatSnackBarModule, MatIconModule],
  templateUrl: './upload.component.html',
  styleUrl: './upload.component.scss'
})
export class UploadComponent {
  selectedFile: File | null = null;
  uploading = false;

  constructor(
    private documentService: DocumentService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const file = input.files[0];
    const name = file.name.toLowerCase();
    if (!name.endsWith('.pdf') && !name.endsWith('.txt')) {
      this.snackBar.open('Only PDF and TXT files are supported.', 'Dismiss', { duration: 4000 });
      input.value = '';
      return;
    }
    this.selectedFile = file;
  }

  upload(): void {
    if (!this.selectedFile || this.uploading) return;
    this.uploading = true;
    this.documentService.upload(this.selectedFile).subscribe({
      next: (doc) => {
        this.uploading = false;
        this.router.navigate(['/documents', doc.id]);
      },
      error: () => {
        this.uploading = false;
        this.snackBar.open('Upload failed. Please try again.', 'Dismiss', { duration: 4000 });
      }
    });
  }
}
```

- [ ] **Step 2: Create `upload.component.html`**

Create `src/app/features/upload/upload.component.html`:

```html
<div class="upload-wrapper">
  <mat-card class="upload-card">
    <mat-card-header>
      <mat-card-title>Upload Document</mat-card-title>
      <mat-card-subtitle>Supported formats: PDF, TXT (max 10 MB)</mat-card-subtitle>
    </mat-card-header>

    <mat-card-content>
      <div class="file-row">
        <input
          type="file"
          #fileInput
          accept=".pdf,.txt"
          (change)="onFileSelected($event)"
          hidden />
        <button mat-stroked-button (click)="fileInput.click()">
          <mat-icon>attach_file</mat-icon>
          Choose File
        </button>
        <span class="file-name">{{ selectedFile ? selectedFile.name : 'No file selected' }}</span>
      </div>
    </mat-card-content>

    <mat-card-actions align="end">
      <button
        mat-raised-button
        color="primary"
        [disabled]="!selectedFile || uploading"
        (click)="upload()">
        {{ uploading ? 'Uploading…' : 'Upload' }}
      </button>
    </mat-card-actions>

    @if (uploading) {
      <mat-progress-bar mode="indeterminate"></mat-progress-bar>
    }
  </mat-card>
</div>
```

- [ ] **Step 3: Create `upload.component.scss`**

Create `src/app/features/upload/upload.component.scss`:

```scss
.upload-wrapper {
  display: flex;
  justify-content: center;
  padding-top: 40px;
}

.upload-card {
  width: 100%;
  max-width: 520px;
}

.file-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 0;
}

.file-name {
  color: rgba(0, 0, 0, 0.6);
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 300px;
}
```

- [ ] **Step 4: Verify in browser**

```powershell
ng serve
```

Navigate to `http://localhost:4200/upload`. Verify:
- Card renders with "Choose File" button and "Upload" button
- Choosing a `.pdf` or `.txt` file shows the filename
- Choosing a `.zip` or other file shows a snack-bar error
- Upload button is disabled until a valid file is selected
- (No backend needed yet — upload will fail with a network error, which is fine)

Stop the server.

---

## Task 5: DocumentListComponent

**Files:**
- Modify: `src/app/features/document-list/document-list.component.ts` (replace stub)
- Create: `src/app/features/document-list/document-list.component.html`
- Create: `src/app/features/document-list/document-list.component.scss`

**Interfaces:**
- Consumes: `DocumentService.getAll(): Observable<DocumentResponse[]>`
- Produces: table of documents; clicking a row navigates to `/documents/:id`

- [ ] **Step 1: Replace `document-list.component.ts`**

Replace the full content of `src/app/features/document-list/document-list.component.ts`:

```typescript
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { DatePipe, CurrencyPipe } from '@angular/common';
import { DocumentService } from '../../core/services/document.service';
import { DocumentResponse } from '../../core/models/document.model';

@Component({
  selector: 'app-document-list',
  standalone: true,
  imports: [
    MatTableModule, MatCardModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, MatSnackBarModule, DatePipe, CurrencyPipe
  ],
  templateUrl: './document-list.component.html',
  styleUrl: './document-list.component.scss'
})
export class DocumentListComponent implements OnInit {
  documents: DocumentResponse[] = [];
  loading = false;
  displayedColumns = ['originalFileName', 'documentType', 'vendor', 'totalAmount', 'status', 'uploadedAt'];

  constructor(
    private documentService: DocumentService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.documentService.getAll().subscribe({
      next: (docs) => {
        this.documents = docs;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.snackBar.open('Failed to load documents.', 'Dismiss', { duration: 4000 });
      }
    });
  }

  openDetail(id: string): void {
    this.router.navigate(['/documents', id]);
  }
}
```

- [ ] **Step 2: Create `document-list.component.html`**

Create `src/app/features/document-list/document-list.component.html`:

```html
<mat-card>
  <mat-card-header>
    <mat-card-title>Documents</mat-card-title>
    <span class="header-spacer"></span>
    <button mat-icon-button (click)="load()" matTooltip="Refresh">
      <mat-icon>refresh</mat-icon>
    </button>
  </mat-card-header>

  <mat-card-content>
    @if (loading) {
      <div class="spinner-row">
        <mat-spinner diameter="40"></mat-spinner>
      </div>
    } @else {
      <mat-table [dataSource]="documents">

        <ng-container matColumnDef="originalFileName">
          <mat-header-cell *matHeaderCellDef>File Name</mat-header-cell>
          <mat-cell *matCellDef="let doc">{{ doc.originalFileName }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="documentType">
          <mat-header-cell *matHeaderCellDef>Type</mat-header-cell>
          <mat-cell *matCellDef="let doc">{{ doc.documentType ?? '—' }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="vendor">
          <mat-header-cell *matHeaderCellDef>Vendor</mat-header-cell>
          <mat-cell *matCellDef="let doc">{{ doc.vendor ?? '—' }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="totalAmount">
          <mat-header-cell *matHeaderCellDef>Amount</mat-header-cell>
          <mat-cell *matCellDef="let doc">
            {{ doc.totalAmount != null ? (doc.totalAmount | currency: (doc.currency ?? 'USD')) : '—' }}
          </mat-cell>
        </ng-container>

        <ng-container matColumnDef="status">
          <mat-header-cell *matHeaderCellDef>Status</mat-header-cell>
          <mat-cell *matCellDef="let doc">
            <span [class]="'status-chip status-' + doc.status">{{ doc.status }}</span>
          </mat-cell>
        </ng-container>

        <ng-container matColumnDef="uploadedAt">
          <mat-header-cell *matHeaderCellDef>Uploaded</mat-header-cell>
          <mat-cell *matCellDef="let doc">{{ doc.uploadedAt | date: 'dd MMM yyyy, HH:mm' }}</mat-cell>
        </ng-container>

        <mat-header-row *matHeaderRowDef="displayedColumns"></mat-header-row>
        <mat-row
          *matRowDef="let row; columns: displayedColumns;"
          class="clickable-row"
          (click)="openDetail(row.id)">
        </mat-row>

        <tr class="mat-row" *matNoDataRow>
          <td class="mat-cell empty-row" [attr.colspan]="displayedColumns.length">
            No documents uploaded yet.
          </td>
        </tr>

      </mat-table>
    }
  </mat-card-content>
</mat-card>
```

- [ ] **Step 3: Create `document-list.component.scss`**

Create `src/app/features/document-list/document-list.component.scss`:

```scss
mat-card-header {
  display: flex;
  align-items: center;
}

.header-spacer {
  flex: 1 1 auto;
}

.spinner-row {
  display: flex;
  justify-content: center;
  padding: 40px 0;
}

.clickable-row {
  cursor: pointer;
  &:hover {
    background: rgba(0, 0, 0, 0.04);
  }
}

.status-chip {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;

  &.status-DONE {
    background: #e8f5e9;
    color: #2e7d32;
  }

  &.status-PROCESSING {
    background: #fff3e0;
    color: #e65100;
  }

  &.status-FAILED {
    background: #ffebee;
    color: #c62828;
  }
}

.empty-row {
  text-align: center;
  padding: 40px;
  color: rgba(0, 0, 0, 0.4);
}
```

- [ ] **Step 4: Verify in browser**

```powershell
ng serve
```

Navigate to `http://localhost:4200/documents`. Verify:
- Table renders with correct column headers
- Spinner shows briefly (when backend is not running, an error snack-bar appears — that's correct)
- Refresh button is visible in the card header
- No TypeScript/template compilation errors in the console

Stop the server.

---

## Task 6: DocumentDetailComponent

**Files:**
- Modify: `src/app/features/document-detail/document-detail.component.ts` (replace stub)
- Create: `src/app/features/document-detail/document-detail.component.html`
- Create: `src/app/features/document-detail/document-detail.component.scss`

**Interfaces:**
- Consumes: `DocumentService.getById(id: string): Observable<DocumentResponse>`, `ActivatedRoute.snapshot.paramMap.get('id')`
- Produces: a detail card with all document fields + line items table; polls every 3 s while `status === 'PROCESSING'`; unsubscribes on destroy

- [ ] **Step 1: Replace `document-detail.component.ts`**

Replace the full content of `src/app/features/document-detail/document-detail.component.ts`:

```typescript
import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription, interval } from 'rxjs';
import { switchMap, takeWhile } from 'rxjs/operators';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { DatePipe, CurrencyPipe } from '@angular/common';
import { DocumentService } from '../../core/services/document.service';
import { DocumentResponse } from '../../core/models/document.model';

@Component({
  selector: 'app-document-detail',
  standalone: true,
  imports: [
    MatCardModule, MatButtonModule, MatIconModule, MatTableModule,
    MatProgressSpinnerModule, MatDividerModule, MatSnackBarModule,
    DatePipe, CurrencyPipe
  ],
  templateUrl: './document-detail.component.html',
  styleUrl: './document-detail.component.scss'
})
export class DocumentDetailComponent implements OnInit, OnDestroy {
  document: DocumentResponse | null = null;
  loading = true;
  lineItemColumns = ['description', 'quantity', 'unitPrice', 'totalPrice'];

  private pollSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private documentService: DocumentService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.documentService.getById(id).subscribe({
      next: (doc) => {
        this.document = doc;
        this.loading = false;
        if (doc.status === 'PROCESSING') {
          this.startPolling(id);
        }
      },
      error: () => {
        this.loading = false;
        this.snackBar.open('Failed to load document.', 'Dismiss', { duration: 4000 });
      }
    });
  }

  private startPolling(id: string): void {
    this.pollSub = interval(3000).pipe(
      switchMap(() => this.documentService.getById(id)),
      takeWhile(doc => doc.status === 'PROCESSING', true)
    ).subscribe({
      next: (doc) => { this.document = doc; },
      error: () => {
        this.snackBar.open('Lost connection while polling.', 'Dismiss', { duration: 4000 });
      }
    });
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
  }

  back(): void {
    this.router.navigate(['/documents']);
  }
}
```

- [ ] **Step 2: Create `document-detail.component.html`**

Create `src/app/features/document-detail/document-detail.component.html`:

```html
@if (loading) {
  <div class="spinner-row">
    <mat-spinner diameter="48"></mat-spinner>
  </div>
}

@if (!loading && document) {
  <mat-card>
    <mat-card-header>
      <mat-card-title>{{ document.originalFileName }}</mat-card-title>
      <mat-card-subtitle>
        <span [class]="'status-chip status-' + document.status">{{ document.status }}</span>
        @if (document.status === 'PROCESSING') {
          <span class="polling-hint"> — checking for updates…</span>
        }
      </mat-card-subtitle>
    </mat-card-header>

    <mat-card-content>
      @if (document.status === 'DONE') {
        <div class="fields-grid">
          <div class="field">
            <span class="label">Document Type</span>
            <span class="value">{{ document.documentType ?? '—' }}</span>
          </div>
          <div class="field">
            <span class="label">Vendor</span>
            <span class="value">{{ document.vendor ?? '—' }}</span>
          </div>
          <div class="field">
            <span class="label">Total Amount</span>
            <span class="value">
              {{ document.totalAmount != null
                ? (document.totalAmount | currency: (document.currency ?? 'USD'))
                : '—' }}
            </span>
          </div>
          <div class="field">
            <span class="label">Document Date</span>
            <span class="value">{{ document.documentDate ?? '—' }}</span>
          </div>
          <div class="field">
            <span class="label">Uploaded At</span>
            <span class="value">{{ document.uploadedAt | date: 'dd MMM yyyy, HH:mm' }}</span>
          </div>
        </div>

        @if (document.summary) {
          <mat-divider></mat-divider>
          <div class="summary-section">
            <span class="label">Summary</span>
            <p>{{ document.summary }}</p>
          </div>
        }

        @if (document.lineItems && document.lineItems.length > 0) {
          <mat-divider></mat-divider>
          <h3 class="line-items-title">Line Items</h3>
          <mat-table [dataSource]="document.lineItems">
            <ng-container matColumnDef="description">
              <mat-header-cell *matHeaderCellDef>Description</mat-header-cell>
              <mat-cell *matCellDef="let item">{{ item.description }}</mat-cell>
            </ng-container>
            <ng-container matColumnDef="quantity">
              <mat-header-cell *matHeaderCellDef>Qty</mat-header-cell>
              <mat-cell *matCellDef="let item">{{ item.quantity }}</mat-cell>
            </ng-container>
            <ng-container matColumnDef="unitPrice">
              <mat-header-cell *matHeaderCellDef>Unit Price</mat-header-cell>
              <mat-cell *matCellDef="let item">{{ item.unitPrice | currency }}</mat-cell>
            </ng-container>
            <ng-container matColumnDef="totalPrice">
              <mat-header-cell *matHeaderCellDef>Total</mat-header-cell>
              <mat-cell *matCellDef="let item">{{ item.totalPrice | currency }}</mat-cell>
            </ng-container>
            <mat-header-row *matHeaderRowDef="lineItemColumns"></mat-header-row>
            <mat-row *matRowDef="let row; columns: lineItemColumns;"></mat-row>
          </mat-table>
        }
      }

      @if (document.status === 'FAILED') {
        <p class="failed-msg">Processing failed. The Gemini API was unable to extract data from this document.</p>
      }
    </mat-card-content>

    <mat-card-actions>
      <button mat-button (click)="back()">
        <mat-icon>arrow_back</mat-icon> Back to Documents
      </button>
    </mat-card-actions>
  </mat-card>
}
```

- [ ] **Step 3: Create `document-detail.component.scss`**

Create `src/app/features/document-detail/document-detail.component.scss`:

```scss
.spinner-row {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

.status-chip {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;

  &.status-DONE { background: #e8f5e9; color: #2e7d32; }
  &.status-PROCESSING { background: #fff3e0; color: #e65100; }
  &.status-FAILED { background: #ffebee; color: #c62828; }
}

.polling-hint {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.5);
}

.fields-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  margin: 16px 0;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.label {
  font-size: 11px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.5);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.value {
  font-size: 15px;
}

.summary-section {
  margin: 16px 0;
  p { margin: 6px 0 0; line-height: 1.6; }
}

.line-items-title {
  margin: 16px 0 8px;
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.7);
}

.failed-msg {
  color: #c62828;
  margin: 16px 0;
}
```

- [ ] **Step 4: Verify in browser**

```powershell
ng serve
```

Navigate to `http://localhost:4200/documents`. Verify:
- No compilation errors in terminal or browser console
- Navigating to `/documents/some-fake-id` shows a spinner briefly then a snack-bar error (backend not running — correct behaviour)
- "Back to Documents" button navigates back to `/documents`

Stop the server.

---

## Task 7: StatsComponent

**Files:**
- Modify: `src/app/features/stats/stats.component.ts` (replace stub)
- Create: `src/app/features/stats/stats.component.html`
- Create: `src/app/features/stats/stats.component.scss`

**Interfaces:**
- Consumes: `DocumentService.getStats(): Observable<StatsResponse>` where `StatsResponse.byType` is `Record<string, number>` (count per type, not spend per type)
- Produces: three summary tiles + bar chart (count by type) + doughnut chart (count by type)

> **Note on chart data:** The backend `byType` field is a count per document type, not spend per type. The bar chart therefore shows document count by type. Total spend is shown as a summary tile.

- [ ] **Step 1: Replace `stats.component.ts`**

Replace the full content of `src/app/features/stats/stats.component.ts`:

```typescript
import { Component, OnInit } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CurrencyPipe } from '@angular/common';
import { BaseChartDirective } from 'ng2-charts';
import {
  Chart, CategoryScale, LinearScale, BarElement,
  Title, Tooltip, Legend, ArcElement
} from 'chart.js';
import { ChartData, ChartOptions } from 'chart.js';
import { DocumentService } from '../../core/services/document.service';
import { StatsResponse } from '../../core/models/document.model';

Chart.register(CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend, ArcElement);

@Component({
  selector: 'app-stats',
  standalone: true,
  imports: [MatCardModule, MatProgressSpinnerModule, MatSnackBarModule, CurrencyPipe, BaseChartDirective],
  templateUrl: './stats.component.html',
  styleUrl: './stats.component.scss'
})
export class StatsComponent implements OnInit {
  stats: StatsResponse | null = null;
  loading = true;

  barChartData: ChartData<'bar'> = {
    labels: [],
    datasets: [{ data: [], label: 'Document Count', backgroundColor: '#3f51b5' }]
  };
  barChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    plugins: { legend: { position: 'top' } },
    scales: { y: { beginAtZero: true, ticks: { stepSize: 1 } } }
  };

  doughnutChartData: ChartData<'doughnut'> = {
    labels: [],
    datasets: [{
      data: [],
      backgroundColor: ['#3f51b5', '#e91e63', '#009688', '#ff9800', '#9c27b0']
    }]
  };
  doughnutChartOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    plugins: { legend: { position: 'right' } }
  };

  constructor(
    private documentService: DocumentService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.documentService.getStats().subscribe({
      next: (stats) => {
        this.stats = stats;
        this.loading = false;
        const labels = Object.keys(stats.byType);
        const counts = Object.values(stats.byType);
        this.barChartData = {
          labels,
          datasets: [{ data: counts, label: 'Document Count', backgroundColor: '#3f51b5' }]
        };
        this.doughnutChartData = {
          labels,
          datasets: [{
            data: counts,
            backgroundColor: ['#3f51b5', '#e91e63', '#009688', '#ff9800', '#9c27b0']
          }]
        };
      },
      error: () => {
        this.loading = false;
        this.snackBar.open('Failed to load stats.', 'Dismiss', { duration: 4000 });
      }
    });
  }
}
```

- [ ] **Step 2: Create `stats.component.html`**

Create `src/app/features/stats/stats.component.html`:

```html
@if (loading) {
  <div class="spinner-row">
    <mat-spinner diameter="48"></mat-spinner>
  </div>
}

@if (!loading && stats) {
  <div class="stats-grid summary-tiles">
    <mat-card class="tile">
      <mat-card-content>
        <div class="tile-value">{{ stats.totalDocuments }}</div>
        <div class="tile-label">Total Documents</div>
      </mat-card-content>
    </mat-card>

    <mat-card class="tile">
      <mat-card-content>
        <div class="tile-value">{{ stats.totalSpend | currency: 'USD' }}</div>
        <div class="tile-label">Total Spend</div>
      </mat-card-content>
    </mat-card>

    <mat-card class="tile">
      <mat-card-content>
        <div class="tile-value">{{ (stats.byType | keyvalue).length }}</div>
        <div class="tile-label">Document Types</div>
      </mat-card-content>
    </mat-card>
  </div>

  <div class="charts-grid">
    <mat-card>
      <mat-card-header>
        <mat-card-title>Documents by Type</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        <canvas baseChart
          [data]="barChartData"
          [options]="barChartOptions"
          type="bar">
        </canvas>
      </mat-card-content>
    </mat-card>

    <mat-card>
      <mat-card-header>
        <mat-card-title>Type Distribution</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        <canvas baseChart
          [data]="doughnutChartData"
          [options]="doughnutChartOptions"
          type="doughnut">
        </canvas>
      </mat-card-content>
    </mat-card>
  </div>
}
```

- [ ] **Step 3: Create `stats.component.scss`**

Create `src/app/features/stats/stats.component.scss`:

```scss
.spinner-row {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

.summary-tiles {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;

  @media (max-width: 600px) {
    grid-template-columns: 1fr;
  }
}

.tile {
  text-align: center;

  .tile-value {
    font-size: 2rem;
    font-weight: 700;
    color: #3f51b5;
  }

  .tile-label {
    font-size: 13px;
    color: rgba(0, 0, 0, 0.5);
    margin-top: 4px;
  }
}

.charts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}
```

- [ ] **Step 4: Add `KeyValuePipe` import for the template**

The template uses `stats.byType | keyvalue`. `KeyValuePipe` must be imported in the component.

Open `src/app/features/stats/stats.component.ts` and add `KeyValuePipe` to the imports:

```typescript
import { CurrencyPipe, KeyValuePipe } from '@angular/common';

// in @Component:
imports: [MatCardModule, MatProgressSpinnerModule, MatSnackBarModule, CurrencyPipe, KeyValuePipe, BaseChartDirective],
```

- [ ] **Step 5: Verify full app in browser**

```powershell
ng serve
```

Verify each route:
- `http://localhost:4200/upload` — upload card renders, file picker works
- `http://localhost:4200/documents` — table renders with headers, snack-bar on load error (no backend)
- `http://localhost:4200/stats` — three tiles and two chart canvases render (charts empty without backend, no errors)
- Navbar links highlight when active
- No TypeScript errors in terminal

Stop the server.

---

## Self-Review

**Spec coverage check:**
- ✅ `/upload` route with file input, validation, progress bar, snack-bar, navigate on success
- ✅ `/documents` route with mat-table, status chips (green/amber/red), refresh button, row click nav
- ✅ `/documents/:id` route with polling via `interval(3000) + switchMap + takeWhile`, line items table, back button
- ✅ `/stats` route with 3 summary tiles, bar chart, doughnut chart
- ✅ `DocumentService` with all 4 methods
- ✅ TypeScript models matching backend DTOs exactly
- ✅ Angular Material (indigo-pink) throughout
- ✅ ng2-charts for bar + doughnut
- ✅ `environment.apiUrl` — never hardcoded
- ✅ Sibling folder `doc-intelligence-frontend`
- ✅ Standalone components
- ✅ All errors shown as `mat-snack-bar` (4 s duration)
- ✅ Spinners on initial load, progress bar on upload

**Placeholder scan:** None found.

**Type consistency:**
- `DocumentService.upload()` → `Observable<DocumentResponse>` — `UploadComponent` uses `doc.id` ✅
- `DocumentService.getAll()` → `Observable<DocumentResponse[]>` — `DocumentListComponent` binds `documents` array ✅
- `DocumentService.getById()` → `Observable<DocumentResponse>` — `DocumentDetailComponent` assigns to `document: DocumentResponse | null` ✅
- `DocumentService.getStats()` → `Observable<StatsResponse>` — `StatsComponent` reads `stats.totalDocuments`, `stats.totalSpend`, `stats.byType` ✅
- `lineItemColumns` in detail = `['description', 'quantity', 'unitPrice', 'totalPrice']` — all match `LineItem` interface fields ✅
