package com.bautruc.ecommerce.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI bauTrucOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bau Truc Ecommerce API")
                        .version("v1")
                        .description("Backend API for Bau Truc Ecommerce"));
    }
}
