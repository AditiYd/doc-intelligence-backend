package project2.example.proj.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import project2.example.proj.dto.DocumentResponse;
import project2.example.proj.dto.StatsResponse;
import project2.example.proj.service.DocumentService;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    @Test
    void upload_validFile_returns200WithProcessingStatus() throws Exception {
        DocumentResponse response = new DocumentResponse();
        response.setId("doc-1");
        response.setOriginalFileName("invoice.pdf");
        response.setStatus("PROCESSING");

        when(documentService.upload(any())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile(
            "file", "invoice.pdf", "application/pdf", "pdf data".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PROCESSING"))
            .andExpect(jsonPath("$.originalFileName").value("invoice.pdf"));
    }

    @Test
    void upload_unsupportedType_returns400() throws Exception {
        when(documentService.upload(any()))
            .thenThrow(new IllegalArgumentException("Unsupported file type"));

        MockMultipartFile file = new MockMultipartFile(
            "file", "doc.docx", "application/octet-stream", "data".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload").file(file))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getAll_returnsListOfDocuments() throws Exception {
        DocumentResponse doc = new DocumentResponse();
        doc.setId("doc-1");
        doc.setStatus("DONE");

        when(documentService.getAll()).thenReturn(List.of(doc));

        mockMvc.perform(get("/api/documents"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("doc-1"))
            .andExpect(jsonPath("$[0].status").value("DONE"));
    }

    @Test
    void getById_existingId_returnsDocument() throws Exception {
        DocumentResponse doc = new DocumentResponse();
        doc.setId("doc-1");
        doc.setStatus("DONE");

        when(documentService.getById("doc-1")).thenReturn(doc);

        mockMvc.perform(get("/api/documents/doc-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("doc-1"));
    }

    @Test
    void getById_missingId_returns404() throws Exception {
        when(documentService.getById("missing"))
            .thenThrow(new RuntimeException("Document not found"));

        mockMvc.perform(get("/api/documents/missing"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getStats_returnsStatsResponse() throws Exception {
        StatsResponse stats = new StatsResponse();
        stats.setTotalDocuments(3);
        stats.setByType(Map.of("INVOICE", 2L, "RECEIPT", 1L));
        stats.setTotalSpend(175.0);

        when(documentService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/api/documents/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalDocuments").value(3))
            .andExpect(jsonPath("$.totalSpend").value(175.0));
    }
}
