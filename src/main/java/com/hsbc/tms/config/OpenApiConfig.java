package com.hsbc.tms.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI transactionMonitoringOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Transaction Monitoring API")
                        .version("v1")
                        .description("Core backend APIs for transaction management"));
    }
}

