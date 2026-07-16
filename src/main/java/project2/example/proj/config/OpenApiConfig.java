package project2.example.proj.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI documentIntelligenceOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Document Intelligence API")
                .description("Upload, process, and query extracted data from invoices and other documents.")
                .version("v0.0.1"));
    }
}
