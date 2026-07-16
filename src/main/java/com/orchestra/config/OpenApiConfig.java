package com.orchestra.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    /**
     * Swagger/OpenAPI 3 konfigürasyonu.
     * Uygulama ayağa kalkınca /swagger-ui.html adresinden API dokümanına ulaşılır.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Job Orchestrator API")
                        .description("Asenkron iş işleme (job orchestration) vitrin projesi")
                        .version("1.0.0"));
    }
}
