## Task 4: DTOs

**Files:**
- Create: `src/main/java/project2/example/proj/dto/GeminiExtractionResult.java`
- Create: `src/main/java/project2/example/proj/dto/DocumentResponse.java`
- Create: `src/main/java/project2/example/proj/dto/StatsResponse.java`

**Interfaces:**
- Produces:
  - `GeminiExtractionResult` — Jackson-deserializable POJO matching the Gemini JSON response
  - `DocumentResponse` — API response with nested `LineItemResponse`
  - `StatsResponse` — `totalDocuments (int)`, `byType (Map<String,Long>)`, `totalSpend (Double)`

No test needed for plain data classes — they are exercised by Task 6, 8, and 9 tests.

- [ ] **Step 1: Create GeminiExtractionResult.java**

Create `src/main/java/project2/example/proj/dto/GeminiExtractionResult.java`:

```java
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
```

- [ ] **Step 2: Create DocumentResponse.java**

Create `src/main/java/project2/example/proj/dto/DocumentResponse.java`:

```java
package project2.example.proj.dto;

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
    private String documentType;
    private String vendor;
    private Double totalAmount;
    private String currency;
    private LocalDate documentDate;
    private String summary;
    private LocalDateTime uploadedAt;
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
```

- [ ] **Step 3: Create StatsResponse.java**

Create `src/main/java/project2/example/proj/dto/StatsResponse.java`:

```java
package project2.example.proj.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class StatsResponse {
    private int totalDocuments;
    private Map<String, Long> byType;
    private Double totalSpend;
}
```

- [ ] **Step 4: Verify compilation**

```
./mvnw compile
```
Expected: BUILD SUCCESS, no errors.

---

