package project2.example.proj.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import project2.example.proj.dto.DocumentResponse;
import project2.example.proj.dto.StatsResponse;
import project2.example.proj.model.DocumentRecord;
import project2.example.proj.repository.DocumentRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final DocumentProcessingService documentProcessingService;

    private static final List<String> SUPPORTED = List.of(".pdf", ".txt");

    public DocumentResponse upload(MultipartFile file) throws IOException {
        String name = file.getOriginalFilename();
        if (name == null || SUPPORTED.stream().noneMatch(ext -> name.toLowerCase().endsWith(ext))) {
            throw new IllegalArgumentException("Unsupported file type. Only PDF and TXT are allowed.");
        }

        String filePath = fileStorageService.save(file);

        DocumentRecord doc = new DocumentRecord();
        doc.setOriginalFileName(name);
        doc.setFilePath(filePath);
        doc.setStatus("PROCESSING");
        doc.setUploadedAt(LocalDateTime.now());
        DocumentRecord saved = documentRepository.save(doc);

        documentProcessingService.processAsync(saved.getId());
        return toResponse(saved);
    }

    public List<DocumentResponse> getAll() {
        return documentRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public DocumentResponse getById(String id) {
        DocumentRecord doc = documentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Document not found: " + id));
        return toResponse(doc);
    }

    public StatsResponse getStats() {
        List<DocumentRecord> all = documentRepository.findAll();

        Map<String, Long> byType = all.stream()
            .filter(d -> d.getDocumentType() != null)
            .collect(Collectors.groupingBy(DocumentRecord::getDocumentType, Collectors.counting()));

        double totalSpend = all.stream()
            .filter(d -> d.getTotalAmount() != null)
            .mapToDouble(DocumentRecord::getTotalAmount)
            .sum();

        StatsResponse stats = new StatsResponse();
        stats.setTotalDocuments(all.size());
        stats.setByType(byType);
        stats.setTotalSpend(totalSpend);
        return stats;
    }

    private DocumentResponse toResponse(DocumentRecord doc) {
        DocumentResponse resp = new DocumentResponse();
        resp.setId(doc.getId());
        resp.setOriginalFileName(doc.getOriginalFileName());
        resp.setDocumentType(doc.getDocumentType());
        resp.setVendor(doc.getVendor());
        resp.setTotalAmount(doc.getTotalAmount());
        resp.setCurrency(doc.getCurrency());
        resp.setDocumentDate(doc.getDocumentDate());
        resp.setSummary(doc.getSummary());
        resp.setUploadedAt(doc.getUploadedAt());
        resp.setStatus(doc.getStatus());

        if (doc.getLineItems() != null) {
            resp.setLineItems(doc.getLineItems().stream().map(li -> {
                DocumentResponse.LineItemResponse liResp = new DocumentResponse.LineItemResponse();
                liResp.setId(li.getId());
                liResp.setDescription(li.getDescription());
                liResp.setQuantity(li.getQuantity());
                liResp.setUnitPrice(li.getUnitPrice());
                liResp.setTotalPrice(li.getTotalPrice());
                return liResp;
            }).collect(Collectors.toList()));
        }
        return resp;
    }
}
