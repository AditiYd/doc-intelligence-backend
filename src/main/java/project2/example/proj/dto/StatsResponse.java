package project2.example.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class StatsResponse {

    private int totalDocuments;

    @Schema(description = "Count of documents grouped by document type")
    private Map<String, Long> byType;

    @Schema(description = "Sum of totalAmount across all processed documents")
    private Double totalSpend;
}
