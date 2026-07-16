package project2.example.proj.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import project2.example.proj.dto.GeminiExtractionResult;
import project2.example.proj.model.DocumentRecord;
import project2.example.proj.repository.DocumentRepository;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentProcessingServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private GeminiService geminiService;

    @InjectMocks
    private DocumentProcessingService documentProcessingService;

    @Test
    void processAsync_setsStatusDone_onSuccess() throws Exception {
        DocumentRecord doc = new DocumentRecord();
        doc.setFilePath("uploads/test.txt");
        doc.setStatus("PROCESSING");

        GeminiExtractionResult result = new GeminiExtractionResult();
        result.setDocumentType("INVOICE");
        result.setVendor("Acme");
        result.setTotalAmount(100.0);
        result.setCurrency("USD");
        result.setDocumentDate("2024-01-15");
        result.setSummary("Test summary.");
        result.setLineItems(List.of());

        when(documentRepository.findById("doc-1")).thenReturn(Optional.of(doc));
        when(fileStorageService.extractText("uploads/test.txt")).thenReturn("some text");
        when(geminiService.extract("some text")).thenReturn(result);
        when(documentRepository.save(any())).thenReturn(doc);

        documentProcessingService.processAsync("doc-1");

        verify(documentRepository).save(argThat(d -> "DONE".equals(d.getStatus())));
    }

    @Test
    void processAsync_setsStatusFailed_onGeminiException() throws Exception {
        DocumentRecord doc = new DocumentRecord();
        doc.setFilePath("uploads/test.txt");
        doc.setStatus("PROCESSING");

        when(documentRepository.findById("doc-1")).thenReturn(Optional.of(doc));
        when(fileStorageService.extractText("uploads/test.txt")).thenReturn("some text");
        when(geminiService.extract("some text")).thenThrow(new RuntimeException("Gemini unavailable"));
        when(documentRepository.save(any())).thenReturn(doc);

        documentProcessingService.processAsync("doc-1");

        verify(documentRepository).save(argThat(d -> "FAILED".equals(d.getStatus())));
    }
}
