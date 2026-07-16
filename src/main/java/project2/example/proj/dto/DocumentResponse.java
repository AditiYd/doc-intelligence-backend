package project2.example.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class DocumentResponse {

    private String id;
    private String originalFileName;

    @Schema(description = "Type of document as classified by extraction, e.g. Invoice, Receipt")
    private String documentType;

    private String vendor;
    private Double totalAmount;

    @Schema(description = "ISO 4217 currency code", example = "USD")
    private String currency;

    private LocalDate documentDate;
    private String summary;
    private LocalDateTime uploadedAt;

    @Schema(description = "Processing status", allowableValues = {"PROCESSING", "DONE", "FAILED"})
    private String status;

    private List<LineItemResponse> lineItems;

    @Data
    @NoArgsConstructor
    public static class LineItemResponse {
        private String id;
        private String description;
        private Integer quantity;
        private Double unitPrice;
        private Double totalPrice;
    }
}
