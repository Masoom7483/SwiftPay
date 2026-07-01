package com.swiftpay.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Service C (bonus) — Analytics Worker.
 *
 * <p>Consumes {@code PaymentCompleted} events and appends them to an analytics
 * table for real-time volume monitoring. Uses a Postgres "mock analytics" table
 * here; swap the repository for a ClickHouse writer to go full OLAP.
 */
@SpringBootApplication
public class AnalyticsWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsWorkerApplication.class, args);
    }
}
