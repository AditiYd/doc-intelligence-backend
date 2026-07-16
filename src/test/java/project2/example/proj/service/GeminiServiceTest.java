package project2.example.proj.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import project2.example.proj.dto.GeminiExtractionResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mockito.AdditionalAnswers.returnsFirstArg;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
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
