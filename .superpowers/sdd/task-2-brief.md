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

