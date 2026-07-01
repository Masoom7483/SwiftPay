package com.swiftpay.ledger.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ledgerOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("SwiftPay — Ledger Service API")
                .description("Reporting endpoints for applied transfers and per-user history.")
                .version("v1"));
    }
}
