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

