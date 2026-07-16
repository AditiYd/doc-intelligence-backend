package project2.example.proj.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project2.example.proj.dto.GeminiExtractionResult;
import project2.example.proj.model.DocumentRecord;
import project2.example.proj.model.LineItem;
import project2.example.proj.repository.DocumentRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final GeminiService geminiService;

    @Async("docProcessingExecutor")
    @Transactional
    public void processAsync(String documentId) {
        DocumentRecord doc = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalStateException("Document not found: " + documentId));
        try {
            String text = fileStorageService.extractText(doc.getFilePath());
            GeminiExtractionResult result = geminiService.extract(text);

            doc.setDocumentType(result.getDocumentType());
            doc.setVendor(result.getVendor());
            doc.setTotalAmount(result.getTotalAmount());
            doc.setCurrency(result.getCurrency());
            doc.setSummary(result.getSummary());

            if (result.getDocumentDate() != null) {
                doc.setDocumentDate(LocalDate.parse(result.getDocumentDate()));
            }

            List<LineItem> lineItems = new ArrayList<>();
            if (result.getLineItems() != null) {
                for (GeminiExtractionResult.LineItemResult li : result.getLineItems()) {
                    LineItem lineItem = new LineItem();
                    lineItem.setDescription(li.getDescription());
                    lineItem.setQuantity(li.getQuantity());
                    lineItem.setUnitPrice(li.getUnitPrice());
                    lineItem.setTotalPrice(li.getTotalPrice());
                    lineItem.setDocument(doc);
                    lineItems.add(lineItem);
                }
            }
            doc.getLineItems().clear();
            doc.getLineItems().addAll(lineItems);
            doc.setStatus("DONE");
            documentRepository.save(doc);

        } catch (Exception e) {
            log.error("Processing failed for document {}: {}", documentId, e.getMessage(), e);
            doc.setStatus("FAILED");
            documentRepository.save(doc);
        }
    }
}
