package project2.example.proj.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import project2.example.proj.dto.DocumentResponse;
import project2.example.proj.dto.StatsResponse;
import project2.example.proj.service.DocumentService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Tag(name = "Documents", description = "Upload documents and query their extracted data")
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @Operation(
        summary = "Upload a document",
        description = "Uploads a PDF or TXT file (max 10 MB) and kicks off asynchronous extraction of its data."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Document accepted and stored",
            content = @Content(schema = @Schema(implementation = DocumentResponse.class))),
        @ApiResponse(responseCode = "400", description = "No file provided or file failed validation",
            content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "500", description = "File could not be read or stored",
            content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
        @Parameter(description = "File to upload (PDF or TXT)", required = true)
        @RequestParam("file") MultipartFile file
    ) {
        try {
            DocumentResponse response = documentService.upload(file);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "File processing failed"));
        }
    }

    @Operation(summary = "List all documents", description = "Returns every uploaded document and its current processing status.")
    @ApiResponse(responseCode = "200", description = "Documents returned")
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getAll() {
        return ResponseEntity.ok(documentService.getAll());
    }

    @Operation(summary = "Get a document by id", description = "Returns a single document, including extracted fields and line items once processing is done.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Document found"),
        @ApiResponse(responseCode = "404", description = "No document with the given id")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(
        @Parameter(description = "Document id", required = true)
        @PathVariable String id
    ) {
        try {
            return ResponseEntity.ok(documentService.getById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get aggregate stats", description = "Returns totals across all documents: count, spend, and breakdown by document type.")
    @ApiResponse(responseCode = "200", description = "Stats returned")
    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getStats() {
        return ResponseEntity.ok(documentService.getStats());
    }
}
