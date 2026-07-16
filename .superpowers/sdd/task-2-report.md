# Task 2 Report: JPA Entities — DocumentRecord and LineItem

**Status:** DONE

---

## Implementation Summary

### Step 1: Write Failing Test ✓
Created `src/test/java/project2/example/proj/model/DocumentRecordTest.java` with two test methods:
- `prePersist_setsUuidId()` — verifies that `DocumentRecord.prePersist()` generates a 36-character UUID string
- `lineItems_defaultsToEmptyList()` — verifies that the `lineItems` collection initializes as empty

### Step 2: Run Test to Verify Failure ✓
Test compilation failed as expected with "cannot find symbol: class DocumentRecord" error.
- Build output confirmed 4 compilation errors
- Tests could not run due to missing entity class

### Step 3: Create DocumentRecord Entity ✓
Created `src/main/java/project2/example/proj/model/DocumentRecord.java` with:
- `@Entity` annotation for JPA mapping
- `@Id` String field for UUID storage
- All required fields:
  - `originalFileName`, `filePath`, `documentType`, `vendor`
  - `totalAmount` (Double), `currency`
  - `documentDate` (LocalDate), `uploadedAt` (LocalDateTime)
  - `status`, `summary` (with `@Column(length = 2000)`)
  - `lineItems` (List initialized to empty ArrayList)
- `@OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)` relationship
- `@PrePersist` method to generate UUID if null
- Lombok `@Data` and `@NoArgsConstructor` annotations

### Step 4: Create LineItem Entity ✓
Created `src/main/java/project2/example/proj/model/LineItem.java` with:
- `@Entity` annotation for JPA mapping
- `@Id` String field for UUID storage
- Required fields:
  - `description`, `quantity` (Integer)
  - `unitPrice` (Double), `totalPrice` (Double)
  - `document` (DocumentRecord reference)
- `@ManyToOne(fetch = FetchType.LAZY)` with `@JoinColumn(name = "document_id")` for back-reference
- `@PrePersist` method to generate UUID if null
- Lombok `@Data` and `@NoArgsConstructor` annotations

### Step 5: Run Test to Verify Success ✓
Tests executed successfully:
```
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Compliance Checklist

- ✓ Package: `project2.example.proj.model`
- ✓ JPA annotations from `jakarta.persistence.*` (not `javax.persistence.*`)
- ✓ String UUIDs generated via `@PrePersist`
- ✓ DocumentRecord and LineItem entities created with correct field types
- ✓ Bidirectional relationship: DocumentRecord → `@OneToMany(mappedBy)` to LineItem
- ✓ LineItem → `@ManyToOne` back to DocumentRecord
- ✓ LineItem collection defaults to empty ArrayList
- ✓ All tests pass
- ✓ Spring Boot 4.1.0 and Java 25 versions unchanged in pom.xml

---

## Files Created

1. `src/test/java/project2/example/proj/model/DocumentRecordTest.java`
2. `src/main/java/project2/example/proj/model/DocumentRecord.java`
3. `src/main/java/project2/example/proj/model/LineItem.java`

---

## Test Results

```
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.139 s
```

Both unit tests pass:
- UUID generation verified: 36-character strings produced correctly
- LineItem collection initialization verified: empty by default

---

## Concerns

None. Implementation follows all requirements exactly as specified in task brief.
