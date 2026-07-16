package project2.example.proj.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class GeminiExtractionResult {

    private String documentType;
    private String vendor;
    private Double totalAmount;
    private String currency;
    private String documentDate;
    private String summary;
    private List<LineItemResult> lineItems;

    @Data
    @NoArgsConstructor
    public static class LineItemResult {
        private String description;
        private Integer quantity;
        private Double unitPrice;
        private Double totalPrice;
    }
}
