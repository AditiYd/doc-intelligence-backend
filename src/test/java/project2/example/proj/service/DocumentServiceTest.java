package project2.example.proj.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import project2.example.proj.dto.DocumentResponse;
import project2.example.proj.dto.StatsResponse;
import project2.example.proj.model.DocumentRecord;
import project2.example.proj.repository.DocumentRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private DocumentProcessingService documentProcessingService;

    @InjectMocks
    private DocumentService documentService;

    @Test
    void upload_validPdf_returnsProcessingStatus() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "invoice.pdf", "application/pdf", "pdf content".getBytes()
        );
        DocumentRecord saved = new DocumentRecord();
        saved.setId("doc-123");
        saved.setOriginalFileName("invoice.pdf");
        saved.setStatus("PROCESSING");
        saved.setUploadedAt(LocalDateTime.now());

        when(fileStorageService.save(file)).thenReturn("uploads/invoice.pdf");
        when(documentRepository.save(any())).thenReturn(saved);

        DocumentResponse response = documentService.upload(file);

        assertThat(response.getStatus()).isEqualTo("PROCESSING");
        assertThat(response.getOriginalFileName()).isEqualTo("invoice.pdf");
        verify(documentProcessingService).processAsync("doc-123");
    }

    @Test
    void upload_unsupportedExtension_throwsIllegalArgumentException() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "doc.docx", "application/octet-stream", "data".getBytes()
        );
        assertThatThrownBy(() -> documentService.upload(file))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported file type");
    }

    @Test
    void getById_notFound_throwsRuntimeException() {
        when(documentRepository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> documentService.getById("missing"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Document not found");
    }

    @Test
    void getStats_returnsCorrectCounts() {
        DocumentRecord inv1 = new DocumentRecord();
        inv1.setDocumentType("INVOICE");
        inv1.setTotalAmount(100.0);

        DocumentRecord inv2 = new DocumentRecord();
        inv2.setDocumentType("INVOICE");
        inv2.setTotalAmount(50.0);

        DocumentRecord receipt = new DocumentRecord();
        receipt.setDocumentType("RECEIPT");
        receipt.setTotalAmount(25.0);

        when(documentRepository.findAll()).thenReturn(List.of(inv1, inv2, receipt));

        StatsResponse stats = documentService.getStats();

        assertThat(stats.getTotalDocuments()).isEqualTo(3);
        assertThat(stats.getByType()).containsEntry("INVOICE", 2L);
        assertThat(stats.getByType()).containsEntry("RECEIPT", 1L);
        assertThat(stats.getTotalSpend()).isEqualTo(175.0);
    }
}
