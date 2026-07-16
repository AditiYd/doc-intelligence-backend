package project2.example.proj.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
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
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    // @Autowired required when multiple constructors exist (Spring Framework 7+)
    @Autowired
    public GeminiService(@Value("${gemini.api.key}") String apiKey) {
        this(RestClient.create(), new ObjectMapper(), apiKey);
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
