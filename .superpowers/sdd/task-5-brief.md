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

