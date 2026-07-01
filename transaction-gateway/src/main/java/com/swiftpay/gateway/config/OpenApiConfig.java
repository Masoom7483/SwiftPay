package com.swiftpay.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;


@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI swiftPayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SwiftPay — Transaction Gateway API")
                        .description("""
                                Public REST API for initiating peer-to-peer payments on SwiftPay.

                                Payments are accepted asynchronously: this service validates the
                                request, guarantees idempotency (via Redis, 24h window), persists a
                                PENDING record and emits a `PaymentInitiated` event. The Ledger
                                Service applies the transfer and reports the final status.
                                """)
                        .version("v1")
                        .contact(new Contact().name("SwiftPay Team").email("dev@swiftpay.example"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local")));
    }
}
