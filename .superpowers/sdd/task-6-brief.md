## Task 6: GeminiService

**Files:**
- Create: `src/main/java/project2/example/proj/service/GeminiService.java`
- Test: `src/test/java/project2/example/proj/service/GeminiServiceTest.java`

**Interfaces:**
- Consumes: `GeminiExtractionResult` from Task 4
- Produces: `GeminiService.extract(String text): GeminiExtractionResult`

- [ ] **Step 1: Write failing test**

Create `src/test/java/project2/example/proj/service/GeminiServiceTest.java`:

```java
package project2.example.proj.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import project2.example.proj.dto.GeminiExtractionResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private GeminiService geminiService;

    @BeforeEach
    void setUp() {
        // Uses the package-private test constructor — avoids ReflectionTestUtils on final fields
        geminiService = new GeminiService(restClient, new ObjectMapper(), "test-key");
    }

    @Test
    void extract_parsesGeminiResponseCorrectly() throws Exception {
        String geminiJson = """
            {
              "candidates": [{
                "content": {
                  "parts": [{
                    "text": "{\\"documentType\\":\\"INVOICE\\",\\"vendor\\":\\"Acme Corp\\",\\"totalAmount\\":150.0,\\"currency\\":\\"USD\\",\\"documentDate\\":\\"2024-01-15\\",\\"summary\\":\\"Invoice from Acme Corp.\\",\\"lineItems\\":[]}"
                  }]
                }
              }]
            }
            """;

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(geminiJson);

        GeminiExtractionResult result = geminiService.extract("some document text");

        assertThat(result.getDocumentType()).isEqualTo("INVOICE");
        assertThat(result.getVendor()).isEqualTo("Acme Corp");
        assertThat(result.getTotalAmount()).isEqualTo(150.0);
        assertThat(result.getCurrency()).isEqualTo("USD");
        assertThat(result.getDocumentDate()).isEqualTo("2024-01-15");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```
./mvnw test -Dtest=GeminiServiceTest
```
Expected: FAIL — `GeminiService` does not exist yet.

- [ ] **Step 3: Create GeminiService.java**

Create `src/main/java/project2/example/proj/service/GeminiService.java`:

```java
package project2.example.proj.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import project2.example.proj.dto.GeminiExtractionResult;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private static final String GEMINI_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    // Spring-managed constructor
    public GeminiService(ObjectMapper objectMapper, @Value("${gemini.api.key}") String apiKey) {
        this(RestClient.create(), objectMapper, apiKey);
    }

    // Package-private constructor for unit tests — allows injecting a mock RestClient
    GeminiService(RestClient restClient, ObjectMapper objectMapper, String apiKey) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    public GeminiExtractionResult extract(String text) throws Exception {
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(Map.of("text", buildPrompt(text))))
            )
        );

        String response = restClient.post()
            .uri(GEMINI_URL + "?key=" + apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBody)
            .retrieve()
            .body(String.class);

        JsonNode root = objectMapper.readTree(response);
        String content = root
            .path("candidates").get(0)
            .path("content").path("parts").get(0)
            .path("text").asText();

        return objectMapper.readValue(content, GeminiExtractionResult.class);
    }

    private String buildPrompt(String text) {
        return """
            Analyze this document and extract the following in strict JSON format:
            {
              "documentType": "INVOICE | RECEIPT | REPORT | OTHER",
              "vendor": "company/vendor name or null",
              "totalAmount": numeric value or null,
              "currency": "USD/INR/EUR or null",
              "documentDate": "YYYY-MM-DD or null",
              "summary": "2-3 sentence summary of the document",
              "lineItems": [
                { "description": "", "quantity": 0, "unitPrice": 0.0, "totalPrice": 0.0 }
              ]
            }
            Return ONLY the JSON object, no markdown, no explanation.
            Document content: \
            """ + text;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```
./mvnw test -Dtest=GeminiServiceTest
```
Expected: PASS (1 test).

---

