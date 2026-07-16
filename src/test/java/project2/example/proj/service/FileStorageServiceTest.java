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
