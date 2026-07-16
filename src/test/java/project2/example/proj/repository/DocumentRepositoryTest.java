package project2.example.proj.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import project2.example.proj.model.DocumentRecord;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "gemini.api.key=test-placeholder")
@Transactional
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
